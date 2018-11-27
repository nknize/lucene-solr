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
import java.util.Iterator;

import org.apache.lucene.geo.GeoUtils;
import org.apache.lucene.geo.parsers.WKBParser;
import org.apache.lucene.geo.parsers.WKTParser;
import org.apache.lucene.store.OutputStreamDataOutput;

/**
 * Represents a MultiPoint object on the earth's surface in decimal degrees.
 *
 * @lucene.experimental
 */
class MultiPoint extends Shape implements Iterable<Point> {
  protected final double[] xVals;
  protected final double[] yVals;

  public MultiPoint(double[] xVals, double[] yVals) {
    this.xVals = xVals.clone();
    this.yVals = yVals.clone();
  }

  /** returns the number of points for this multipoint */
  public int numPoints() {
    return xVals.length;
  }

  /** Returns a copy of the internal x value array */
  public double[] getXValues() {
    return xVals.clone();
  }

  /** Returns a copy of the internal y value array */
  public double[] getYValues() {
    return yVals.clone();
  }

  /** get the x value for the given vertex */
  public double getX(int vertex) {
    checkVertexIndex(vertex);
    return xVals[vertex];
  }

  /** get the y value for the given vertex */
  public double getY(int vertex) {
    checkVertexIndex(vertex);
    return yVals[vertex];
  }

  private void checkVertexIndex(final int i) {
    if (i >= numPoints()) {
      throw new IllegalArgumentException("Index " + i + " is outside the bounds of the " + numPoints() + " vertices ");
    }
  }

  @Override
  public ShapeType type() {
    return ShapeType.MULTIPOINT;
  }

  @Override
  protected Rectangle computeBoundingBox() {
    // compute bounding box
    double minY = Math.min(yVals[0], yVals[yVals.length - 1]);
    double maxY = Math.max(yVals[0], yVals[yVals.length - 1]);
    double minX = Math.min(xVals[0], xVals[xVals.length - 1]);
    double maxX = Math.max(xVals[0], xVals[xVals.length - 1]);

    final int numPts = yVals.length - 1;
    for (int i = 1; i < numPts; ++i) {
      minY = Math.min(yVals[i], minY);
      maxY = Math.max(yVals[i], maxY);
      minX = Math.min(xVals[i], minX);
      maxX = Math.max(xVals[i], maxX);
    }

    return new Rectangle(minX, maxX, minY, maxY);
  }

  @Override
  protected Point computeCenter() {
    return this.boundingBox.center();
  }

  @Override
  public boolean hasArea() {
    return false;
  }

  @Override
  protected double computeArea() {
    return Double.NaN;
  }

  @Override
  public Relation relate(double minX, double maxX, double minY, double maxY) {
    // note: this relate is not used; points are indexed as separate POINT types
    // note: if needed, we could build an in-memory BKD for each MultiPoint type
    throw new UnsupportedOperationException("use Point.relate instead");
  }

  @Override
  public Relation relate(Geometry that) {
    throw new UnsupportedOperationException("use Point.relate instead");
  }

  @Override
  public boolean equals(Object other) {
    if (super.equals(other) == false) return false;
    MultiPoint o = getClass().cast(other);
    return Arrays.equals(xVals, o.xVals) && Arrays.equals(yVals, o.yVals);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Arrays.hashCode(xVals);
    result = 31 * result + Arrays.hashCode(yVals);
    return result;
  }

  @Override
  protected StringBuilder contentToWKT() {
    return coordinatesToWKT(xVals, yVals);
  }

  protected static StringBuilder coordinatesToWKT(final double[] xVals, final double[] yVals) {
    StringBuilder sb = new StringBuilder();
    if (xVals.length == 0) {
      sb.append(WKTParser.EMPTY);
    } else {
      // walk through coordinates:
      sb.append(WKTParser.LPAREN);
      sb.append(Point.coordinateToWKT(xVals[0], yVals[0]));
      for (int i = 1; i < xVals.length; ++i) {
        sb.append(WKTParser.COMMA);
        sb.append(WKTParser.SPACE);
        sb.append(Point.coordinateToWKT(xVals[i], yVals[i]));
      }
      sb.append(WKTParser.RPAREN);
    }

    return sb;
  }

  @Override
  protected void appendWKBContent(OutputStreamDataOutput out) throws IOException {
    out.writeVInt(numPoints());
    pointsToWKB(xVals, yVals, out, true);
  }

  protected static OutputStreamDataOutput pointsToWKB(final double[] xVals, final double[] yVals,
                                                      OutputStreamDataOutput out, boolean writeHeader) throws IOException {
    final int numPoints = xVals.length;
    for (int i = 0; i < numPoints; ++i) {
      if (writeHeader == true) {
        // write header for each coordinate (req. as part of spec for MultiPoints but not LineStrings)
        out.writeVInt(WKBParser.ByteOrder.XDR.ordinal());
        out.writeVInt(ShapeType.POINT.wkbOrdinal());
      }
      // write coordinates
      Point.coordinateToWKB(xVals[i], yVals[i], out);
    }
    return out;
  }

  @Override
  public Iterator<Point> iterator() {
    return new Iterator<Point>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return i < numPoints();
      }

      @Override
      public Point next() {
        return new Point(xVals[i], yVals[i]);
      }
    };
  }
}

