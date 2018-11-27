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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.lucene.geo.parsers.WKBParser;
import org.apache.lucene.geo.parsers.WKTParser;
import org.apache.lucene.store.OutputStreamDataOutput;

public class GeoPoint extends Point {
  public GeoPoint(final double lat, final double lon) {
    super(lon, lat);
  }

  public double lat() {
    return y;
  }

  public double lon() {
    return x;
  }

  /** Relates another Geo object to this GeoPoint */
  @Override
  public Relation relate(Geometry that) {
    throw new UnsupportedOperationException("GeoPoint does not yet support relation to other Geo objects" );
  }

  /** Relates a geo bounding box to this GeoPoint */
  @Override
  public Relation relate(double minLat, double maxLat, double minLon, double maxLon) {
    if (y < minLat || y > maxLat || x < minLon || x > maxLon) {
      return Relation.DISJOINT;
    }
    return Relation.WITHIN;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    GeoPoint point = (GeoPoint) o;

    if (Double.compare(point.x, x) != 0) return false;
    return Double.compare(point.y, y) == 0;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    long temp;
    temp = Double.doubleToLongBits(x);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(y);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return y + ", " + x;
  }

  @Override
  public String toWKT() {
    StringBuilder sb = new StringBuilder();
    sb.append(type().wktName());
    sb.append(WKTParser.SPACE);
    sb.append(Point.coordinateToWKT(x, y));
    return sb.toString();
  }


  @Override
  public ByteArrayOutputStream toWKB(ByteArrayOutputStream reuse) {
    if (reuse == null) {
      reuse = new ByteArrayOutputStream();
    }
    try (OutputStreamDataOutput out = new OutputStreamDataOutput(reuse)) {
      out.writeVInt(WKBParser.ByteOrder.XDR.ordinal()); // byteOrder
      out.writeVInt(type().wkbOrdinal());     // shapeType ordinal
      Point.coordinateToWKB(x, y, out);
    } catch (IOException e) {
      throw new RuntimeException(e);  // not possible
    }
    return reuse;
  }
}
