/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.spatial.prefix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.Query;
import org.apache.lucene.spatial.SpatialContext;
import org.apache.lucene.spatial.StrategyTestCase;
import org.apache.lucene.spatial.geometry.GeocentricGeometryFactory;
import org.apache.lucene.spatial.geometry.GeometryFactory;
import org.apache.lucene.spatial.geometry.Point;
import org.apache.lucene.spatial.geometry.Rectangle;
import org.apache.lucene.spatial.prefix.tree.Cell;
import org.apache.lucene.spatial.prefix.tree.CellIterator;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.PackedQuadPrefixTree;
import org.apache.lucene.spatial.prefix.tree.QuadPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.junit.Test;

import static org.apache.lucene.geo.geometry.GeoShape.Relation.CONTAINS;
import static org.apache.lucene.geo.geometry.GeoShape.Relation.DISJOINT;
import static org.apache.lucene.geo.geometry.GeoShape.Relation.INTERSECTS;
import static org.apache.lucene.geo.geometry.GeoShape.Relation.WITHIN;
import static com.carrotsearch.randomizedtesting.RandomizedTest.randomBoolean;
import static com.carrotsearch.randomizedtesting.RandomizedTest.randomInt;
import static com.carrotsearch.randomizedtesting.RandomizedTest.randomIntBetween;

/** Randomized PrefixTree test that considers the fuzziness of the
 * results introduced by grid approximation. */
public class RandomSpatialOpFuzzyPrefixTreeTest extends StrategyTestCase {

  static final int ITERATIONS = 10;

  protected SpatialPrefixTree grid;
  private SpatialContext ctx2D;

  public void setupGrid(int maxLevels) throws IOException {
    if (randomBoolean())
      setupQuadGrid(maxLevels, randomBoolean());
    else
      setupGeohashGrid(maxLevels);
    setupCtx2D(ctx);

    // set prune independently on strategy & grid randomly; should work
    ((RecursivePrefixTreeStrategy)strategy).setPruneLeafyBranches(randomBoolean());
    if (this.grid instanceof PackedQuadPrefixTree) {
      ((PackedQuadPrefixTree) this.grid).setPruneLeafyBranches(randomBoolean());
    }

    if (maxLevels == -1 && rarely()) {
      ((PrefixTreeStrategy) strategy).setPointsOnly(true);
    }

    log.info("Strategy: " + strategy.toString());
  }

  private void setupCtx2D(SpatialContext ctx) {
    if (ctx.isGeo() == false) {
      ctx2D = ctx;
    }
    //A non-geo version of ctx.
    GeometryFactory geometryFactory = GeocentricGeometryFactory.DEFAULT;
    ctx2D = new SpatialContext(geometryFactory);
  }

  private void setupQuadGrid(int maxLevels, boolean packedQuadPrefixTree) {
    //non-geospatial makes this test a little easier (in gridSnap), and using boundary values 2^X raises
    // the prospect of edge conditions we want to test, plus makes for simpler numbers (no decimals).
    GeometryFactory geometryFactory = new GeocentricGeometryFactory(0, 256, -128, 128) {};
    this.ctx = new SpatialContext(geometryFactory);
    //A fairly shallow grid, and default 2.5% distErrPct
    if (maxLevels == -1)
      maxLevels = randomIntBetween(1, 8);//max 64k cells (4^8), also 256*256
    if (packedQuadPrefixTree) {
      this.grid = new PackedQuadPrefixTree(ctx, maxLevels);
    } else {
      this.grid = new QuadPrefixTree(ctx, maxLevels);
    }
    this.strategy = newRPT();
  }

  public void setupGeohashGrid(int maxLevels) {
    this.ctx = SpatialContext.GEO;
    //A fairly shallow grid, and default 2.5% distErrPct
    if (maxLevels == -1)
      maxLevels = randomIntBetween(1, 3);//max 16k cells (32^3)
    this.grid = new GeohashPrefixTree(ctx, maxLevels);
    this.strategy = newRPT();
  }

  protected RecursivePrefixTreeStrategy newRPT() {
    return new RecursivePrefixTreeStrategy(this.grid, getClass().getSimpleName());
  }

  @Test
  @Repeat(iterations = ITERATIONS)
  public void testIntersects() throws IOException {
    setupGrid(-1);
    doTest(SpatialOperation.INTERSECTS);
  }

  @Test
  @Repeat(iterations = ITERATIONS)
  public void testWithin() throws IOException {
    setupGrid(-1);
    doTest(SpatialOperation.WITHIN);
  }

