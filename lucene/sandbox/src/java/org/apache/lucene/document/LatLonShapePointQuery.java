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
package org.apache.lucene.document;

import java.util.Arrays;

import org.apache.lucene.document.LatLonShape.QueryRelation;
import org.apache.lucene.geo.Rectangle;
import org.apache.lucene.geo.Tessellator;
import org.apache.lucene.index.PointValues.Relation;
import org.apache.lucene.util.NumericUtils;

import static org.apache.lucene.geo.GeoEncodingUtils.decodeLatitude;
import static org.apache.lucene.geo.GeoEncodingUtils.decodeLongitude;
import static org.apache.lucene.geo.GeoEncodingUtils.encodeLatitude;
import static org.apache.lucene.geo.GeoEncodingUtils.encodeLongitude;

public class LatLonShapePointQuery extends LatLonShapeQuery {
  final int[][] points;
  final int minX;
  final int maxX;
  final int minY;
  final int maxY;

  public LatLonShapePointQuery(String field, QueryRelation queryRelation, double[][] points) {
    super(field, queryRelation);
    /** point queries do not support within relations, only intersects and disjoint */
    if (queryRelation == QueryRelation.WITHIN) {
      throw new IllegalArgumentException("LatLonShapePointQuery does not support " + QueryRelation.WITHIN + " queries");
    }

    if (points == null) {
      throw new IllegalArgumentException("points must not be null");
    }
    if (points.length == 0) {
      throw new IllegalArgumentException("points must not be empty");
    }

    int xMin = Integer.MAX_VALUE;
    int xMax = Integer.MIN_VALUE;
    int yMin = Integer.MAX_VALUE;
    int yMax = Integer.MIN_VALUE;

    int latEncoded, lonEncoded;
    this.points = new int[points.length][2];
    for (int i = 0; i < points.length; ++i) {
      if (points[i] == null) {
        throw new IllegalArgumentException("points[" + i + "] must not be null");
      }
      latEncoded = encodeLatitude(points[i][0]);
      lonEncoded = encodeLongitude(points[i][1]);
      this.points[i][0] = latEncoded;
      this.points[i][1] = lonEncoded;

      yMin = Math.min(yMin, latEncoded);
      yMax = Math.max(yMax, latEncoded);
      xMin = Math.min(xMin, lonEncoded);
      xMax = Math.max(xMax, lonEncoded);
    }
    this.minX = xMin;
    this.maxX = xMax;
    this.minY = yMin;
    this.maxY = yMax;
  }

  @Override
  protected Relation relateRangeBBoxToQuery(int minXOffset, int minYOffset, byte[] minTriangle,
                                            int maxXOffset, int maxYOffset, byte[] maxTriangle) {
    int minY = NumericUtils.sortableBytesToInt(minTriangle, minYOffset);
    int minX = NumericUtils.sortableBytesToInt(minTriangle, minXOffset);
    int maxY = NumericUtils.sortableBytesToInt(maxTriangle, maxYOffset);
    int maxX = NumericUtils.sortableBytesToInt(maxTriangle, maxXOffset);

    // check point bounding box against range box
    if (boxesAreDisjoint(minX, maxX, minY, maxY, this.minX, this.maxX, this.minY, this.maxY)) {
      return Relation.CELL_OUTSIDE_QUERY;
    }

    // check points against query
    for (int i = 0; i < this.points.length; ++i) {
      if (Rectangle.containsPoint(this.points[i][0], this.points[i][1], minY, maxY, minX, maxX)) {
        return Relation.CELL_CROSSES_QUERY;
      }
    }
    return Relation.CELL_OUTSIDE_QUERY;
  }

  @Override
  protected boolean queryMatches(byte[] t, int[] scratchTriangle, QueryRelation queryRelation) {
    LatLonShape.decodeTriangle(t, scratchTriangle);

    int aX = scratchTriangle[1];
    int bX = scratchTriangle[3];
    int cX = scratchTriangle[5];
    int aY = scratchTriangle[0];
    int bY = scratchTriangle[2];
    int cY = scratchTriangle[4];

    int minX = Math.min(aX, Math.min(bX, cX));
    int maxX = Math.max(aX, Math.max(bX, cX));
    int minY = Math.min(aY, Math.min(bY, cY));
    int maxY = Math.max(aY, Math.max(bY, cY));

    // check triangle bounding box against points bounding box
    if (boxesAreDisjoint(minX, maxX, minY, maxY, this.minX, this.maxX, this.minY, this.maxY)) {
      return false;
    }

    // check points against triangle
    int x, y;
    for (int i = 0; i < this.points.length; ++i) {
      x = this.points[i][1];
      y = this.points[i][0];
      // first check point against triangle bounding box
      if (y < minY || y > maxY || x < minX || x > maxX) {
        continue;
      }
      // check the more expensive operation (pointInTriangle)
      if (Tessellator.pointInTriangle(x, y, aX, aY, bX, bY, cX, cY)) {
        return true;
      }
    }
    return false;
  }

  /** utility method to check if two boxes are disjoint */
  public static boolean boxesAreDisjoint(final int aMinX, final int aMaxX, final int aMinY, final int aMaxY,
                                         final int bMinX, final int bMaxX, final int bMinY, final int bMaxY) {
    return (aMaxX < bMinX || aMinX > bMaxX || aMaxY < bMinY || aMinY > bMaxY);
  }

  @Override
  public String toString(String field) {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append(':');
    if (this.field.equals(field) == false) {
      sb.append(" field=");
      sb.append(this.field);
      sb.append(':');
    }
    sb.append("Point(" + pointsToGeoJSON() + ")");
    return sb.toString();
  }

  private String pointsToGeoJSON() {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    sb.append(decodeLongitude(points[0][1]) + ", " + decodeLatitude(points[0][0]) + "]");
    for (int i = 1; i < points.length; ++i) {
      sb.append(", [");
      sb.append(decodeLongitude(points[i][1]));
      sb.append(", ");
      sb.append(decodeLatitude(points[i][0]));
      sb.append(']');
    }
    return sb.toString();
  }

  @Override
  protected boolean equalsTo(Object o) {
    return super.equalsTo(o) && Arrays.equals(points, ((LatLonShapePointQuery)o).points);
  }

  @Override
  public int hashCode() {
    int hash = super.hashCode();
    hash = 31 * hash + Arrays.hashCode(points);
    return hash;
  }
}
