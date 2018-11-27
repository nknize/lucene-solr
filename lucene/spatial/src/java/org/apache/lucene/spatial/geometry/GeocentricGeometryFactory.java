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

public abstract class GeocentricGeometryFactory extends GeometryFactory {

  public static GeocentricGeometryFactory DEFAULT = new GeocentricGeometryFactory(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
      Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY) {
  };

  public GeocentricGeometryFactory(double minX, double maxX, double minY, double maxY) {
    super(CRSType.GEOCENTRIC, minX, maxX, minY, maxY);
  }

  protected Point makePoint(double x, double y) {
    return new Point(x, y);
  }
  protected MultiPoint makeMultiPoint(double[] x, double[] y) {
    return new MultiPoint(x, y);
  }
  protected Line makeLine(double[] x, double[] y) {
    return new Line(x, y);
  }
  protected MultiLine makeMultiLine(Shape... lines) {
    return new MultiLine((Line[])lines);
  }
  protected Polygon makePolygon(double[] x, double[] y, Shape... holes) {
    return new Polygon(x, y, (Polygon[])holes);
  }
  protected MultiPolygon makeMultiPolygon(Shape... polygons) {
    return new MultiPolygon((Polygon[])polygons);
  }
  protected ShapeCollection makeShapeCollection(Shape... shapes) {
    return new ShapeCollection(shapes);
  }
}
