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

import org.apache.lucene.geo.GeoUtils;
import org.apache.lucene.spatial.SpatialContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.spatial.geometry.GeoCircle;
import org.apache.lucene.spatial.geometry.Geometry;
import org.apache.lucene.spatial.geometry.Geometry.Relation;
import org.apache.lucene.spatial.geometry.Point;
import org.apache.lucene.spatial.geometry.Rectangle;
import org.apache.lucene.spatial.prefix.tree.Cell;
import org.apache.lucene.spatial.prefix.tree.CellIterator;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.FixedBitSet;

/**
 * Finds docs where its indexed shape is {@link org.apache.lucene.spatial.query.SpatialOperation#WITHIN
 * WITHIN} the query shape.  It works by looking at cells outside of the query
 * shape to ensure documents there are excluded. By default, it will
 * examine all cells, and it's fairly slow.  If you know that the indexed shapes
 * are never comprised of multiple disjoint parts (which also means it is not multi-valued),
 * then you can pass {@code SpatialPrefixTree.getDistanceForLevel(maxLevels)} as
 * the {@code queryBuffer} constructor parameter to minimally look this distance
 * beyond the query shape's edge.  Even if the indexed shapes are sometimes
 * comprised of multiple disjoint parts, you might want to use this option with
 * a large buffer as a faster approximation with minimal false-positives.
 *
 * @lucene.experimental
 */
public class WithinPrefixTreeQuery extends AbstractVisitingPrefixTreeQuery {
  //TODO LUCENE-4869: implement faster algorithm based on filtering out false-positives of a
  //  minimal query buffer by looking in a DocValues cache holding a representative
  //  point of each disjoint component of a document's shape(s).

  //TODO Could the recursion in allCellsIntersectQuery() be eliminated when non-fuzzy or other
  //  circumstances?

  private final Geometry bufferedQueryShape;//if null then the whole world

  /**
   * See {@link AbstractVisitingPrefixTreeQuery#AbstractVisitingPrefixTreeQuery(Geometry, String, SpatialPrefixTree, int, int)}.
   * {@code queryBuffer} is the (minimum) distance beyond the query shape edge
   * where non-matching documents are looked for so they can be excluded. If
   * -1 is used then the whole world is examined (a good default for correctness).
   */
  public WithinPrefixTreeQuery(Geometry queryShape, String fieldName, SpatialPrefixTree grid,
                               int detailLevel, int prefixGridScanLevel,
                               double queryBuffer) {
    super(queryShape, fieldName, grid, detailLevel, prefixGridScanLevel);
    this.bufferedQueryShape = queryBuffer == -1 ? null : bufferShape(queryShape, queryBuffer);
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) return false;//checks getClass == o.getClass & instanceof

    WithinPrefixTreeQuery that = (WithinPrefixTreeQuery) o;

    if (bufferedQueryShape != null ? !bufferedQueryShape.equals(that.bufferedQueryShape) : that.bufferedQueryShape != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (bufferedQueryShape != null ? bufferedQueryShape.hashCode() : 0);
    return result;
  }

  @Override
  public String toString(String field) {
    return getClass().getSimpleName() + "(" +
             "fieldName=" + fieldName + "," +
             "queryShape=" + queryShape + "," +
             "detailLevel=" + detailLevel + "," +
             "prefixGridScanLevel=" + prefixGridScanLevel +
           ")";
  }

