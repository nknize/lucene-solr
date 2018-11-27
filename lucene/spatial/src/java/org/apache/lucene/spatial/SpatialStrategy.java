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
package org.apache.lucene.spatial;

import org.apache.lucene.document.Field;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Query;
import org.apache.lucene.geo.geometry.GeoShape;
import org.apache.lucene.geo.geometry.Point;
import org.apache.lucene.geo.geometry.Rectangle;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.spatial.util.ReciprocalDoubleValuesSource;

import static org.apache.lucene.spatial.SpatialContext.calculateDistance;

public abstract class SpatialStrategy {
  protected final SpatialContext ctx;
  protected final String fieldName;

  /** constructs a simple spatial strategy object used for indexing spatial fields */
  public SpatialStrategy(SpatialContext ctx, String fieldName) {
    if (ctx == null) {
      throw new IllegalArgumentException("ctx is required");
    }
    this.ctx = ctx;
    if (fieldName == null || fieldName.length() == 0) {
      throw new IllegalArgumentException("fieldName is required");
    }
    this.fieldName = fieldName;
  }

  public SpatialContext getSpatialContext() {
    return ctx;
  }

  /**
   * The name of the field or the prefix of them if there are multiple
   * fields needed internally.
   * @return Not null.
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * Returns the IndexableField(s) from the {@code shape} that are to be
   * added to the {@link org.apache.lucene.document.Document}.  These fields
   * are expected to be marked as indexed and not stored.
   * <p>
   * Note: If you want to <i>store</i> the shape as a string for retrieval in
   * search results, you could add it like this:
   * <pre>document.add(new StoredField(fieldName,ctx.toString(shape)));</pre>
   * The particular string representation used doesn't matter to the Strategy
   * since it doesn't use it.
   *
   * @return Not null nor will it have null elements.
   * @throws UnsupportedOperationException if given a shape incompatible with the strategy
   */
  public abstract Field[] createIndexableFields(GeoShape shape);

  /**
   * See {@link #makeDistanceValueSource(Point, double)} called with
   * a multiplier of 1.0 (i.e. units of degrees).
   */
  public DoubleValuesSource makeDistanceValueSource(Point queryPoint) {
    return makeDistanceValueSource(queryPoint, 1.0);
  }

  /**
   * Make a ValueSource returning the distance between the center of the
   * indexed shape and {@code queryPoint}.  If there are multiple indexed shapes
   * then the closest one is chosen. The result is multiplied by {@code multiplier}, which
   * conveniently is used to get the desired units.
   */
  public abstract DoubleValuesSource makeDistanceValueSource(Point queryPoint, double multiplier);

  /**
   * Returns a ValueSource with values ranging from 1 to 0, depending inversely
   * on the distance from {@link #makeDistanceValueSource(Point,double)}.
   * The formula is {@code c/(d + c)} where 'd' is the distance and 'c' is
   * one tenth the distance to the farthest edge from the center. Thus the
   * scores will be 1 for indexed points at the center of the query shape and as
   * low as ~0.1 at its furthest edges.
   */
  public final DoubleValuesSource makeRecipDistanceValueSource(GeoShape queryShape) {
    Rectangle bbox = queryShape.getBoundingBox();
    double diagonalDist = calculateDistance(bbox.minLat(), bbox.minLon(), bbox.maxLat(), bbox.maxLon());
    double distToEdge = diagonalDist * 0.5;
    float c = (float)distToEdge * 0.1f;//one tenth
    DoubleValuesSource distance = makeDistanceValueSource(queryShape.getCenter(), 1.0);
    return new ReciprocalDoubleValuesSource(c, distance);
  }

  /**
   * Make a Query based principally on {@link SpatialOperation}
   * and {@link GeoShape} from the supplied {@code args}.  It should be constant scoring of 1.
   *
   * @throws UnsupportedOperationException If the strategy does not support the shape in {@code args}
   * BaseSpatialOperation} in {@code args}.
   */
  public abstract Query makeQuery(SpatialArgs args);

  @Override
  public String toString() {
    return super.toString() + " ctx=" + ctx;
  }
}
