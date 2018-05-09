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

import java.util.Arrays;

import org.apache.lucene.geo.GeoUtils;
import org.apache.lucene.index.PointValues.Relation;

/**
 * Created by nknize on 2/27/17.
 */
public class MultiPoint extends GeoShape {
  protected final double[] lats;
  protected final double[] lons;

  public MultiPoint(double[] lats, double[] lons) {
    checkLatArgs(lats);
    checkLonArgs(lons);
    if (lats.length != lons.length) {
      throw new IllegalArgumentException("lats and lons must be equal length");
    }
    for (int i = 0; i < lats.length; i++) {
      GeoUtils.checkLatitude(lats[i]);
      GeoUtils.checkLongitude(lons[i]);
    }
    this.lats = lats.clone();
    this.lons = lons.clone();

    // compute bounding box
    double minLat = Double.POSITIVE_INFINITY;
    double maxLat = Double.NEGATIVE_INFINITY;
    double minLon = Double.POSITIVE_INFINITY;
    double maxLon = Double.NEGATIVE_INFINITY;

    double windingSum = 0d;
    final int numPts = lats.length - 1;
    for (int i = 1, j = 0;i < numPts; j = i++) {
      minLat = Math.min(lats[i], minLat);
      maxLat = Math.max(lats[i], maxLat);
      minLon = Math.min(lons[i], minLon);
      maxLon = Math.max(lons[i], maxLon);
      // compute signed area
      windingSum += (lons[j] - lons[numPts])*(lats[i] - lats[numPts])
          - (lats[j] - lats[numPts])*(lons[i] - lons[numPts]);
    }
    this.minLat = minLat;
    this.maxLat = maxLat;
    this.minLon = minLon;
    this.maxLon = maxLon;
  }

  @Override
  public ShapeType type() {
    return ShapeType.MULTIPOINT;
  }

  protected void checkLatArgs(double[] lats) {
    if (lats == null) {
      throw new IllegalArgumentException("lats must not be null");
    }
    if (lats.length < 2) {
      throw new IllegalArgumentException("at least 2 points are required");
    }
  }

  protected void checkLonArgs(double[] lons) {
    if (lons == null) {
      throw new IllegalArgumentException("lons must not be null");
    }
  }

  public int numPoints() {
    return lats.length;
  }

  private void checkVertexIndex(final int i) {
    if (i >= lats.length) {
      throw new IllegalArgumentException("Index " + i + " is outside the bounds of the " + lats.length + " vertices ");
    }
  }

  public double getVertexLat(int vertex) {
    checkVertexIndex(vertex);
    return lats[vertex];
  }

  public double getVertexLon(int vertex) {
    checkVertexIndex(vertex);
    return lons[vertex];
  }

  /** Returns a copy of the internal latitude array */
  public double[] getLats() {
    return lats.clone();
  }

  /** Returns a copy of the internal longitude array */
  public double[] getLons() {
    return lons.clone();
  }

  public double minLat() {
    return minLat;
  }

  public double maxLat() {
    return maxLat;
  }

  public double minLon() {
    return minLon;
  }

  public double maxLon() {
    return maxLon;
  }

  public Relation relate(double minLat, double maxLat, double minLon, double maxLon) {
    return null;
  }

  public Relation relate(GeoShape shape) {
    return null;
  }

  @Override
  public boolean equals(Object other) {
    if (super.equals(other) == false) return false;
    MultiPoint o = getClass().cast(other);
    return Arrays.equals(lats, o.lats) && Arrays.equals(lons, o.lons);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Arrays.hashCode(lats);
    result = 31 * result + Arrays.hashCode(lons);
    return result;
  }
}
