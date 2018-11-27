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

import java.io.ByteArrayOutputStream;

import org.apache.lucene.index.PointValues;

/**
 * @lucene.experimental
 */
public interface Geometry {
  /** type of geometry */
  ShapeType type();

  /** returns the bounding box of the geometry */
  Rectangle getBoundingBox();

  /** returns minimum x value */
  double left();
  /** returns maximum x value */
  double right();
  /** returns minimum y value */
  double bottom();
  /** returns maximum y value */
  double top();

  /** returns the geometry center */
  Point center();
  /** returns whether the geometry has an area */
  boolean hasArea();
  /** returns the geometry area */
  double area();

  //////////  Relations  //////////
  /** Relates the geometry to the provided rectangle coordinates */
  Relation relate(double minX, double maxX, double minY, double maxY);
  /** Relates *this* geometry to *that* geometry */
  Relation relate(Geometry that);

  /** Shape Relation */
  enum Relation {
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

    public boolean intersects() {
      return this != DISJOINT;
    }

    public Relation transpose() {
      if (this == CONTAINS) {
        return WITHIN;
      } else if (this == WITHIN) {
        return CONTAINS;
      }
      return this;
    }
  }

  //////////  formatting  /////////
  /** converts geometry to WKT */
  String toWKT();
  /** converts geometry to WKB */
  default ByteArrayOutputStream toWKB() {
    return toWKB(null);
  }
  ByteArrayOutputStream toWKB(ByteArrayOutputStream reuse);

  /** Coordinate Reference System */
  enum CRSType {
    GEODETIC, GEOCENTRIC
  }
}
