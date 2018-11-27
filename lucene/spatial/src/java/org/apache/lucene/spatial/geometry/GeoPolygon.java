/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.lucene.spatial.geometry;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.geo.parsers.WKBParser;
import org.apache.lucene.spatial.geometry.Polygon.PolygonPredicate;
import org.apache.lucene.store.OutputStreamDataOutput;

public class GeoPolygon extends GeoLine {
  final protected GeoPolygon[] holes;
  protected PolygonPredicate predicate;

  public GeoPolygon(double[] lats, double[] lons, GeoPolygon... holes) {
    super(lats, lons);
    this.holes = holes;
  }

  @Override
  public ShapeType type() {
    return ShapeType.POLYGON;
  }

  /** Returns a copy of the internal holes array */
  public GeoPolygon[] getHoles() {
    return holes.clone();
  }

  public int numHoles() {
    return holes.length;
  }

  public GeoPolygon getHole(int i) {
    if (i >= holes.length) {
      throw new IllegalArgumentException("Index " + i + " is outside the bounds of the " + holes.length + " polygon holes");
    }
    return holes[i];
  }

  /** Lazily builds an EdgeTree from multipolygon */
  public static EdgeTree createEdgeTree(GeoPolygon... polygons) {
    EdgeTree components[] = new EdgeTree[polygons.length];
    for (int i = 0; i < components.length; i++) {
      GeoPolygon gon = polygons[i];
      GeoPolygon gonHoles[] = gon.getHoles();
      EdgeTree holes = null;
      if (gonHoles.length > 0) {
        holes = createEdgeTree(gonHoles);
      }
      components[i] = new EdgeTree(gon, holes);
    }
    return EdgeTree.createTree(components, 0, components.length - 1, false);
  }

  @Override
  public boolean hasArea() {
    return true;
  }

  @Override
  protected double computeArea() {
    assertEdgeTree();
    return this.edgeTree.getArea();
  }

  public boolean pointInside(int encodedLat, int encodedLon) {
    return predicate.test(encodedLat, encodedLon);
  }

  @Override
  public Relation relate(double minLat, double maxLat, double minLon, double maxLon) {
    assertEdgeTree();
    Relation r = edgeTree.relate(minLat, maxLat, minLon, maxLon);
    return r.transpose();
  }

  protected void assertEdgeTree() {
    if (this.edgeTree == null) {
      this.predicate = PolygonPredicate.create(this.boundingBox, createEdgeTree(this));
      this.edgeTree = predicate.tree;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Arrays.hashCode(holes);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (super.equals(obj) == false) return false;
    GeoPolygon other = (GeoPolygon) obj;
    if (!Arrays.equals(holes, other.holes)) return false;
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    if (holes.length > 0) {
      sb.append(", holes=");
      sb.append(Arrays.toString(holes));
    }
    return sb.toString();
  }

  @Override
  protected StringBuilder contentToWKT() {
    return polygonToWKT(this);
  }

  protected static StringBuilder polygonToWKT(final GeoPolygon polygon) {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    sb.append(MultiPoint.coordinatesToWKT(polygon.lons, polygon.lats));
    GeoPolygon[] holes = polygon.getHoles();
    for (int i = 0; i < holes.length; ++i) {
      sb.append(", ");
      sb.append(MultiPoint.coordinatesToWKT(holes[i].lons, holes[i].lats));
    }
    sb.append(')');
    return sb;
  }

  @Override
  protected void appendWKBContent(OutputStreamDataOutput out) throws IOException {
    polygonToWKB(this, out, false);
  }

  public static void polygonToWKB(final GeoPolygon polygon, OutputStreamDataOutput out,
                                  final boolean writeHeader) throws IOException {
    if (writeHeader == true) {
      out.writeVInt(WKBParser.ByteOrder.XDR.ordinal());
      out.writeVInt(ShapeType.POLYGON.wkbOrdinal());
    }
    int numHoles = polygon.numHoles();
    out.writeVInt(numHoles + 1);  // number rings
    // write shell
    Line.lineToWKB(polygon.lons, polygon.lats, out, false);
    // write holes
    GeoPolygon hole;
    for (int i = 0; i < numHoles; ++i) {
      hole = polygon.getHole(i);
      Line.lineToWKB(hole.lons, hole.lats, out, false);
    }
  }

}
