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
package org.apache.lucene.spatial.composite;

import java.io.IOException;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import org.apache.lucene.geo.geometry.Circle;
import org.apache.lucene.geo.geometry.GeoShape;
import org.apache.lucene.geo.geometry.Point;
import org.apache.lucene.geo.geometry.Rectangle;
import org.apache.lucene.spatial.SpatialContext;
import org.apache.lucene.spatial.prefix.RandomSpatialOpStrategyTestCase;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.QuadPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.spatial.serialized.LegacySerializedDVStrategy;
import org.junit.Test;

import static com.carrotsearch.randomizedtesting.RandomizedTest.randomBoolean;
import static com.carrotsearch.randomizedtesting.RandomizedTest.randomDouble;
import static com.carrotsearch.randomizedtesting.RandomizedTest.randomIntBetween;

public class CompositeStrategyTest extends RandomSpatialOpStrategyTestCase {

  private SpatialPrefixTree grid;
  private RecursivePrefixTreeStrategy rptStrategy;

  private void setupQuadGrid(int maxLevels) {
    //non-geospatial makes this test a little easier (in gridSnap), and using boundary values 2^X raises
    // the prospect of edge conditions we want to test, plus makes for simpler numbers (no decimals).
    this.ctx = new SpatialContext(false, new Rectangle(-128, 128, 0, 256));
    //A fairly shallow grid
    if (maxLevels == -1)
      maxLevels = randomIntBetween(1, 8);//max 64k cells (4^8), also 256*256
    this.grid = new QuadPrefixTree(ctx, maxLevels);
    this.rptStrategy = newRPT();
  }

  private void setupGeohashGrid(int maxLevels) {
    this.ctx = SpatialContext.GEO;
    //A fairly shallow grid
    if (maxLevels == -1)
      maxLevels = randomIntBetween(1, 3);//max 16k cells (32^3)
    this.grid = new GeohashPrefixTree(ctx, maxLevels);
    this.rptStrategy = newRPT();
  }

  protected RecursivePrefixTreeStrategy newRPT() {
    final RecursivePrefixTreeStrategy rpt = new RecursivePrefixTreeStrategy(this.grid,
        getClass().getSimpleName() + "_rpt");
    rpt.setDistErrPct(0.10);//not too many cells
    return rpt;
  }

  @Test
  @Repeat(iterations = 20)
  public void testOperations() throws IOException {
    //setup
    if (randomBoolean()) {
      setupQuadGrid(-1);
    } else {
      setupGeohashGrid(-1);
    }
    LegacySerializedDVStrategy legacySerializedDVStrategy = new LegacySerializedDVStrategy(ctx, getClass().getSimpleName() + "_sdv");
    this.strategy = new CompositeSpatialStrategy("composite_" + getClass().getSimpleName(),
        rptStrategy, legacySerializedDVStrategy);

    //Do it!

    for (SpatialOperation pred : SpatialOperation.values()) {
      if (pred == SpatialOperation.BBOX_INTERSECTS || pred == SpatialOperation.BBOX_WITHIN) {
        continue;
      }
      if (pred == SpatialOperation.DISJOINT) {//TODO
        continue;
      }
      testOperationRandomShapes(pred);
      deleteAll();
      commit();
    }
  }

  @Override
  protected GeoShape randomIndexedShape() {
    return randomShape();
  }

  @Override
  protected GeoShape randomQueryShape() {
    return randomShape();
  }

  private GeoShape randomShape() {
    return random().nextBoolean() ? randomCircle() : randomRectangle();
  }

  //TODO move up
  private GeoShape randomCircle() {
    final Point point = randomPoint();
    //TODO pick using gaussian
    double radius;
    if (ctx.isGeo()) {
      radius = randomDouble() * 100;
    } else {
      //find distance to closest edge
      final Rectangle worldBounds = ctx.getWorldBounds();
      double maxRad = point.lon() - worldBounds.minLon();
      maxRad = Math.min(maxRad, worldBounds.maxLon() - point.lon());
      maxRad = Math.min(maxRad, point.lat() - worldBounds.minLat());
      maxRad = Math.min(maxRad, worldBounds.maxLat() - point.lat());
      radius = randomDouble() * maxRad;
    }

    return new Circle(point.lat(), point.lon(), radius);
  }
}
