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
package org.apache.lucene.spatial.query;

import org.apache.lucene.spatial.SpatialContext;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.geometry.Geometry;
import org.apache.lucene.spatial.geometry.Point;
import org.apache.lucene.spatial.geometry.Rectangle;

/**
 * Principally holds the query {@link Geometry} and the {@link SpatialOperation}.
 * It's used as an argument to some methods on {@link SpatialStrategy}.
 *
 * @lucene.experimental
 */
public class SpatialArgs {

  public static final double DEFAULT_DISTERRPCT = 0.025d;

  protected SpatialOperation operation;
  protected Geometry shape;
  protected Double distErrPct;
  protected Double distErr;

  public SpatialArgs(SpatialOperation operation, Geometry shape) {
    if (operation == null || shape == null) {
      throw new NullPointerException("operation and shape are required");
    }
    this.operation = operation;
    this.shape = shape;
  }

  /**
   * Computes the distance given a shape and the {@code distErrPct}.  The
   * algorithm is the fraction of the distance from the center of the query
   * shape to its closest bounding box corner.
   *
   * @param shape Mandatory.
   * @param distErrPct 0 to 0.5
   * @return A distance (in degrees).
   */
  public static double calcDistanceFromErrPct(Geometry shape, double distErrPct, SpatialContext ctx) {
    if (distErrPct < 0 || distErrPct > 0.5) {
      throw new IllegalArgumentException("distErrPct " + distErrPct + " must be between [0 to 0.5]");
    }
    if (distErrPct == 0 || shape instanceof Point) {
      return 0;
    }
    Rectangle bbox = shape.getBoundingBox();
    //Compute the distance from the center to a corner.  Because the distance
    // to a bottom corner vs a top corner can vary in a geospatial scenario,
    // take the closest one (greater precision).
    Point ctr = bbox.getCenter();
    return calcDistanceFromErrPct(distErrPct, bbox.bottom(), bbox.top(), bbox.right(), ctr.y(), ctr.x());
  }

  protected static double calcDistanceFromErrPct(final double distErrPct, final double minLat, final double maxLat,
                                                 final double maxLon, final double cntrLat, final double cntrLon) {
    double y = (cntrLat >= 0 ? maxLat : minLat);
    double diagonalDist =  SpatialContext.calculateDistanceDegrees(cntrLat, cntrLon, y, maxLon);
    return diagonalDist * distErrPct;
  }

  /**
   * Gets the error distance that specifies how precise the query shape is. This
   * looks at {@link #getDistErr()}, {@link #getDistErrPct()}, and {@code
   * defaultDistErrPct}.
   * @param defaultDistErrPct 0 to 0.5
   * @return {@code >= 0}
   */
  public double resolveDistErr(SpatialContext ctx, double defaultDistErrPct) {
    if (distErr != null)
      return distErr;
    double distErrPct = (this.distErrPct != null ? this.distErrPct : defaultDistErrPct);
    return calcDistanceFromErrPct(shape, distErrPct, ctx);
  }

  /** Check if the arguments make sense -- throw an exception if not */
  public void validate() throws IllegalArgumentException {
    if (distErr != null && distErrPct != null)
      throw new IllegalArgumentException("Only distErr or distErrPct can be specified.");
  }

  @Override
  public String toString() {
    return SpatialArgsParser.writeSpatialArgs(this);
  }

  //------------------------------------------------
  // Getters & Setters
  //------------------------------------------------

  public SpatialOperation getOperation() {
    return operation;
  }

  public void setOperation(SpatialOperation operation) {
    this.operation = operation;
  }

  public Geometry getShape() {
    return shape;
  }

  public void setShape(Geometry shape) {
    this.shape = shape;
  }

  /**
   * A measure of acceptable error of the shape as a fraction.  This effectively
   * inflates the size of the shape but should not shrink it.
   *
   * @return 0 to 0.5
   * @see #calcDistanceFromErrPct(Geometry, double, SpatialContext)
   */
  public Double getDistErrPct() {
    return distErrPct;
  }

  public void setDistErrPct(Double distErrPct) {
    if (distErrPct != null)
      this.distErrPct = distErrPct;
  }

  /**
   * The acceptable error of the shape.  This effectively inflates the
   * size of the shape but should not shrink it.
   *
   * @return {@code >= 0}
   */
  public Double getDistErr() {
    return distErr;
  }

  public void setDistErr(Double distErr) {
    this.distErr = distErr;
  }



}

