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
package org.apache.lucene.geo;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestUtil;

import static org.apache.lucene.geo.GeoEncodingUtils.decodeLatitude;
import static org.apache.lucene.geo.GeoEncodingUtils.decodeLongitude;
import static org.apache.lucene.geo.GeoEncodingUtils.encodeLatitude;
import static org.apache.lucene.geo.GeoEncodingUtils.encodeLongitude;
import static org.apache.lucene.geo.GeoTestUtil.nextBox;

/** Test case for the Polygon {@link Tessellator} class */
public class TestTessellator extends LuceneTestCase {

  public void testLinesIntersect() {
    Rectangle rect = nextBox();
    // quantize lat/lon of bounding box:
    int minX = encodeLongitude(rect.minLon);
    int maxX = encodeLongitude(rect.maxLon);
    int minY = encodeLatitude(rect.minLat);
    int maxY = encodeLatitude(rect.maxLat);
    // simple case; test intersecting diagonals
    assertTrue(Tessellator.linesIntersect(minX, minY, maxX, maxY, maxX, minY, minX, maxY));
    // test closest encoded value
    assertFalse(Tessellator.linesIntersect(minX, maxY, maxX, maxY, minX - 1, minY, minX - 1, maxY));
  }

  public void testBug() throws Exception {
    // add a random polygon with a hole
    Polygon inner = GeoTestUtil.createRegularPolygon(0, -90, atLeast(10000), 262726);
    Polygon outer = GeoTestUtil.createRegularPolygon(0, -90, atLeast(1000000), 262726);
    List<Tessellator.Triangle> tess = Tessellator.tessellate(new Polygon(outer.getPolyLats(), outer.getPolyLons(), inner));

    PrintWriter writer = new PrintWriter("poly.svg", "UTF-8");
    writer.println(toSVG(tess));
    writer.close();
  }

  public void testSimpleTessellation() throws Exception {
    Polygon poly = GeoTestUtil.createRegularPolygon(0.0, 0.0, 1000000, 1000000);
    Polygon inner = new Polygon(new double[] {-1.0, -1.0, 0.5, 1.0, 1.0, 0.5, -1.0},
        new double[]{1.0, -1.0, -0.5, -1.0, 1.0, 0.5, 1.0});
    Polygon inner2 = new Polygon(new double[] {-1.0, -1.0, 0.5, 1.0, 1.0, 0.5, -1.0},
        new double[]{-2.0, -4.0, -3.5, -4.0, -2.0, -2.5, -2.0});
    poly = new Polygon(poly.getPolyLats(), poly.getPolyLons(), inner, inner2);

    List<Tessellator.Triangle> tessellation = Tessellator.tessellate(poly);
    assertTrue(Tessellator.tessellate(poly).size() > 0);

    //System.out.println("tessellation bootstrap: " + TimeUnit.NANOSECONDS.toMillis(end - start));
    //PrintWriter writer = new PrintWriter("poly.svg", "UTF-8");
    //writer.println(toSVG(tessellation));
    //writer.close();
  }

  public void testAtScale() throws Exception {
    int numVertices;
    long start, end;
    long avgTime = 0;
    int avgNumVertices = 0;
    int avgNumTriangles = 0;

    int iters = atLeast(250);

    Polygon[] polygons = new Polygon[iters];
    System.out.println("generating " + iters + " polygons");
    for (int i=0; i<polygons.length;) {
      numVertices = TestUtil.nextInt(random(), 200000, 500000);
      polygons[i] = GeoTestUtil.createRegularPolygon(0, 0, atLeast(1000000), numVertices);
      avgNumVertices += (numVertices - avgNumVertices) / ++i;
    }

    List l;
    for (int i=1; i<iters; ++i) {
      System.out.println("..");

      start = System.nanoTime();
      l = Tessellator.tessellate(polygons[i-1]);
      end = System.nanoTime();
      avgNumTriangles += (l.size() - avgNumTriangles) / i;
      avgTime += (end - start - avgTime) / i;
      if (i%100 == 0) {
        System.out.println("\niteration: " + i + "\n  avg time:    " + TimeUnit.NANOSECONDS.toMillis(avgTime));
      }
    }

    System.out.println("avg number vertices: " + avgNumVertices);
    System.out.println("avg tessellation time: " + TimeUnit.NANOSECONDS.toMillis(avgTime));
    System.out.println("avg number of triangles: " + avgNumTriangles);
  }

  public static String toSVG(List<Tessellator.Triangle> tessellation) {
    Polygon[] polygons = new Polygon[tessellation.size()];
    // convert tessellation triangles to list of Polygons
    Tessellator.Triangle t;
    for (int i = 0; i < tessellation.size(); ++i) {
      t = tessellation.get(i);
      polygons[i] = new Polygon(
          new double[]{decodeLatitude(t.getAY()), decodeLatitude(t.getBY()), decodeLatitude(t.getCY()), decodeLatitude(t.getAY())},
          new double[]{decodeLongitude(t.getAX()), decodeLongitude(t.getBX()), decodeLongitude(t.getCX()), decodeLongitude(t.getAX())});
    }
    return GeoTestUtil.toSVG(polygons);
  }
}