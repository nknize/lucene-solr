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

import org.apache.lucene.store.OutputStreamDataOutput;

class MultiGeoPoint extends GeoShape implements Iterable<GeoPoint> {
  // todo support geocentric projection in x, y variables
  protected final double[] lats;
  protected final double[] lons;

  public MultiGeoPoint(double[] lats, double[] lons) {
    // for now, don't allocate any unnecessary space for lat and lon values
    this.lats = lats.clone();
    this.lons = lons.clone();
  }

  public int numPoints() {
    return lats.length;
  }

  public double[] getLats() {
    return lats.clone();
  }

  public double[] getLons() {
    return lons.clone();
  }

  @Override
  public ShapeType type() {
    return ShapeType.MULTIPOINT;
  }

  @Override
  protected Rectangle computeBoundingBox() {
    // compute bounding box
    double minLat = Math.min(lats[0], lats[lats.length - 1]);
    double maxLat = Math.max(lats[0], lats[lats.length - 1]);
    double minLon = Math.min(lons[0], lons[lons.length - 1]);
    double maxLon = Math.max(lons[0], lons[lons.length - 1]);

    final int numPts = lats.length - 1;
    for (int i = 1; i < numPts; ++i) {
      minLat = Math.min(lats[i], minLat);
      maxLat = Math.max(lats[i], maxLat);
      minLon = Math.min(lons[i], minLon);
      maxLon = Math.max(lons[i], maxLon);
    }

    return new Rectangle(minLon, maxLon, minLat, maxLat);
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
  public Relation relate(Geometry that) {
    throw new UnsupportedOperationException("use GeoPoint.relate instead");
  }


  @Override
  public Relation relate(double minLat, double maxLat, double minLon, double maxLon) {
    // note: this relate is not used; points are indexed as separate POINT types
    // note: if needed, we could build an in-memory BKD for each MultiPoint type
    throw new UnsupportedOperationException("use GeoPoint.relate instead");
  }

  @Override
  public boolean equals(Object other) {
    if (super.equals(other) == false) return false;
    MultiGeoPoint o = getClass().cast(other);
    return Arrays.equals(lats, o.lats) && Arrays.equals(lons, o.lats);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Arrays.hashCode(lats);
    result = 31 * result + Arrays.hashCode(lons);
    return result;
  }

  @Override
  protected StringBuilder contentToWKT() {
    return MultiPoint.coordinatesToWKT(lons, lats);
  }

  @Override
  protected void appendWKBContent(OutputStreamDataOutput out) throws IOException {
    out.writeVInt(numPoints());
    MultiPoint.pointsToWKB(lons, lats, out, true);
  }

  @Override
  public Iterator<GeoPoint> iterator() {
    return new Iterator<GeoPoint>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return i < numPoints();
      }

      @Override
      public GeoPoint next() {
        return new GeoPoint(lats[i], lons[i]);
      }
    };
  }
}