  @Test
  @Repeat(iterations = ITERATIONS)
  public void testContains() throws IOException {
    setupGrid(-1);
    doTest(SpatialOperation.CONTAINS);
  }

  @Test
  public void testPackedQuadPointsOnlyBug() throws IOException {
    setupQuadGrid(1, true); // packed quad.  maxLevels doesn't matter.
    setupCtx2D(ctx);
    ((PrefixTreeStrategy) strategy).setPointsOnly(true);
    Point point = (Point)ctx.geometryFactory().newPoint(169.0, 107.0);
    adoc("0", point);
    commit();
    Query query = strategy.makeQuery(new SpatialArgs(SpatialOperation.INTERSECTS, point));
    assertEquals(1, executeQuery(query, 1).numFound);
  }

  @Test
  public void testPointsOnlyOptBug() throws IOException {
    setupQuadGrid(8, false);
    setupCtx2D(ctx);
    ((PrefixTreeStrategy) strategy).setPointsOnly(true);
    Point point = new Point(-127.44362190053255, 86);
    adoc("0", point);
    commit();
    Query query = strategy.makeQuery(new SpatialArgs(SpatialOperation.INTERSECTS,
        new Rectangle(point.x(), point.x(), point.y(), point.y())));
    assertEquals(1, executeQuery(query, 1).numFound);
  }

  /** See LUCENE-5062, {@link ContainsPrefixTreeQuery#multiOverlappingIndexedShapes}. */
  @Test
  public void testContainsPairOverlap() throws IOException {
    setupQuadGrid(3, randomBoolean());
    adoc("0", new ShapePair(new Rectangle(-128, 128, 0, 33),
        new Rectangle(-128, 128, 33, 128), true));
    commit();
    Query query = strategy.makeQuery(new SpatialArgs(SpatialOperation.CONTAINS,
        new Rectangle(-16, 128, 0, 128)));
    SearchResults searchResults = executeQuery(query, 1);
    assertEquals(1, searchResults.numFound);
  }

  @Test
  public void testWithinDisjointParts() throws IOException {
    setupQuadGrid(7, randomBoolean());
    //one shape comprised of two parts, quite separated apart
    adoc("0", new ShapePair(new Rectangle(-120, -100, 0, 10),
        new Rectangle(110, 125,220, 240), false));
    commit();
    //query surrounds only the second part of the indexed shape
    Query query = strategy.makeQuery(new SpatialArgs(SpatialOperation.WITHIN,
        new Rectangle(105, 128, 210, 245)));
    SearchResults searchResults = executeQuery(query, 1);
    //we shouldn't find it because it's not completely within
    assertTrue(searchResults.numFound == 0);
  }

  @Test /** LUCENE-4916 */
  public void testWithinLeafApproxRule() throws IOException {
    setupQuadGrid(2, randomBoolean());//4x4 grid
    //indexed shape will simplify to entire right half (2 top cells)
    adoc("0", new Rectangle(-128, 128, 192, 204));
    commit();

    ((RecursivePrefixTreeStrategy) strategy).setPrefixGridScanLevel(randomInt(2));

    //query does NOT contain it; both indexed cells are leaves to the query, and
    // when expanded to the full grid cells, the top one's top row is disjoint
    // from the query and thus not a match.
    assertTrue(executeQuery(strategy.makeQuery(
        new SpatialArgs(SpatialOperation.WITHIN, new Rectangle(-72, 56, 38, 192))
    ), 1).numFound==0);//no-match

    //this time the rect is a little bigger and is considered a match. It's
    // an acceptable false-positive because of the grid approximation.
    assertTrue(executeQuery(strategy.makeQuery(
        new SpatialArgs(SpatialOperation.WITHIN, new Rectangle(-72, 80, 38, 192))
    ), 1).numFound==1);//match
  }

  @Test
  public void testShapePair() {
    ctx = SpatialContext.GEO;
    setupCtx2D(ctx);

    GeoShape leftShape = new ShapePair(new Rectangle(-8, 1, -74, -56),
        new Rectangle(-90, 90, -180, 134), true);
    GeoShape queryShape = new Rectangle(-90, 90, -180, 180);
    assertEquals(WITHIN, leftShape.relate(queryShape));
  }

