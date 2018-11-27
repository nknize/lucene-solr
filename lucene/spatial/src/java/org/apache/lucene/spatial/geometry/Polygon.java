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
import java.util.function.Function;

import org.apache.lucene.geo.parsers.WKBParser;
import org.apache.lucene.store.OutputStreamDataOutput;

import static org.apache.lucene.geo.GeoEncodingUtils.decodeLatitude;
import static org.apache.lucene.geo.GeoEncodingUtils.decodeLongitude;

class Polygon extends Line {
  private final Polygon[] holes;
  protected PolygonPredicate predicate;

  /**
   * Creates a new Polygon from the supplied latitude/longitude array, and optionally any holes.
   */
  public Polygon(double[] x, double[] y, Polygon... holes) {
    super(x, y);
    this.holes = holes.clone();
  }

  @Override
  public ShapeType type() {
    return ShapeType.POLYGON;
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

  /** Lazily builds an EdgeTree from multipolygon */
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
  public boolean hasArea() {
    return true;
  }

  @Override
  protected double computeArea() {
    assertEdgeTree();
    return this.edgeTree.getArea();
  }

  public boolean pointInside(int encodedX, int encodedY) {
    return predicate.test(encodedY, encodedX);
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

  protected static StringBuilder polygonToWKT(final Polygon polygon) {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    sb.append(MultiPoint.coordinatesToWKT(polygon.xVals, polygon.yVals));
    Polygon[] holes = polygon.getHoles();
    for (int i = 0; i < holes.length; ++i) {
      sb.append(", ");
      sb.append(MultiPoint.coordinatesToWKT(holes[i].xVals, holes[i].yVals));
    }
    sb.append(')');
    return sb;
  }

  @Override
  protected StringBuilder contentToWKT() {
    return polygonToWKT(this);
  }

  /** Parses a standard GeoJSON polygon string.  The type of the incoming GeoJSON object must be a Polygon or MultiPolygon, optionally
   *  embedded under a "type: Feature".  A Polygon will return as a length 1 array, while a MultiPolygon will be 1 or more in length.
   *
   *  <p>See <a href="http://geojson.org/geojson-spec.html">the GeoJSON specification</a>. */
//  public static Polygon[] fromGeoJSON(String geojson) throws ParseException {
//    return new SimpleGeoJSONPolygonParser(geojson).parse();
//  }

  @Override
  protected void appendWKBContent(OutputStreamDataOutput out) throws IOException {
    polygonToWKB(this, out, false);
  }

  public static void polygonToWKB(final Polygon polygon, OutputStreamDataOutput out,
                                  final boolean writeHeader) throws IOException {
    if (writeHeader == true) {
      out.writeVInt(WKBParser.ByteOrder.XDR.ordinal());
      out.writeVInt(ShapeType.POLYGON.wkbOrdinal());
    }
    int numHoles = polygon.numHoles();
    out.writeVInt(numHoles + 1);  // number rings
    // write shell
    Line.lineToWKB(polygon.xVals, polygon.yVals, out, false);
    // write holes
    Polygon hole;
    for (int i = 0; i < numHoles; ++i) {
      hole = polygon.getHole(i);
      Line.lineToWKB(hole.xVals, hole.yVals, out, false);
    }
  }

  /** A predicate that checks whether a given point is within a polygon. */
  final static class PolygonPredicate extends Predicate {

    final EdgeTree tree;

    private PolygonPredicate(EdgeTree tree, Rectangle boundingBox, Function<Rectangle, Relation> boxToRelation) {
      super(boundingBox, boxToRelation);
      this.tree = tree;
    }

    /** Create a predicate that checks whether points are within a polygon.
     *  It works the same way as {@code DistancePredicate.create}.
     *  @lucene.internal */
    public static PolygonPredicate create(Rectangle boundingBox, EdgeTree tree) {
      final Function<Rectangle, Relation> boxToRelation = box -> tree.relate(
          box.bottom(), box.top(), box.left(), box.right());
      return new PolygonPredicate(tree, boundingBox, boxToRelation);
    }


    public Relation relate(final double minLat, final double maxLat, final double minLon, final double maxLon) {
      return tree.relate(minLat, maxLat, minLon, maxLon);
    }

    /** Check whether the given point is within the considered polygon.
     *  NOTE: this operates directly on the encoded representation of points. */
    public boolean test(int lat, int lon) {
      final int lat2 = ((lat - Integer.MIN_VALUE) >>> latShift);
      if (lat2 < latBase || lat2 >= latBase + maxLatDelta) {
        return false;
      }
      int lon2 = ((lon - Integer.MIN_VALUE) >>> lonShift);
      if (lon2 < lonBase) { // wrap
        lon2 += 1 << (32 - lonShift);
      }
      assert Integer.toUnsignedLong(lon2) >= lonBase;
      assert lon2 - lonBase >= 0;
      if (lon2 - lonBase >= maxLonDelta) {
        return false;
      }

      final int relation = relations[(lat2 - latBase) * maxLonDelta + (lon2 - lonBase)];
      if (relation == Relation.CROSSES.ordinal()) {
        return tree.contains(decodeLatitude(lat), decodeLongitude(lon));
      } else {
        return relation == Relation.WITHIN.ordinal();
      }
    }
  }
}
