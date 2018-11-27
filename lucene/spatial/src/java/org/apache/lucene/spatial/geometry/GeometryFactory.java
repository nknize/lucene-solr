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

import org.apache.lucene.spatial.geometry.Geometry.CRSType;

public abstract class GeometryFactory {
  protected CRSType crsType;
  protected Rectangle worldBounds;

  // no instance
  protected GeometryFactory(CRSType crsType, double minX, double maxX, double minY, double maxY) {
    this.crsType = crsType;
    this.worldBounds = new Rectangle(minX, maxX, minY, maxY);
  }

  public CRSType crsType() {
    return this.crsType;
  }

  public Rectangle worldBounds() {
    return this.worldBounds;
  }

  protected abstract Geometry makePoint(double x, double y);
  protected abstract Shape makeMultiPoint(double[] x, double[] y);
  protected abstract Shape makeLine(double[] x, double[] y);
  protected abstract Shape makeMultiLine(Shape... lines);
  protected abstract Shape makePolygon(double[] x, double[] y, Shape... holes);
  protected abstract Shape makeMultiPolygon(Shape... polygons);
  protected abstract Shape makeShapeCollection(Shape... shapes);


  /** return a new Point (Geocentric) or GeoPoint (Geodetic) */
  public Geometry newPoint(double x, double y) {
    validateValue(x, y);
    return this.makePoint(x, y);
  }

  protected Shape newMultiPoint(double[] x, double[] y) {
    validateMultiValue(x, y, 2);
    return makeMultiPoint(x, y);
  }

  protected Shape newLine(double[] x, double[] y) {
    validateMultiValue(x, y, 2);
    return makeLine(x, y);
  }

  protected Shape newMultiLine(Shape... lines) {
    validateLines(lines);
    return makeMultiLine(lines);
  }

  protected Shape newPolygon(double[] x, double[] y, Shape... holes) {
    validateMultiValue(x, y, 4);
    validateClosed(x, y);
    for (int i = 0; i < holes.length; i++) {
      Shape inner = holes[i];
      validatePolygonHole(inner);
    }
    return makePolygon(x, y, holes);
  }

  protected Shape newMultiPolygon(Shape... polygons) {
    return makeMultiPolygon(polygons);
  }

  protected Shape newShapeCollection(Shape... shapes ) {
    return makeShapeCollection(shapes);
  }

  protected void validateLines(Shape... lines) {
    for (Shape line : lines) {
      if (line instanceof Line == false) {
        throw new IllegalArgumentException("expected ");
      }
    }
  }

  protected void validatePolygonHole(Shape hole) {
    if (hole instanceof Polygon == false) {
      throw new IllegalArgumentException("hole must be a " + ShapeType.POLYGON);
    }

    Polygon h = (Polygon)hole;
    if (h.numHoles() > 0) {
      throw new IllegalArgumentException("holes may not contain holes: polygons may not nest.");
    }
  }

  /** validates the x value against the world bounds */
  protected void checkX(double x) {
    if (Double.isNaN(x) || x < worldBounds.left() || x > worldBounds.right()) {
      throw new IllegalArgumentException("invalid x value " +  x
          + "; must be between " + worldBounds.left() + " and " + worldBounds.right());
    }
  }

  /** validates the y value against the world bounds */
  protected void checkY(double y) {
    if (Double.isNaN(y) || y < worldBounds.bottom() || y > worldBounds.top()) {
      throw new IllegalArgumentException("invalid y value " +  y
          + "; must be between " + worldBounds.bottom() + " and " + worldBounds.top());
    }
  }

  protected void validateValue(double x, double y) {
    checkX(x);
    checkY(y);
  }

  protected void validateMultiValue(double[] x, double[] y, int minLength) {
    checkMultiValueX(x, minLength);
    checkMultiValueY(y, minLength);
    for (int i = 0; i < x.length; ++i) {
      validateValue(x[i], y[i]);
    }
  }

  protected void validateClosed(double[] x, double[] y) {
    if (x[0] != x[x.length-1]) {
      throw new IllegalArgumentException("first and last points of the polygon must be the same (it must close itself): x[0]=" + x[0] + " x[" + (x.length-1) + "]=" + x[x.length-1]);
    }
    if (y[0] != y[y.length-1]) {
      throw new IllegalArgumentException("first and last points of the polygon must be the same (it must close itself): y[0]=" + y[0] + " y[" + (y.length-1) + "]=" + y[y.length-1]);
    }
  }

  /** validates multi x values */
  protected void checkMultiValueX(double[] x, int minLength) {
    if (x == null) {
      throw new IllegalArgumentException("x values must not be null");
    }
    if (x.length < minLength) {
      throw new IllegalArgumentException("at least " + minLength + " x values are required");
    }
  }

  /** validates multi y values */
  protected void checkMultiValueY(double[] y, int minLength) {
    if (y == null) {
      throw new IllegalArgumentException("y values must not be null");
    }
    if (y.length < minLength) {
      throw new IllegalArgumentException("at least " + minLength + " y values are required");
    }
  }

  /** returns whether this Factory is Geodetic (lat, lon) or Geocentric (x, y) */
  public boolean isGeocentric() {
    return crsType == CRSType.GEOCENTRIC;
  }
}
