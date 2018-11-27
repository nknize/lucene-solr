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
import java.util.List;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import org.apache.lucene.geo.GeoUtils;
import org.apache.lucene.geo.geometry.Circle;
import org.apache.lucene.geo.geometry.GeoShape;
import org.apache.lucene.geo.geometry.GeoShape.Relation;
import org.apache.lucene.geo.geometry.Point;
import org.apache.lucene.geo.geometry.Rectangle;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.spatial.SpatialContext;
import org.apache.lucene.spatial.StrategyTestCase;
import org.apache.lucene.spatial.prefix.tree.QuadPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.util.Bits;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.carrotsearch.randomizedtesting.RandomizedTest.atMost;
import static com.carrotsearch.randomizedtesting.RandomizedTest.randomIntBetween;

public class HeatmapFacetCounterTest extends StrategyTestCase {

  SpatialPrefixTree grid;

  int cellsValidated;
  int cellValidatedNonZero;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    cellsValidated = cellValidatedNonZero = 0;
    ctx = SpatialContext.GEO;
    grid = new QuadPrefixTree(ctx, randomIntBetween(1, 8));
    strategy = new RecursivePrefixTreeStrategy(grid, getTestClass().getSimpleName());
    if (rarely()) {
      ((PrefixTreeStrategy) strategy).setPointsOnly(true);
    }
  }

  @After
  public void after() {
    log.info("Validated " + cellsValidated + " cells, " + cellValidatedNonZero + " non-zero");
  }

  @Test
  public void testStatic() throws IOException {
    //Some specific tests (static, not random).
    adoc("0", new Rectangle(-90, -80, 179.8, -170));//barely crosses equator
    adoc("1", new Point(-85, -180));//a pt within the above rect
    adoc("2", new Point(-85, 172));//a pt to left of rect
    commit();

    validateHeatmapResultLoop(new Rectangle(-90, -85, +170, +180), 1, 100);
    validateHeatmapResultLoop(new Rectangle(-89, -50, -180, -160), 1, 100);
    validateHeatmapResultLoop(new Rectangle( -89, -50, 179, 179), 1, 100);//line
    // We could test anything and everything at this point... I prefer we leave that to random testing and then
    // add specific tests if we find a bug.
  }

  @Test
  public void testLucene7291Dateline() throws IOException {
    grid = new QuadPrefixTree(ctx, 2); // only 2, and we wind up with some big leaf cells
    strategy = new RecursivePrefixTreeStrategy(grid, getTestClass().getSimpleName());
    adoc("0", new Rectangle(43, 52, -102, -83));
    commit();
    validateHeatmapResultLoop(new Rectangle(62, 63, 179, -179), 2, 100);// HM crosses dateline
  }

  @Test
  public void testQueryCircle() throws IOException {
    //overwrite setUp; non-geo bounds is more straight-forward; otherwise 88,88 would actually be practically north,
    ctx = new SpatialContext(false, new Rectangle(-90, 90, -90, 90));
    final int LEVEL = 4;
    grid = new QuadPrefixTree(ctx, LEVEL);
    strategy = new RecursivePrefixTreeStrategy(grid, getTestClass().getSimpleName());
    Circle circle = new Circle(0, 0, 89);
    adoc("0", new Point(88, 88));//top-right, inside bbox of circle but not the circle
    adoc("1", new Point(0, 0));//clearly inside; dead center in fact
    commit();
    final HeatmapFacetCounter.Heatmap heatmap = HeatmapFacetCounter.calcFacets(
        (PrefixTreeStrategy) strategy, indexSearcher.getTopReaderContext(), null,
        circle, LEVEL, 1000);
    //assert that only one point is found, not 2
    boolean foundOne = false;
    for (int count : heatmap.counts) {
      switch (count) {
        case 0: break;
        case 1:
          assertFalse(foundOne);//this is the first
          foundOne = true;
          break;
        default:
          fail("counts should be 0 or 1: " + count);
      }
    }
    assertTrue(foundOne);
  }

  /** Recursively facet & validate at higher resolutions until we've seen enough. We assume there are
   * some non-zero cells. */
  private void validateHeatmapResultLoop(Rectangle inputRange, int facetLevel, int cellCountRecursThreshold)
      throws IOException {
    if (facetLevel > grid.getMaxLevels()) {
      return;
    }
    final int maxCells = 10_000;
    final HeatmapFacetCounter.Heatmap heatmap = HeatmapFacetCounter.calcFacets(
        (PrefixTreeStrategy) strategy, indexSearcher.getTopReaderContext(), null, inputRange, facetLevel, maxCells);
    int preNonZero = cellValidatedNonZero;
    validateHeatmapResult(inputRange, facetLevel, heatmap);
    assert cellValidatedNonZero - preNonZero > 0;//we validated more non-zero cells
    if (heatmap.counts.length < cellCountRecursThreshold) {
      validateHeatmapResultLoop(inputRange, facetLevel + 1, cellCountRecursThreshold);
    }
  }

  @Test
  @Repeat(iterations = 20)
  public void testRandom() throws IOException {
    // Tests using random index shapes & query shapes. This has found all sorts of edge case bugs (e.g. dateline,
    // cell border, overflow(?)).

    final int numIndexedShapes = 1 + atMost(9);
    List<GeoShape> indexedShapes = new ArrayList<>(numIndexedShapes);
    for (int i = 0; i < numIndexedShapes; i++) {
      indexedShapes.add(randomIndexedShape());
    }

    //Main index loop:
    for (int i = 0; i < indexedShapes.size(); i++) {
      GeoShape shape = indexedShapes.get(i);
      adoc("" + i, shape);

      if (random().nextInt(10) == 0)
        commit();//intermediate commit, produces extra segments
    }
    //delete some documents randomly
    for (int id = 0; id < indexedShapes.size(); id++) {
      if (random().nextInt(10) == 0) {
        deleteDoc("" + id);
        indexedShapes.set(id, null);
      }
    }

    commit();

    // once without dateline wrap
    final Rectangle rect = randomRectangle();
    queryHeatmapRecursive(usually() ? ctx.getWorldBounds() : rect, 1);
    // and once with dateline wrap
    if (rect.getWidth() > 0) {
      double shift = random().nextDouble() % rect.getWidth();
      queryHeatmapRecursive(new Rectangle(
              rect.minLat(), rect.maxLat(),
              GeoUtils.normalizeLonDegrees(rect.minLon() - shift),
              GeoUtils.normalizeLonDegrees(rect.maxLon() - shift)),
          1);
    }
  }

  /** Build heatmap, validate results, then descend recursively to another facet level. */
  private boolean queryHeatmapRecursive(Rectangle inputRange, int facetLevel) throws IOException {
    if (!inputRange.hasArea()) {
      // Don't test line inputs. It's not that we don't support it but it is more challenging to test if per-chance it
      // coincides with a grid line due due to edge overlap issue for some grid implementations (geo & quad).
      return false;
    }
    Bits filter = null; //FYI testing filtering of underlying PrefixTreeFacetCounter is done in another test
    //Calculate facets
    final int maxCells = 10_000;
    final HeatmapFacetCounter.Heatmap heatmap = HeatmapFacetCounter.calcFacets(
        (PrefixTreeStrategy) strategy, indexSearcher.getTopReaderContext(), filter, inputRange, facetLevel, maxCells);

    validateHeatmapResult(inputRange, facetLevel, heatmap);

    boolean foundNonZeroCount = false;
    for (int count : heatmap.counts) {
      if (count > 0) {
        foundNonZeroCount = true;
        break;
      }
    }

    //Test again recursively to higher facetLevel (more detailed cells)
    if (foundNonZeroCount && cellsValidated <= 500 && facetLevel != grid.getMaxLevels() && inputRange.hasArea()) {
      for (int i = 0; i < 5; i++) {//try multiple times until we find non-zero counts
        if (queryHeatmapRecursive(randomRectangle(inputRange), facetLevel + 1)) {
          break;//we found data here so we needn't try again
        }
      }
    }
    return foundNonZeroCount;
  }

  private void validateHeatmapResult(Rectangle inputRange, int facetLevel, HeatmapFacetCounter.Heatmap heatmap)
      throws IOException {
    final Rectangle heatRect = heatmap.region;
    assertTrue(heatRect.relate(inputRange) == Relation.CONTAINS || heatRect.equals(inputRange));
    final double cellWidth = heatRect.getWidth() / heatmap.columns;
    final double cellHeight = heatRect.getHeight() / heatmap.rows;
    for (int c = 0; c < heatmap.columns; c++) {
      for (int r = 0; r < heatmap.rows; r++) {
        final int facetCount = heatmap.getCount(c, r);
        double x = GeoUtils.normalizeLonDegrees(heatRect.minLon() + c * cellWidth + cellWidth / 2);
        double y = GeoUtils.normalizeLonDegrees(heatRect.minLat() + r * cellHeight + cellHeight / 2);
        Point pt =  new Point(y, x);
        assertEquals(countMatchingDocsAtLevel(pt, facetLevel), facetCount);
      }
    }
  }

  private int countMatchingDocsAtLevel(Point pt, int facetLevel) throws IOException {
    // we use IntersectsPrefixTreeFilter directly so that we can specify the level to go to exactly.
    RecursivePrefixTreeStrategy strategy = (RecursivePrefixTreeStrategy) this.strategy;
    Query filter = new IntersectsPrefixTreeQuery(
        pt, strategy.getFieldName(), grid, facetLevel, grid.getMaxLevels());
    final TotalHitCountCollector collector = new TotalHitCountCollector();
    indexSearcher.search(filter, collector);
    cellsValidated++;
    if (collector.getTotalHits() > 0) {
      cellValidatedNonZero++;
    }
    return collector.getTotalHits();
  }

  private GeoShape randomIndexedShape() {
    if (((PrefixTreeStrategy) strategy).isPointsOnly() || random().nextBoolean()) {
      return randomPoint();
    } else {
      return randomRectangle();
    }
  }
}