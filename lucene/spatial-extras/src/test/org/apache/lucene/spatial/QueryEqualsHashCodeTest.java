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
package org.apache.lucene.spatial;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.lucene.geo.geometry.GeoShape;
import org.apache.lucene.geo.geometry.Rectangle;
import org.apache.lucene.spatial.bbox.BBoxStrategy;
import org.apache.lucene.spatial.composite.CompositeSpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.TermQueryPrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.QuadPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.spatial.serialized.LegacySerializedDVStrategy;
import org.apache.lucene.spatial.vector.PointVectorStrategy;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

public class QueryEqualsHashCodeTest extends LuceneTestCase {

  private final SpatialContext ctx = SpatialContext.GEO;

  private SpatialOperation predicate;

  @Test
  public void testEqualsHashCode() {

    switch (random().nextInt(4)) {//0-3
      case 0: predicate = SpatialOperation.CONTAINS; break;
      case 1: predicate = SpatialOperation.WITHIN; break;

      default: predicate = SpatialOperation.INTERSECTS; break;
    }
    final SpatialPrefixTree gridQuad = new QuadPrefixTree(ctx,10);
    final SpatialPrefixTree gridGeohash = new GeohashPrefixTree(ctx,10);

    Collection<SpatialStrategy> strategies = new ArrayList<>();
    RecursivePrefixTreeStrategy recursive_geohash = new RecursivePrefixTreeStrategy(gridGeohash, "recursive_geohash");
    strategies.add(recursive_geohash);
    strategies.add(new TermQueryPrefixTreeStrategy(gridQuad, "termquery_quad"));
    strategies.add(PointVectorStrategy.newInstance(ctx, "pointvector"));
    strategies.add(BBoxStrategy.newInstance(ctx, "bbox"));
    final LegacySerializedDVStrategy serialized = new LegacySerializedDVStrategy(ctx, "serialized");
    strategies.add(serialized);
    strategies.add(new CompositeSpatialStrategy("composite", recursive_geohash, serialized));
    for (SpatialStrategy strategy : strategies) {
      testEqualsHashcode(strategy);
    }
  }

  private void testEqualsHashcode(final SpatialStrategy strategy) {
    final SpatialArgs args1 = makeArgs1();
    final SpatialArgs args2 = makeArgs2();
    testEqualsHashcode(args1, args2, new ObjGenerator() {
      @Override
      public Object gen(SpatialArgs args) {
        return strategy.makeQuery(args);
      }
    });
    testEqualsHashcode(args1, args2, new ObjGenerator() {
      @Override
      public Object gen(SpatialArgs args) {
        return strategy.makeDistanceValueSource(args.getShape().getCenter());
      }
    });
  }

  private void testEqualsHashcode(SpatialArgs args1, SpatialArgs args2, ObjGenerator generator) {
    Object first;
    try {
      first = generator.gen(args1);
    } catch (UnsupportedOperationException e) {
      return;
    }
    if (first == null)
      return;//unsupported op?
    Object second = generator.gen(args1);//should be the same
    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
    assertTrue(args1.equals(args2) == false);
    second = generator.gen(args2);//now should be different
    assertTrue(first.equals(second) == false);
    assertTrue(first.hashCode() != second.hashCode());
  }

  private SpatialArgs makeArgs1() {
    final GeoShape shape1 = new Rectangle(10, 10, 0, 0);
    return new SpatialArgs(predicate, shape1);
  }

  private SpatialArgs makeArgs2() {
    final GeoShape shape2 = new Rectangle(20, 20, 0, 0);
    return new SpatialArgs(predicate, shape2);
  }

  interface ObjGenerator {
    Object gen(SpatialArgs args);
  }

}