  /** Returns a new shape that is larger than shape by at distErr.
   */
  //TODO move this generic code elsewhere?  Spatial4j?
  protected Geometry bufferShape(Geometry shape, double distErr) {
    if (distErr <= 0)
      throw new IllegalArgumentException("distErr must be > 0");
    SpatialContext ctx = grid.getSpatialContext();
    if (shape instanceof Point) {
      Point p = (Point)shape;
      return new GeoCircle(p.y(), p.x(), distErr);
    } else if (shape instanceof GeoCircle) {
      GeoCircle circle = (GeoCircle) shape;
      double newDist = circle.getRadiusMeters() + distErr;
      if (ctx.isGeo() && newDist > 180)
        newDist = 180;
      return new GeoCircle(circle.getCenterLat(), circle.getCenterLon(), newDist);
    } else {
      Rectangle bbox = shape.getBoundingBox();
      double newMinLon = bbox.left() - distErr;
      double newMaxLon = bbox.right() + distErr;
      double newMinLat = bbox.bottom() - distErr;
      double newMaxLat = bbox.top() + distErr;
      if (ctx.isGeo()) {
        if (newMinLat < GeoUtils.MIN_LAT_INCL)
          newMinLat = GeoUtils.MIN_LAT_INCL;
        if (newMaxLat > GeoUtils.MAX_LAT_INCL)
          newMaxLat = GeoUtils.MAX_LAT_INCL;
        if (newMinLat == GeoUtils.MIN_LAT_INCL || newMaxLat == GeoUtils.MAX_LAT_INCL
            || bbox.getWidth() + 2 * distErr > 360) {
          newMinLon = GeoUtils.MIN_LON_INCL;
          newMaxLon = GeoUtils.MAX_LON_INCL;
        } else {
          newMinLon = GeoUtils.normalizeLonDegrees(newMinLon);
          newMaxLon = GeoUtils.normalizeLonDegrees(newMaxLon);
        }
      } else {
        //restrict to world bounds
        newMinLon = Math.max(newMinLon, ctx.getWorldBounds().left());
        newMaxLon = Math.min(newMaxLon, ctx.getWorldBounds().right());
        newMinLat = Math.max(newMinLat, ctx.getWorldBounds().bottom());
        newMaxLat = Math.min(newMaxLat, ctx.getWorldBounds().top());
      }
      return new Rectangle(newMinLat, newMaxLat, newMinLon, newMaxLon);
    }
  }


  @Override
  protected DocIdSet getDocIdSet(LeafReaderContext context) throws IOException {
    return new VisitorTemplate(context) {
      private FixedBitSet inside;
      private FixedBitSet outside;

      @Override
      protected void start() {
        inside = new FixedBitSet(maxDoc);
        outside = new FixedBitSet(maxDoc);
      }

      @Override
      protected DocIdSet finish() {
        inside.andNot(outside);
        return new BitDocIdSet(inside);
      }

      @Override
      protected CellIterator findSubCellsToVisit(Cell cell) {
        //use buffered query shape instead of orig.  Works with null too.
        return cell.getNextLevelCells(bufferedQueryShape);
      }

      @Override
      protected boolean visitPrefix(Cell cell) throws IOException {
        //cell.relate is based on the bufferedQueryShape; we need to examine what
        // the relation is against the queryShape
        final Rectangle r = cell.getRectangle();
        Relation visitRelation = queryShape.relate(r.left(), r.right(), r.bottom(), r.top());
        if (cell.getLevel() == detailLevel) {
          collectDocs(visitRelation == Relation.INTERSECTS ? inside : outside);
          return false;
        } else if (visitRelation == Relation.CONTAINS) {
          collectDocs(inside);
          return false;
        } else if (visitRelation == Relation.DISJOINT) {
          collectDocs(outside);
          return false;
        }
        return true;
      }

      @Override
      protected void visitLeaf(Cell cell) throws IOException {
        if (allCellsIntersectQuery(cell))
          collectDocs(inside);
        else
          collectDocs(outside);
      }

      /** Returns true if the provided cell, and all its sub-cells down to
       * detailLevel all intersect the queryShape.
       */
      private boolean allCellsIntersectQuery(Cell cell) {
        Rectangle r = cell.getRectangle();
        Relation relate = queryShape.relate(r.left(), r.right(), r.bottom(), r.top());
        if (cell.getLevel() == detailLevel)
          return relate == Relation.INTERSECTS;
        if (relate == Relation.CONTAINS)
          return true;
        if (relate == Relation.DISJOINT)
          return false;
        // Note: Generating all these cells just to determine intersection is not ideal.
        // The real solution is LUCENE-4869.
        CellIterator subCells = cell.getNextLevelCells(null);
        while (subCells.hasNext()) {
          Cell subCell = subCells.next();
          if (!allCellsIntersectQuery(subCell))//recursion
            return false;
        }
        return true;
      }

      @Override
      protected void visitScanned(Cell cell) throws IOException {
        visitLeaf(cell);//collects as we want, even if not a leaf
//        if (cell.isLeaf()) {
//          visitLeaf(cell);
//        } else {
//          visitPrefix(cell);
//        }
      }

    }.getDocIdSet();
  }

}
