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

import org.apache.lucene.index.PointValues;

/**
 * @lucene.experimental
 */
public abstract class GeoShape {
  protected double minLat;
  protected double maxLat;
  protected double minLon;
  protected double maxLon;

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

  public abstract ShapeType type();
  public abstract Relation relate(double minLat, double maxLat, double minLon, double maxLon);
  public abstract Relation relate(GeoShape shape);

  interface ConnectedComponent {
    EdgeTree createEdgeTree();
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || getClass() != other.getClass()) return false;
    GeoShape o = getClass().cast(other);
    return minLat == o.minLat
        && minLon == o.minLon
        && maxLat == o.maxLat
        && maxLon == o.maxLon;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(minLat);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(maxLat);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(minLon);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(maxLon);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  public enum Relation {
    DISJOINT(PointValues.Relation.CELL_OUTSIDE_QUERY),
    INTERSECTS(PointValues.Relation.CELL_CROSSES_QUERY),
    CONTAINS(PointValues.Relation.CELL_CROSSES_QUERY),
    WITHIN(PointValues.Relation.CELL_INSIDE_QUERY),
    CROSSES(PointValues.Relation.CELL_CROSSES_QUERY);

    // used to translate between PointValues.Relation and full geo relations
    private final PointValues.Relation pointsRelation;


    Relation(PointValues.Relation pointsRelation) {
      this.pointsRelation = pointsRelation;
    }

    public PointValues.Relation toPointsRelation() {
      return pointsRelation;
    }
  }
}
