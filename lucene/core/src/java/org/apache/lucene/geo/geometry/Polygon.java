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
package org.apache.lucene.geo.geometry;

import java.text.ParseException;
import java.util.Arrays;

import org.apache.lucene.geo.parsers.SimpleGeoJSONPolygonParser;
import org.apache.lucene.index.PointValues;

/**
 * Represents a closed polygon on the earth's surface.  You can either construct the Polygon directly yourself with {@code double[]}
 * coordinates, or use {@link Polygon#fromGeoJSON} if you have a polygon already encoded as a
 * <a href="http://geojson.org/geojson-spec.html">GeoJSON</a> string.
 * <p>
 * NOTES:
 * <ol>
 *   <li>Coordinates must be in clockwise order, except for holes. Holes must be in counter-clockwise order.
 *   <li>The polygon must be closed: the first and last coordinates need to have the same values.
 *   <li>The polygon must not be self-crossing, otherwise may result in unexpected behavior.
 *   <li>All latitude/longitude values must be in decimal degrees.
 *   <li>Polygons cannot cross the 180th meridian. Instead, use two polygons: one on each side.
 *   <li>For more advanced GeoSpatial indexing and query operations see the {@code spatial-extras} module
 * </ol>
 * @lucene.experimental
 */
public final class Polygon extends Line {
  private final Polygon[] holes;

  /**
   * Creates a new Polygon from the supplied latitude/longitude array, and optionally any holes.
   */
  public Polygon(double[] polyLats, double[] polyLons, Polygon... holes) {
    super(polyLats, polyLons);
    if (holes == null) {
      throw new IllegalArgumentException("holes must not be null");
    }
    if (polyLats[0] != polyLats[polyLats.length-1]) {
      throw new IllegalArgumentException("first and last points of the polygon must be the same (it must close itself): polyLats[0]=" + polyLats[0] + " polyLats[" + (polyLats.length-1) + "]=" + polyLats[polyLats.length-1]);
    }
    if (polyLons[0] != polyLons[polyLons.length-1]) {
      throw new IllegalArgumentException("first and last points of the polygon must be the same (it must close itself): polyLons[0]=" + polyLons[0] + " polyLons[" + (polyLons.length-1) + "]=" + polyLons[polyLons.length-1]);
    }
    for (int i = 0; i < holes.length; i++) {
      Polygon inner = holes[i];
      if (inner.holes.length > 0) {
        throw new IllegalArgumentException("holes may not contain holes: polygons may not nest.");
      }
    }
    this.holes = holes.clone();
  }

  @Override
  public ShapeType type() {
    return ShapeType.POLYGON;
  }

  @Override
  protected void checkLatArgs(final double[] lats) {
    super.checkLatArgs(lats);
    if (lats.length < 4) {
      throw new IllegalArgumentException("at least 4 polygon points required");
    }
  }

  @Override
  protected void checkLonArgs(final double[] lons) {
    super.checkLonArgs(lons);
  }

  /** Returns a copy of the internal latitude array */
  public double[] getPolyLats() {
    return getLats();
  }

  /** Returns a copy of the internal longitude array */
  public double[] getPolyLons() {
    return getLons();
  }

  /** Returns a copy of the internal holes array */
  public Polygon[] getHoles() {
    return holes.clone();
  }

  public int numHoles() {
    return holes.length;
  }

  public Polygon getHole(int i) {
    if (i >= holes.length) {
      throw new IllegalArgumentException("Index " + i + " is outside the bounds of the " + holes.length + " polygon holes");
    }
    return holes[i];
  }

  /** Builds a EdgeTree from multipolygon */
  public static EdgeTree createEdgeTree(Polygon... polygons) {
    EdgeTree components[] = new EdgeTree[polygons.length];
    for (int i = 0; i < components.length; i++) {
      Polygon gon = polygons[i];
      Polygon gonHoles[] = gon.getHoles();
      EdgeTree holes = null;
      if (gonHoles.length > 0) {
        holes = createEdgeTree(gonHoles);
      }
      components[i] = new EdgeTree(gon, holes);
    }
    return EdgeTree.createTree(components, 0, components.length - 1, false);
  }

  @Override
  public Relation relate(double minLat, double maxLat, double minLon, double maxLon) {
    if (tree == null) {
      tree = createEdgeTree(this);
    }
    return tree.relate(minLat, maxLat, minLon, maxLon);
  }

//  @Override
//  public PointValues.Relation relate(GeoShape other) {
//    switch (other.type()) {
//      case POLYGON:
//        return relatePolyPoly(getClass().cast(other));
//    }
//    // not yet implemented
//    throw new UnsupportedOperationException("not yet able to relate other GeoShape types to linestrings");
//  }

//  public PointValues.Relation relatePolyPoly(Polygon other) {
//    tree.relate
//  }

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
    Polygon other = (Polygon) obj;
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

  /** Parses a standard GeoJSON polygon string.  The type of the incoming GeoJSON object must be a Polygon or MultiPolygon, optionally
   *  embedded under a "type: Feature".  A Polygon will return as a length 1 array, while a MultiPolygon will be 1 or more in length.
   *
   *  <p>See <a href="http://geojson.org/geojson-spec.html">the GeoJSON specification</a>. */
  public static Polygon[] fromGeoJSON(String geojson) throws ParseException {
    return new SimpleGeoJSONPolygonParser(geojson).parse();
  }
}
