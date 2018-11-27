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
import java.util.function.Function;

import org.apache.lucene.geo.GeoUtils;
import org.apache.lucene.spatial.util.GeoRelationUtils;
import org.apache.lucene.store.OutputStreamDataOutput;
import org.apache.lucene.util.SloppyMath;

import static org.apache.lucene.geo.GeoEncodingUtils.decodeLatitude;
import static org.apache.lucene.geo.GeoEncodingUtils.decodeLongitude;

public class GeoCircle extends GeoShape {
  private final double lat;
  private final double lon;
  private final double radiusMeters;
  private DistancePredicate predicate;

  public GeoCircle(final double lat, final double lon, final double radiusMeters) {
    this.lat = lat;
    this.lon = lon;
    this.radiusMeters = radiusMeters;
  }

  public double getCenterLat() {
    return lat;
  }

  public double getCenterLon() {
    return lon;
  }

  public double getRadiusMeters() {
    return radiusMeters;
  }

  @Override
  protected Rectangle computeBoundingBox() {
    return GeoRectangle.fromPointDistance(lat, lon, radiusMeters);
  }

  protected Point computeCenter() {
    return new Point(lon, lat);
  }

  protected double computeArea() {
    return radiusMeters * radiusMeters * StrictMath.PI;
  }

  @Override
  public ShapeType type() {
    return ShapeType.CIRCLE;
  }

  @Override
  public boolean hasArea() {
    return true;
  }

  @Override
  public Relation relate(double minLat, double maxLat, double minLon, double maxLon) {
    return predicate().relate(minLat, maxLat, minLon, maxLon);
  }

  @Override
  public Relation relate(Geometry shape) {
    throw new UnsupportedOperationException("not yet able to relate other GeoShape types to circles");
  }

  public boolean pointInside(final int encodedLat, final int encodedLon) {
    return predicate().test(encodedLat, encodedLon);
  }

  private DistancePredicate predicate() {
    if (predicate == null) {
      predicate = DistancePredicate.create(lat, lon, radiusMeters);
    }
    return predicate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    GeoCircle circle = (GeoCircle) o;
    if (Double.compare(circle.lat, lat) != 0) return false;
    if (Double.compare(circle.lon, lon) != 0) return false;
    if (Double.compare(circle.radiusMeters, radiusMeters) != 0) return false;
    return predicate.equals(circle.predicate);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    long temp;
    temp = Double.doubleToLongBits(lat);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(lon);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(radiusMeters);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + predicate.hashCode();
    return result;
  }

  @Override
  public String toWKT() {
    throw new UnsupportedOperationException("The WKT spec does not support CIRCLE geometry");
  }


  @Override
  protected StringBuilder contentToWKT() {
    throw new UnsupportedOperationException("The WKT spec does not support CIRCLE geometry");
  }

  @Override
  protected void appendWKBContent(OutputStreamDataOutput out) throws IOException {
    out.writeVLong(Double.doubleToRawLongBits(lat));
    out.writeVLong(Double.doubleToRawLongBits(lon));
    out.writeVLong(Double.doubleToRawLongBits(radiusMeters));
  }

  /** A predicate that checks whether a given point is within a distance of another point. */
  final static class DistancePredicate extends Predicate {

    private final double lat, lon;
    private final double distanceKey;
    private final double axisLat;

    private DistancePredicate(double lat, double lon, double distanceKey, double axisLat, Rectangle boundingBox,
                              Function<Rectangle, Relation> boxToRelation) {
      super(boundingBox, boxToRelation);
      this.lat = lat;
      this.lon = lon;
      this.distanceKey = distanceKey;
      this.axisLat = axisLat;
    }

    /** Create a predicate that checks whether points are within a distance of a given point.
     *  It works by computing the bounding box around the circle that is defined
     *  by the given points/distance and splitting it into between 1024 and 4096
     *  smaller boxes (4096*0.75^2=2304 on average). Then for each sub box, it
     *  computes the relation between this box and the distance query. Finally at
     *  search time, it first computes the sub box that the point belongs to,
     *  most of the time, no distance computation will need to be performed since
     *  all points from the sub box will either be in or out of the circle.
     *  @lucene.internal */
    static DistancePredicate create(double lat, double lon, double radiusMeters) {
      final Rectangle boundingBox = GeoRectangle.fromPointDistance(lat, lon, radiusMeters);
      final double axisLat = GeoRectangle.axisLat(lat, radiusMeters);
      final double distanceSortKey = GeoUtils.distanceQuerySortKey(radiusMeters);
      final Function<Rectangle, Relation> boxToRelation = box -> GeoRelationUtils.relate(
          box.bottom(), box.top(), box.left(), box.right(), lat, lon, distanceSortKey, axisLat);
      return new DistancePredicate(lat, lon, distanceSortKey, axisLat, boundingBox, boxToRelation);
    }

    public Relation relate(double minLat, double maxLat, double minLon, double maxLon) {
      return GeoRelationUtils.relate(minLat, maxLat, minLon, maxLon, lat, lon, distanceKey, axisLat);
    }

    /** Check whether the given point is within a distance of another point.
     *  NOTE: this operates directly on the encoded representation of points. */
    public boolean test(int lat, int lon) {
      final int lat2 = ((lat - Integer.MIN_VALUE) >>> latShift);
      if (lat2 < latBase || lat2 >= latBase + maxLatDelta) {
        return false;
      }
      int lon2 = ((lon - Integer.MIN_VALUE) >>> lonShift);
      if (lon2 < lonBase) { // wrap
        lon2 += 1 << (32 - lonShift);
      }
      assert Integer.toUnsignedLong(lon2) >= lonBase;
      assert lon2 - lonBase >= 0;
      if (lon2 - lonBase >= maxLonDelta) {
        return false;
      }

      final int relation = relations[(lat2 - latBase) * maxLonDelta + (lon2 - lonBase)];
      if (relation == Relation.CROSSES.ordinal()) {
        return SloppyMath.haversinSortKey(
            decodeLatitude(lat), decodeLongitude(lon),
            this.lat, this.lon) <= distanceKey;
      } else {
        return relation == Relation.WITHIN.ordinal();
      }
    }
  }
}
