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
package org.apache.lucene.spatial.bbox;

import java.io.IOException;

import org.apache.lucene.geo.geometry.GeoShape;
import org.apache.lucene.geo.geometry.Rectangle;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.spatial.ShapeValues;
import org.apache.lucene.spatial.ShapeValuesSource;

/**
 * A ShapeValuesSource returning a Rectangle from each document derived from four numeric fields
 *
 * @lucene.internal
 */
class BBoxValueSource extends ShapeValuesSource {

  private final BBoxStrategy strategy;

  public BBoxValueSource(BBoxStrategy strategy) {
    this.strategy = strategy;
  }

  @Override
  public String toString() {
    return "bboxShape(" + strategy.getFieldName() + ")";
  }

  @Override
  public ShapeValues getValues(LeafReaderContext readerContext) throws IOException {
    LeafReader reader = readerContext.reader();
    final NumericDocValues minX = DocValues.getNumeric(reader, strategy.field_minX);
    final NumericDocValues minY = DocValues.getNumeric(reader, strategy.field_minY);
    final NumericDocValues maxX = DocValues.getNumeric(reader, strategy.field_maxX);
    final NumericDocValues maxY = DocValues.getNumeric(reader, strategy.field_maxY);

    //reused
    final MutableRectangle rect = new MutableRectangle(0, 0, 0, 0);

    return new ShapeValues() {

      @Override
      public boolean advanceExact(int doc) throws IOException {
        return minX.advanceExact(doc) && minY.advanceExact(doc) && maxX.advanceExact(doc) && maxY.advanceExact(doc);
      }

      @Override
      public GeoShape value() throws IOException {
        double minXValue = Double.longBitsToDouble(minX.longValue());
        double minYValue = Double.longBitsToDouble(minY.longValue());
        double maxXValue = Double.longBitsToDouble(maxX.longValue());
        double maxYValue = Double.longBitsToDouble(maxY.longValue());
        rect.reset(minXValue, maxXValue, minYValue, maxYValue);
        return rect;
      }

    };
  }

  @Override
  public boolean isCacheable(LeafReaderContext ctx) {
    return DocValues.isCacheable(ctx,
        strategy.field_minX, strategy.field_minY, strategy.field_maxX, strategy.field_maxY);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BBoxValueSource that = (BBoxValueSource) o;

    if (!strategy.equals(that.strategy)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return strategy.hashCode();
  }

  class MutableRectangle extends Rectangle {
    double minX, maxX;
    double minY, maxY;

    MutableRectangle(double minX, double maxX, double minY, double maxY) {
      super(minY, maxY, minX, maxX);
      minX = minLon;
      maxX = maxLon;
      minY = minLat;
      maxY = maxLat;
    }

    public void reset(double minX, double maxX, double minY, double maxY) {
      minX = minLon;
      maxX = maxLon;
      minY = minLat;
      maxY = maxLat;
    }

    @Override
    public double minLat() {
      return minY;
    }

    @Override
    public double maxLat() {
      return maxY;
    }

    @Override
    public double minLon() {
      return minX;
    }

    @Override
    public double maxLon() {
      return maxX;
    }
  }
}
