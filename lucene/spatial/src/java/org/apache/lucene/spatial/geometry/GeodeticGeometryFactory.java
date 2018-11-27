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

import static org.apache.lucene.spatial.geometry.Geometry.CRSType;
import static org.apache.lucene.geo.GeoUtils.MAX_LAT_INCL;
import static org.apache.lucene.geo.GeoUtils.MAX_LON_INCL;
import static org.apache.lucene.geo.GeoUtils.MIN_LAT_INCL;
import static org.apache.lucene.geo.GeoUtils.MIN_LON_INCL;
import static org.apache.lucene.geo.GeoUtils.checkLatitude;
import static org.apache.lucene.geo.GeoUtils.checkLongitude;


public abstract class GeodeticGeometryFactory extends GeometryFactory {

  public static GeodeticGeometryFactory DEFAULT = new GeodeticGeometryFactory(
      MIN_LAT_INCL, MAX_LAT_INCL, MIN_LON_INCL, MAX_LON_INCL) {};

  protected GeodeticGeometryFactory(final double minLat, final double maxLat, final double minLon, final double maxLon) {
    super(CRSType.GEODETIC, minLon, maxLon, minLat, maxLat);
  }

  /// public factory methods /////
  public GeoPoint newGeoPoint(double lat, double lon) {
    return (GeoPoint)newPoint(lon, lat);
  }

  public MultiGeoPoint newMultiGeoPoint(double[] lat, double[] lon) {
    return (MultiGeoPoint)newMultiPoint(lon, lat);
  }

  public GeoLine newGeoLine(double[] lat, double[] lon) {
    return (GeoLine)newLine(lon, lat);
  }

  public MultiGeoLine newMultiGeoLine(GeoLine... lines) {
    return (MultiGeoLine)newMultiLine(lines);
  }

  public GeoPolygon newGeoPolygon(double[] lats, double[] lons, GeoPolygon... holes) {
    return (GeoPolygon)newPolygon(lons, lats, holes);
  }

  public MultiGeoPolygon newMultiGeoPolygon(GeoPolygon... polygons) {
    return (MultiGeoPolygon)newMultiPolygon(polygons);
  }

  public GeoShapeCollection newGeoShapeCollection(Shape... shapes) {
    return (GeoShapeCollection)newShapeCollection(shapes);
  }

  /// overridden utility methods /////
  @Override
  protected GeoPoint makePoint(double x, double y) {
    return new GeoPoint(y, x);
  }

  @Override
  protected MultiGeoPoint makeMultiPoint(double[] x, double[] y) {
    return new MultiGeoPoint(y, x);
  }

  @Override
  protected GeoLine makeLine(double[] x, double[] y) {
    return new GeoLine(y, x);
  }

  @Override
  protected MultiGeoLine makeMultiLine(Shape... lines) {
    return new MultiGeoLine((GeoLine[])lines);
  }

  @Override
  protected GeoPolygon makePolygon(double[] x, double[] y, Shape... holes) {
    return new GeoPolygon(y, x, (GeoPolygon[])holes);
  }

  @Override
  protected MultiGeoPolygon makeMultiPolygon(Shape... polygons) {
    return new MultiGeoPolygon((GeoPolygon[]) polygons);
  }

  @Override
  protected GeoShapeCollection makeShapeCollection(Shape... shapes) {
    return new GeoShapeCollection((GeoShape[]) shapes);
  }

  @Override
  public void checkX(double x) {
    checkLongitude(x);
  }

  @Override
  public void checkY(double y) {
    checkLatitude(y);
  }

  @Override
  protected void checkMultiValueX(double[] lons, int minLength) {
    if (lons == null) {
      throw new IllegalArgumentException("longitude values must not be null");
    }
    if (lons.length < minLength) {
      throw new IllegalArgumentException("at least " + minLength + " longitude values are required");
    }
  }

  @Override
  protected void checkMultiValueY(double[] lats, int minLength) {
    if (lats == null) {
      throw new IllegalArgumentException("latitude values must not be null");
    }
    if (lats.length < minLength) {
      throw new IllegalArgumentException("at least " + minLength + " latitude values are required");
    }
  }

  @Override
  protected void validatePolygonHole(Shape hole) {
    if (hole instanceof GeoPolygon == false) {
      throw new IllegalArgumentException("hole must be a " + ShapeType.POLYGON);
    }

    GeoPolygon h = (GeoPolygon)hole;
    if (h.numHoles() > 0) {
      throw new IllegalArgumentException("holes may not contain holes: polygons may not nest.");
    }
  }
}
