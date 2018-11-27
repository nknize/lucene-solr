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
package org.apache.lucene.spatial.geometry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.lucene.geo.GeoUtils;
import org.apache.lucene.geo.parsers.WKBParser;
import org.apache.lucene.geo.parsers.WKTParser;
import org.apache.lucene.store.OutputStreamDataOutput;

/**
 * Represents a Point on the earth's surface in decimal degrees.
 *
 * @lucene.experimental
 */
public class Point implements Geometry {
  protected final double x;
  protected final double y;

  public Point(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public double x() {
    return x;
  }

  public double y() {
    return y;
  }

  @Override
  public ShapeType type() {
    return ShapeType.POINT;
  }

  @Override
  public Rectangle getBoundingBox() {
    throw new UnsupportedOperationException(type() + " does not have a bounding box");
  }

  @Override
  public double left() {
    return x;
  }

  @Override
  public double right() {
    return x;
  }

  @Override
  public double bottom() {
    return y;
  }

  @Override
  public double top() {
    return y;
  }

  @Override
  public Point center() {
    return this;
  }

  @Override
  public boolean hasArea() {
    return false;
  }

  @Override
  public double area() {
    return Double.NaN;
  }

  @Override
  public Relation relate(double minX, double maxX, double minY, double maxY) {
    if (x < minX || x > maxX || y < minY || y > maxY) {
      return Relation.DISJOINT;
    }
    return Relation.WITHIN;
  }

  @Override
  public Relation relate(Geometry shape) {
    throw new UnsupportedOperationException(type() + " does not support relate with other Geometry");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    Point point = (Point) o;

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
  public String toWKT() {
    StringBuilder sb = new StringBuilder();
    sb.append(type().wktName());
    sb.append(WKTParser.SPACE);
    sb.append(coordinateToWKT(x, y));
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
      coordinateToWKB(x, y, out);
    } catch (IOException e) {
      throw new RuntimeException(e);  // not possible
    }
    return reuse;
  }

  protected static StringBuilder coordinateToWKT(final double x, final double y) {
    final StringBuilder sb = new StringBuilder();
    sb.append(x + WKTParser.SPACE + y);
    return sb;
  }

  public static OutputStreamDataOutput coordinateToWKB(double x, double y, OutputStreamDataOutput out) throws IOException {
    out.writeVLong(Double.doubleToRawLongBits(x));  // lon
    out.writeVLong(Double.doubleToRawLongBits(y));  // lat
    return out;
  }
}
