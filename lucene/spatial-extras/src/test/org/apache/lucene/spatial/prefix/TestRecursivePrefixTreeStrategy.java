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

import org.apache.lucene.geo.GeoUtils;
import org.apache.lucene.geo.geometry.Circle;
import org.apache.lucene.geo.geometry.GeoShape;
import org.apache.lucene.geo.geometry.Point;
import org.apache.lucene.spatial.SpatialContext;
import org.apache.lucene.spatial.SpatialMatchConcern;
import org.apache.lucene.spatial.StrategyTestCase;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class TestRecursivePrefixTreeStrategy extends StrategyTestCase {

  private int maxLength;

  //Tests should call this first.
  private void init(int maxLength) {
    this.maxLength = maxLength;
    this.ctx = SpatialContext.GEO;
    GeohashPrefixTree grid = new GeohashPrefixTree(ctx, maxLength);
    this.strategy = new RecursivePrefixTreeStrategy(grid, getClass().getSimpleName());
  }

  @Test
  public void testFilterWithVariableScanLevel() throws IOException {
    init(GeohashPrefixTree.getMaxLevelsPossible());
    getAddAndVerifyIndexedDocuments(DATA_WORLD_CITIES_POINTS);

    //execute queries for each prefix grid scan level
    for(int i = 0; i <= maxLength; i++) {
      ((RecursivePrefixTreeStrategy)strategy).setPrefixGridScanLevel(i);
      executeQueries(SpatialMatchConcern.FILTER, QTEST_Cities_Intersects_BBox);
    }
  }

  @Test
  public void testOneMeterPrecision() {
    init(GeohashPrefixTree.getMaxLevelsPossible());
    GeohashPrefixTree grid = (GeohashPrefixTree) ((RecursivePrefixTreeStrategy) strategy).getGrid();
    //DWS: I know this to be true.  11 is needed for one meter
    double degrees = GeoUtils.distanceToDegrees(0.001, GeoUtils.EARTH_MEAN_RADIUS_METERS / 1000d);
    assertEquals(11, grid.getLevelForDistance(degrees));
  }

  @Test
  public void testPrecision() throws IOException{
    init(GeohashPrefixTree.getMaxLevelsPossible());

    Point iPt = new Point(48.3708044, 2.8028712999999925);//lon, lat
    addDocument(newDoc("iPt", iPt));
    commit();

    Point qPt = new Point(48.6003516, 2.4632387000000335);

    final double KM2DEG = GeoUtils.distanceToDegrees(1, GeoUtils.EARTH_MEAN_RADIUS_METERS / 1000d);
    final double DEG2KM = 1 / KM2DEG;

    final double DIST = 35.75;//35.7499...
    assertEquals(DIST, SpatialContext.calculateDistance(iPt.lat(), iPt.lon(), qPt.lat(), qPt.lon()) * DEG2KM, 0.001);

    //distErrPct will affect the query shape precision. The indexed precision
    // was set to nearly zilch via init(GeohashPrefixTree.getMaxLevelsPossible());
    final double distErrPct = 0.025; //the suggested default, by the way
    final double distMult = 1+distErrPct;

    assertTrue(35.74*distMult >= DIST);
    checkHits(q(qPt, 35.74 * KM2DEG, distErrPct), 1, null);

    assertTrue(30*distMult < DIST);
    checkHits(q(qPt, 30 * KM2DEG, distErrPct), 0, null);

    assertTrue(33*distMult < DIST);
    checkHits(q(qPt, 33 * KM2DEG, distErrPct), 0, null);

    assertTrue(34*distMult < DIST);
    checkHits(q(qPt, 34 * KM2DEG, distErrPct), 0, null);
  }

  private SpatialArgs q(Point pt, double distDEG, double distErrPct) {
    GeoShape shape = new Circle(pt.lat(), pt.lon(), distDEG);
    SpatialArgs args = new SpatialArgs(SpatialOperation.INTERSECTS,shape);
    args.setDistErrPct(distErrPct);
    return args;
  }

  private void checkHits(SpatialArgs args, int assertNumFound, int[] assertIds) {
    SearchResults got = executeQuery(strategy.makeQuery(args), 100);
    assertEquals("" + args, assertNumFound, got.numFound);
    if (assertIds != null) {
      Set<Integer> gotIds = new HashSet<>();
      for (SearchResult result : got.results) {
        gotIds.add(Integer.valueOf(result.document.get("id")));
      }
      for (int assertId : assertIds) {
        assertTrue("has "+assertId,gotIds.contains(assertId));
      }
    }
  }

}