  //Override so we can index parts of a pair separately, resulting in the detailLevel
  // being independent for each shape vs the whole thing
  @Override
  protected Document newDoc(String id, GeoShape shape) {
    Document doc = new Document();
    doc.add(new StringField("id", id, Field.Store.YES));
    if (shape != null) {
      Collection<GeoShape> shapes;
      if (shape instanceof ShapePair) {
        shapes = new ArrayList<>(2);
        shapes.add(((ShapePair)shape).shape1);
        shapes.add(((ShapePair)shape).shape2);
      } else {
        shapes = Collections.singleton(shape);
      }
      for (GeoShape shapei : shapes) {
        for (Field f : strategy.createIndexableFields(shapei)) {
          doc.add(f);
        }
      }
      if (storeShape)//just for diagnostics
        doc.add(new StoredField(strategy.getFieldName(), shape.toString()));
    }
    return doc;
  }

  @SuppressWarnings("fallthrough")
  private void doTest(final SpatialOperation operation) throws IOException {
    //first show that when there's no data, a query will result in no results
    {
      Query query = strategy.makeQuery(new SpatialArgs(operation, randomRectangle()));
      SearchResults searchResults = executeQuery(query, 1);
      assertEquals(0, searchResults.numFound);
    }

    final boolean biasContains = (operation == SpatialOperation.CONTAINS);

    //Main index loop:
    Map<String, GeoShape> indexedShapes = new LinkedHashMap<>();
    Map<String, GeoShape> indexedShapesGS = new LinkedHashMap<>();//grid snapped
    final int numIndexedShapes = randomIntBetween(1, 6);
    boolean indexedAtLeastOneShapePair = false;
    final boolean pointsOnly = ((PrefixTreeStrategy) strategy).isPointsOnly();
    for (int i = 0; i < numIndexedShapes; i++) {
      String id = "" + i;
      GeoShape indexedShape;
      int R = random().nextInt(12);
      if (R == 0) {//1 in 12
        indexedShape = null;
      } else if (R == 1 || pointsOnly) {//1 in 12
        indexedShape = randomPoint();//just one point
      } else if (R <= 4) {//3 in 12
        //comprised of more than one shape
        indexedShape = randomShapePairRect(biasContains);
        indexedAtLeastOneShapePair = true;
      } else {
        indexedShape = randomRectangle();//just one rect
      }

      indexedShapes.put(id, indexedShape);
      indexedShapesGS.put(id, gridSnap(indexedShape));

      adoc(id, indexedShape);

      if (random().nextInt(10) == 0)
        commit();//intermediate commit, produces extra segments

    }
    //delete some documents randomly
    Iterator<String> idIter = indexedShapes.keySet().iterator();
    while (idIter.hasNext()) {
      String id = idIter.next();
      if (random().nextInt(10) == 0) {
        deleteDoc(id);
        idIter.remove();
        indexedShapesGS.remove(id);
      }
    }

    commit();

    //Main query loop:
    final int numQueryShapes = atLeast(20);
    for (int i = 0; i < numQueryShapes; i++) {
      int scanLevel = randomInt(grid.getMaxLevels());
      ((RecursivePrefixTreeStrategy) strategy).setPrefixGridScanLevel(scanLevel);

      final GeoShape queryShape;
      switch (randomInt(10)) {
        case 0: queryShape = randomPoint(); break;
// LUCENE-5549
//TODO debug: -Dtests.method=testWithin -Dtests.multiplier=3 -Dtests.seed=5F5294CE2E075A3E:AAD2F0F79288CA64
//        case 1:case 2:case 3:
//          if (!indexedAtLeastOneShapePair) { // avoids ShapePair.relate(ShapePair), which isn't reliable
//            queryShape = randomShapePairRect(!biasContains);//invert biasContains for query side
//            break;
//          }

        case 4:
          //choose an existing indexed shape
          if (!indexedShapes.isEmpty()) {
            GeoShape tmp = indexedShapes.values().iterator().next();
            if (tmp instanceof Point || tmp instanceof Rectangle) {//avoids null and shapePair
              queryShape = tmp;
              break;
            }
          }//else fall-through

        default: queryShape = randomRectangle();
      }
      final GeoShape queryShapeGS = gridSnap(queryShape);

      final boolean opIsDisjoint = operation == SpatialOperation.DISJOINT;

      //Generate truth via brute force:
      // We ensure true-positive matches (if the predicate on the raw shapes match
      //  then the search should find those same matches).
      // approximations, false-positive matches
      Set<String> expectedIds = new LinkedHashSet<>();//true-positives
      Set<String> secondaryIds = new LinkedHashSet<>();//false-positives (unless disjoint)
      for (Map.Entry<String, GeoShape> entry : indexedShapes.entrySet()) {
        String id = entry.getKey();
        GeoShape indexedShapeCompare = entry.getValue();
        if (indexedShapeCompare == null)
          continue;
        GeoShape queryShapeCompare = queryShape;

        if (operation.evaluate(indexedShapeCompare, queryShapeCompare)) {
          expectedIds.add(id);
          if (opIsDisjoint) {
            //if no longer intersect after buffering them, for disjoint, remember this
            indexedShapeCompare = indexedShapesGS.get(id);
            queryShapeCompare = queryShapeGS;
            if (!operation.evaluate(indexedShapeCompare, queryShapeCompare))
              secondaryIds.add(id);
          }
        } else if (!opIsDisjoint) {
          //buffer either the indexed or query shape (via gridSnap) and try again
          if (operation == SpatialOperation.INTERSECTS) {
            indexedShapeCompare = indexedShapesGS.get(id);
            queryShapeCompare = queryShapeGS;
            //TODO Unfortunately, grid-snapping both can result in intersections that otherwise
            // wouldn't happen when the grids are adjacent. Not a big deal but our test is just a
            // bit more lenient.
          } else if (operation == SpatialOperation.CONTAINS) {
            indexedShapeCompare = indexedShapesGS.get(id);
          } else if (operation == SpatialOperation.WITHIN) {
            queryShapeCompare = queryShapeGS;
          }
          if (operation.evaluate(indexedShapeCompare, queryShapeCompare))
            secondaryIds.add(id);
        }
      }

      //Search and verify results
      SpatialArgs args = new SpatialArgs(operation, queryShape);
      if (queryShape instanceof ShapePair)
        args.setDistErrPct(0.0);//a hack; we want to be more detailed than gridSnap(queryShape)
      Query query = strategy.makeQuery(args);
      SearchResults got = executeQuery(query, 100);
      Set<String> remainingExpectedIds = new LinkedHashSet<>(expectedIds);
      for (SearchResult result : got.results) {
        String id = result.getId();
        boolean removed = remainingExpectedIds.remove(id);
        if (!removed && (!opIsDisjoint && !secondaryIds.contains(id))) {
          fail("Shouldn't match", id, indexedShapes, indexedShapesGS, queryShape);
        }
      }
      if (opIsDisjoint)
        remainingExpectedIds.removeAll(secondaryIds);
      if (!remainingExpectedIds.isEmpty()) {
        String id = remainingExpectedIds.iterator().next();
        fail("Should have matched", id, indexedShapes, indexedShapesGS, queryShape);
      }
    }
  }

