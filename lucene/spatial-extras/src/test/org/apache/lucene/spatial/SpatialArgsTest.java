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

import org.apache.lucene.geo.geometry.Circle;
import org.apache.lucene.geo.geometry.GeoShape;
import org.apache.lucene.geo.geometry.Rectangle;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.spatial4j.Geo3dSpatialContextFactory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

public class SpatialArgsTest extends LuceneTestCase {

  @Test
  public void calcDistanceFromErrPct() {
    // nocommit: fix this first
//    final SpatialContext ctx = usually() ? SpatialContext.GEO : new Geo3dSpatialContextFactory().newSpatialContext();
    final SpatialContext ctx = SpatialContext.GEO;
    final double DEP = 0.5;//distErrPct

    //the result is the diagonal distance from the center to the closest corner,
    // times distErrPct

    GeoShape superwide = new Rectangle(0, 0, -180, 180);
    //0 distErrPct means 0 distance always
    assertEquals(0, SpatialArgs.calcDistanceFromErrPct(superwide, 0, ctx), 0);
    assertEquals(180 * DEP, SpatialArgs.calcDistanceFromErrPct(superwide, DEP, ctx), 0);

    GeoShape supertall = new Rectangle(-90, 90, 0, 0);
    assertEquals(90 * DEP, SpatialArgs.calcDistanceFromErrPct(supertall, DEP, ctx), 0);

    GeoShape upperhalf = new Rectangle(0, 90, -180, 180);
    assertEquals(45 * DEP, SpatialArgs.calcDistanceFromErrPct(upperhalf, DEP, ctx), 0.0001);

    GeoShape midCircle = new Circle(0, 0, 45);
    assertEquals(60 * DEP, SpatialArgs.calcDistanceFromErrPct(midCircle, DEP, ctx), 0.0001);
  }
}
