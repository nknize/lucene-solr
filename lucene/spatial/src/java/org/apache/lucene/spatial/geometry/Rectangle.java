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

import org.apache.lucene.geo.parsers.WKTParser;
import org.apache.lucene.store.OutputStreamDataOutput;

public class Rectangle extends Shape {
  /** minimum x value */
  private final double minX;
  /** minimum y value */
  private final double minY;
  /** maximum x value */
  private final double maxX;
  /** maximum y value */
  private final double maxY;

  public Rectangle(final double minX, final double maxX, final double minY, final double maxY) {
    super();
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;
  }

  @Override
  protected Rectangle computeBoundingBox() {
    return this;
  }

  @Override
  protected double computeArea() {
    return getWidth() * getHeight();
  }

  @Override
  protected Point computeCenter() {
    // compute the center of the rectangle
    final double cntrY = getHeight() / 2 + minY;
    double cntrX = getWidth() / 2 + minX;
    return new Point(cntrX, cntrY);
  }

  @Override
  public boolean hasArea() {
    return minX != maxX && minY != maxY;
  }


  public double getWidth() {
    return maxX - minX;
  }

  public double getHeight() {
    return maxY - minY;
  }

  public Point getCenter() {
    return this.center;
  }

  @Override
  public ShapeType type() {
    return ShapeType.ENVELOPE;
  }


  @Override
  public Relation relate(Geometry that) {
    Relation r = that.relate(that.left(), that.right(), that.bottom(), that.top());
    return r.transpose();
  }

  @Override
  public Relation relate(double minX, double maxX, double minY, double maxY) {
    if (rectDisjoint(this.minX, this.maxX, this.minY, this.maxY, minX, maxX, minY, maxY)) {
      return Relation.DISJOINT;
    } else if (rectWithin(this.minX, this.maxX, this.minY, this.maxY, minX, maxX, minY, maxY)) {
      return Relation.WITHIN;
    } else if (this.minX > minX || this.minY < minY || this.maxX > maxX || this.maxY > maxY) {
      return Relation.CROSSES;
    }
    return Relation.CONTAINS;
  }

  /** Computes whether two rectangles are disjoint */
  protected static boolean rectDisjoint(final double aMinX, final double aMaxX, final double aMinY, final double aMaxY,
                                        final double bMinX, final double bMaxX, final double bMinY, final double bMaxY) {
    return aMaxY < bMinY || aMinY > bMaxY || aMaxX < bMinX || aMinX > bMaxX;
  }

  /** Computes whether the first (a) rectangle is wholly within another (b) rectangle (shared boundaries allowed) */
  protected static boolean rectWithin(final double aMinX, final double aMaxX, final double aMinY, final double aMaxY,
                                      final double bMinX, final double bMaxX, final double bMinY, final double bMaxY) {
    return !(aMinX < bMinX || aMinY < bMinY || aMaxX > bMaxX || aMaxY > bMaxY);
  }

  @Override
  protected StringBuilder contentToWKT() {
    StringBuilder sb = new StringBuilder();

    sb.append(WKTParser.LPAREN);
    // minX, maxX, maxY, minY
    sb.append(minX);
    sb.append(WKTParser.COMMA);
    sb.append(WKTParser.SPACE);
    sb.append(maxX);
    sb.append(WKTParser.COMMA);
    sb.append(WKTParser.SPACE);
    sb.append(maxY);
    sb.append(WKTParser.COMMA);
    sb.append(WKTParser.SPACE);
    sb.append(minY);
    sb.append(WKTParser.RPAREN);

    return sb;
  }

  @Override
  protected void appendWKBContent(OutputStreamDataOutput out) throws IOException {
    out.writeVLong(Double.doubleToRawLongBits(minX));
    out.writeVLong(Double.doubleToRawLongBits(maxX));
    out.writeVLong(Double.doubleToRawLongBits(maxY));
    out.writeVLong(Double.doubleToRawLongBits(minY));
  }



  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append("Rectangle(lat=");
    b.append(minX);
    b.append(" TO ");
    b.append(maxX);
    b.append(" lon=");
    b.append(minY);
    b.append(" TO ");
    b.append(maxY);
    b.append(")");

    return b.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Rectangle rectangle = (Rectangle) o;

    if (Double.compare(rectangle.minX, minX) != 0) return false;
    if (Double.compare(rectangle.maxX, maxX) != 0) return false;
    if (Double.compare(rectangle.maxY, maxY) != 0) return false;
    return Double.compare(rectangle.minY, minY) == 0;

  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(minX);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(minY);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(maxX);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(maxY);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }
}