  private GeoShape randomShapePairRect(boolean biasContains) {
    Rectangle shape1 = randomRectangle();
    Rectangle shape2 = randomRectangle();
    return new ShapePair(shape1, shape2, biasContains);
  }

  private void fail(String label, String id, Map<String, GeoShape> indexedShapes, Map<String, GeoShape> indexedShapesGS, GeoShape queryShape) {
    System.err.println("Ig:" + indexedShapesGS.get(id) + " Qg:" + gridSnap(queryShape));
    fail(label + " I#" + id + ":" + indexedShapes.get(id) + " Q:" + queryShape);
  }

//  private Rectangle inset(Rectangle r) {
//    //typically inset by 1 (whole numbers are easy to read)
//    double d = Math.min(1.0, grid.getDistanceForLevel(grid.getMaxLevels()) / 4);
//    return ctx.makeRectangle(r.getMinX() + d, r.getMaxX() - d, r.getMinY() + d, r.getMaxY() - d);
//  }

  protected GeoShape gridSnap(GeoShape snapMe) {
    if (snapMe == null)
      return null;
    if (snapMe instanceof ShapePair) {
      ShapePair me = (ShapePair) snapMe;
      return new ShapePair(gridSnap(me.shape1), gridSnap(me.shape2), me.biasContainsThenWithin);
    }
    if (snapMe instanceof Point) {
      snapMe = snapMe.getBoundingBox();
    }
    //The next 4 lines mimic PrefixTreeStrategy.createIndexableFields()
    double distErrPct = ((PrefixTreeStrategy) strategy).getDistErrPct();
    double distErr = SpatialArgs.calcDistanceFromErrPct(snapMe, distErrPct, ctx);
    int detailLevel = grid.getLevelForDistance(distErr);
    CellIterator cells = grid.getTreeCellIterator(snapMe, detailLevel);

    //calc bounding box of cells.
    List<GeoShape> cellShapes = new ArrayList<>(1024);
    while (cells.hasNext()) {
      Cell cell = cells.next();
      if (!cell.isLeaf())
        continue;
      cellShapes.add(cell.getShape());
    }
    return new GeoShapeCollection(cellShapes.toArray(new GeoShape[cellShapes.size()])).getBoundingBox();
  }

