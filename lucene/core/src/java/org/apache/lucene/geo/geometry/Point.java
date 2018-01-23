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
package org.apache.lucene.geo.geometry;

import org.apache.lucene.geo.GeoUtils;

/**
 * Created by nknize on 2/28/17.
 */
public class Point extends GeoShape {
  protected final double lat;
  protected final double lon;

  public Point(double lat, double lon) {
    GeoUtils.checkLatitude(lat);
    GeoUtils.checkLongitude(lon);
    this.lat = lat;
    this.lon = lon;
    this.minLat = lat;
    this.minLon = lon;
    this.maxLat = lat;
    this.maxLon = lon;
  }

  @Override
  public ShapeType type() {
    return ShapeType.POINT;
  }

  public double lat() {
    return lat;
  }

  public double lon() {
    return lon;
  }

  public double minLat() {
    return lat;
  }

  public double maxLat() {
    return lat;
  }

  public double minLon() {
    return lon;
  }

  public double maxLon() {
    return lon;
  }

  public Relation relate(double minLat, double maxLat, double minLon, double maxLon) {
    if (lat < minLat || lat > maxLat || lon < minLon || lon > maxLon) {
      return Relation.DISJOINT;
    }
    return Relation.INTERSECTS;
  }

  public Relation relate(GeoShape shape) {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    Point point = (Point) o;

    if (Double.compare(point.lat, lat) != 0) return false;
    return Double.compare(point.lon, lon) == 0;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    long temp;
    temp = Double.doubleToLongBits(lat);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(lon);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }
}
