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
package org.apache.lucene.spatial.spatial4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import org.apache.lucene.geo.geometry.Rectangle;
import org.apache.lucene.geo.parsers.WKTParser;
import org.apache.lucene.spatial.SpatialContext;
import org.apache.lucene.spatial.composite.CompositeSpatialStrategy;
import org.apache.lucene.spatial.prefix.RandomSpatialOpStrategyTestCase;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.QuadPrefixTree;
import org.apache.lucene.spatial.prefix.tree.S2PrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.spatial.serialized.LegacySerializedDVStrategy;
import org.apache.lucene.spatial3d.geom.GeoAreaShape;
import org.apache.lucene.spatial3d.geom.GeoPath;
import org.apache.lucene.spatial3d.geom.GeoPathFactory;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoPointShape;
import org.apache.lucene.spatial3d.geom.GeoPolygonFactory;
import org.apache.lucene.spatial3d.geom.GeoShape;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.RandomGeo3dShapeGenerator;
import org.junit.Test;

public class Geo3dRptTest extends RandomSpatialOpStrategyTestCase {
  @Override
  protected org.apache.lucene.geo.geometry.GeoShape randomIndexedShape() {
    return null;
  }

  @Override
  protected org.apache.lucene.geo.geometry.GeoShape randomQueryShape() {
    return null;
  }

/*  private PlanetModel planetModel;
  private RandomGeo3dShapeGenerator shapeGenerator;
  private SpatialPrefixTree grid;
  private RecursivePrefixTreeStrategy rptStrategy;

  private void setupGrid() {
    int type = random().nextInt(4);
    if (type == 0) {
      this.grid = new GeohashPrefixTree(ctx, 2);
    } else if (type == 1) {
      this.grid = new QuadPrefixTree(ctx, 5);
    } else {
      int arity = random().nextInt(3) + 1;
      this.grid = new S2PrefixTree(ctx, 5 - arity, arity);
    }
    this.rptStrategy = newRPT();
    this.rptStrategy.setPruneLeafyBranches(random().nextBoolean());
  }

  protected RecursivePrefixTreeStrategy newRPT() {
    final RecursivePrefixTreeStrategy rpt = new RecursivePrefixTreeStrategy(this.grid,
        getClass().getSimpleName() + "_rpt");
    rpt.setDistErrPct(0.10);//not too many cells
    return rpt;
  }

  private void setupStrategy() {
    shapeGenerator = new RandomGeo3dShapeGenerator();
    planetModel = shapeGenerator.randomPlanetModel();
    Geo3dSpatialContextFactory factory = new Geo3dSpatialContextFactory();
    factory.planetModel = planetModel;
    ctx = factory.newSpatialContext();

    setupGrid();

    LegacySerializedDVStrategy legacySerializedDVStrategy = new LegacySerializedDVStrategy(ctx, getClass().getSimpleName() + "_sdv");
    this.strategy = new CompositeSpatialStrategy("composite_" + getClass().getSimpleName(),
        rptStrategy, legacySerializedDVStrategy);
  }

  @Test
  public void testFailure1() throws IOException {
    setupStrategy();
    final List<GeoPoint> points = new ArrayList<GeoPoint>();
    points.add(new GeoPoint(planetModel, StrictMath.toRadians(18), StrictMath.toRadians(-27)));
    points.add(new GeoPoint(planetModel, StrictMath.toRadians(-57), StrictMath.toRadians(146)));
    points.add(new GeoPoint(planetModel, StrictMath.toRadians(14), StrictMath.toRadians(-180)));
    points.add(new GeoPoint(planetModel, StrictMath.toRadians(-15), StrictMath.toRadians(153)));

    final GeoShape triangle = new Geo3dShape(GeoPolygonFactory.makeGeoPolygon(planetModel, points),ctx);
    final Rectangle rect = new Rectangle(73, 86, -49, -45);
    testOperation(rect,SpatialOperation.INTERSECTS,triangle, false);
  }

  @Test
  public void testFailureLucene6535() throws IOException {
    setupStrategy();

    final List<GeoPoint> points = new ArrayList<>();
    points.add(new GeoPoint(planetModel, StrictMath.toRadians(18), StrictMath.toRadians(-27)));
    points.add(new GeoPoint(planetModel, StrictMath.toRadians(-57), StrictMath.toRadians(146)));
    points.add(new GeoPoint(planetModel, StrictMath.toRadians(14), StrictMath.toRadians(-180)));
    points.add(new GeoPoint(planetModel, StrictMath.toRadians(-15), StrictMath.toRadians(153)));
    final GeoPoint[] pathPoints = new GeoPoint[] {
<<<<<<< Updated upstream
        new GeoPoint(planetModel, 55.0 * DEGREES_TO_RADIANS, -26.0 * DEGREES_TO_RADIANS),
        new GeoPoint(planetModel, -90.0 * DEGREES_TO_RADIANS, 0.0),
        new GeoPoint(planetModel, 54.0 * DEGREES_TO_RADIANS, 165.0 * DEGREES_TO_RADIANS),
        new GeoPoint(planetModel, -90.0 * DEGREES_TO_RADIANS, 0.0)};
    final GeoPath path = GeoPathFactory.makeGeoPath(planetModel, 29 * DEGREES_TO_RADIANS, pathPoints);
    final Shape shape = new Geo3dShape(path,ctx);
    final Rectangle rect = ctx.makeRectangle(131, 143, 39, 54);
    testOperation(rect,SpatialOperation.Intersects,shape,true);
=======
      new GeoPoint(planetModel, StrictMath.toRadians(55.0), StrictMath.toRadians(-26.0)),
      new GeoPoint(planetModel, StrictMath.toRadians(-90.0), StrictMath.toRadians(0.0)),
      new GeoPoint(planetModel, StrictMath.toRadians(54.0), StrictMath.toRadians(165.0)),
      new GeoPoint(planetModel, StrictMath.toRadians(-90.0), StrictMath.toRadians(0.0))};
    final GeoPath path = GeoPathFactory.makeGeoPath(planetModel, StrictMath.toRadians(29), pathPoints);
    final GeoShape shape = new Geo3dShape(path,ctx);
    final Rectangle rect = new Rectangle(39, 54, 131, 143);
    testOperation(rect, SpatialOperation.INTERSECTS, shape, true);
>>>>>>> Stashed changes
  }

  @Test
  @Repeat(iterations = 30)
  public void testOperations() throws IOException {
    setupStrategy();

    testOperationRandomShapes(SpatialOperation.INTERSECTS);
  }

  @Override
  protected GeoShape randomIndexedShape() {
    int type = shapeGenerator.randomShapeType();
    GeoAreaShape areaShape = shapeGenerator.randomGeoAreaShape(type, planetModel);
    if (areaShape instanceof GeoPointShape) {
      return new Geo3dPointShape((GeoPointShape) areaShape, ctx);
    }
    return new Geo3dShape<>(areaShape, ctx);
  }

  @Override
  protected GeoShape randomQueryShape() {
    int type = shapeGenerator.randomShapeType();
    GeoAreaShape areaShape = shapeGenerator.randomGeoAreaShape(type, planetModel);
    return new Geo3dShape<>(areaShape, ctx);
  }

//  //TODO move to a new test class?
//  @Test
//  public void testWKT() throws Exception {
//    Geo3dSpatialContextFactory factory = new Geo3dSpatialContextFactory();
//    SpatialContext ctx = SpatialContext.GEO;
//    String wkt = "POLYGON ((20.0 -60.4, 20.1 -60.4, 20.1 -60.3, 20.0  -60.3,20.0 -60.4))";
//    GeoShape s = WKTParser.parse(wkt);
//    assertTrue(s instanceof  Geo3dShape<?>);
//    wkt = "POINT (30 10)";
//    s = ctx.getFormats().getWktReader().read(wkt);
//    assertTrue(s instanceof  Geo3dShape<?>);
//    wkt = "LINESTRING (30 10, 10 30, 40 40)";
//    s = ctx.getFormats().getWktReader().read(wkt);
//    assertTrue(s instanceof  Geo3dShape<?>);
//    wkt = "POLYGON ((35 10, 45 45, 15 40, 10 20, 35 10), (20 30, 35 35, 30 20, 20 30))";
//    s = ctx.getFormats().getWktReader().read(wkt);
//    assertTrue(s instanceof  Geo3dShape<?>);
//    wkt = "MULTIPOINT ((10 40), (40 30), (20 20), (30 10))";
//    s = ctx.getFormats().getWktReader().read(wkt);
//    assertTrue(s instanceof  Geo3dShape<?>);
//    wkt = "MULTILINESTRING ((10 10, 20 20, 10 40),(40 40, 30 30, 40 20, 30 10))";
//    s = ctx.getFormats().getWktReader().read(wkt);
//    assertTrue(s instanceof  Geo3dShape<?>);
//    wkt = "MULTIPOLYGON (((40 40, 20 45, 45 30, 40 40)), ((20 35, 10 30, 10 10, 30 5, 45 20, 20 35),(30 20, 20 15, 20 25, 30 20)))";
//    s = ctx.getFormats().getWktReader().read(wkt);
//    assertTrue(s instanceof  Geo3dShape<?>);
//    wkt = "GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(4 6,7 10))";
//    s = ctx.getFormats().getWktReader().read(wkt);
//    assertTrue(s instanceof  Geo3dShape<?>);
//    wkt = "ENVELOPE(1, 2, 4, 3)";
//    s = ctx.getFormats().getWktReader().read(wkt);
//    assertTrue(s instanceof  Geo3dShape<?>);
//    wkt = "BUFFER(POINT(-10 30), 5.2)";
//    s = ctx.getFormats().getWktReader().read(wkt);
//    assertTrue(s instanceof  Geo3dShape<?>);
//    //wkt = "BUFFER(LINESTRING(1 2, 3 4), 0.5)";
//    //s = ctx.getFormats().getWktReader().read(wkt);
//    //assertTrue(s instanceof  Geo3dShape<?>);
//  }
*/
}