  /**
   * An aggregate of 2 shapes. Unfortunately we can't simply use a ShapeCollection because:
   * (a) ambiguity between CONTAINS and WITHIN for equal shapes, and
   * (b) adjacent pairs could as a whole contain the input shape.
   * The tests here are sensitive to these matters, although in practice ShapeCollection
   * is fine.
   */
  private class ShapePair extends GeoShapeCollection {

    final GeoShape shape1, shape2;
    final GeoShape shape1_2D, shape2_2D;//not geo (bit of a hack)
    final boolean biasContainsThenWithin;

    public ShapePair(GeoShape shape1, GeoShape shape2, boolean containsThenWithin) {
      super(shape1, shape2);
      this.shape1 = shape1;
      this.shape2 = shape2;
      this.shape1_2D = toNonGeo(shape1);
      this.shape2_2D = toNonGeo(shape2);
      biasContainsThenWithin = containsThenWithin;
    }

    private GeoShape toNonGeo(GeoShape shape) {
      if (!ctx.isGeo())
        return shape;//already non-geo
      if (shape instanceof Rectangle) {
        Rectangle rect = (Rectangle) shape;
        if (rect.crossesDateline()) {
          return new ShapePair(
              new Rectangle(rect.minLat(), rect.maxLat(), rect.minLon(), 180),
              new Rectangle(rect.minLat(), rect.maxLat(), -180, rect.maxLon()),
              biasContainsThenWithin);
        } else {
          return new Rectangle(rect.minLat(), rect.maxLat(), rect.minLon(), rect.maxLon());
        }
      }
      //no need to do others; this addresses the -180/+180 ambiguity corner test problem
      return shape;
    }

    @Override
    public GeoShape.Relation relate(GeoShape other) {
      GeoShape.Relation r = relateApprox(other);
      if (r == DISJOINT)
        return r;
      if (r == CONTAINS)
        return r;
      if (r == WITHIN && !biasContainsThenWithin)
        return r;

      //See if the correct answer is actually Contains, when the indexed shapes are adjacent,
      // creating a larger shape that contains the input shape.
      boolean pairTouches = shape1.relate(shape2) == INTERSECTS;
      if (!pairTouches)
        return r;
      //test all 4 corners
      // Note: awkwardly, we use a non-geo context for this because in geo, -180 & +180 are the same place, which means
      //  that "other" might wrap the world horizontally and yet all its corners could be in shape1 (or shape2) even
      //  though shape1 is only adjacent to the dateline. I couldn't think of a better way to handle this.
      Rectangle oRect = (Rectangle)other;
      if (cornerContainsNonGeo(oRect.minLon(), oRect.minLat())
          && cornerContainsNonGeo(oRect.minLon(), oRect.maxLat())
          && cornerContainsNonGeo(oRect.maxLon(), oRect.minLat())
          && cornerContainsNonGeo(oRect.maxLon(), oRect.maxLat()) )
        return CONTAINS;
      return r;
    }

    private boolean cornerContainsNonGeo(double x, double y) {
      GeoShape pt = new Point(y, x);
      return shape1_2D.relate(pt) == INTERSECTS
          || shape2_2D.relate(pt) == INTERSECTS;
    }

    private Relation relateApprox(GeoShape other) {
      if (biasContainsThenWithin) {
        if (shape1.relate(other) == CONTAINS || shape1.equals(other)
            || shape2.relate(other) == CONTAINS || shape2.equals(other)) return CONTAINS;

        if (shape1.relate(other) == WITHIN && shape2.relate(other) == WITHIN) return WITHIN;

      } else {
        if ((shape1.relate(other) == WITHIN || shape1.equals(other))
            && (shape2.relate(other) == WITHIN || shape2.equals(other))) return WITHIN;

        if (shape1.relate(other) == CONTAINS || shape2.relate(other) == CONTAINS) return CONTAINS;
      }

      if (shape1.relate(other) == INTERSECTS
          || shape2.relate(other) == INTERSECTS)
        return INTERSECTS;//might actually be 'CONTAINS' if the pair are adjacent but we handle that later
      return DISJOINT;
    }

    @Override
    public String toString() {
      return "ShapePair(" + shape1 + " , " + shape2 + ")";
    }
  }

}
