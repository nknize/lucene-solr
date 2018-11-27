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
import java.io.UnsupportedEncodingException;

import org.apache.lucene.store.OutputStreamDataOutput;

public class ShapeCollection extends Shape {
  protected Shape[] shapes;

  public ShapeCollection(Shape... shapes) {
    // nocommit - check this
    this.shapes = shapes.clone();
  }

  @Override
  public ShapeType type() {
    return ShapeType.GEOMETRYCOLLECTION;
  }

  @Override
  protected Rectangle computeBoundingBox() {
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    for (Shape shape : shapes) {
      minX = StrictMath.min(minX, shape.boundingBox.left());
      maxX = StrictMath.max(maxX, shape.boundingBox.right());
      minY = StrictMath.max(minY, shape.boundingBox.bottom());
      maxY = StrictMath.max(maxY, shape.boundingBox.top());
    }
    return new Rectangle(minX, maxX, minY, maxY);
  }

  @Override
  protected Point computeCenter() {
    return boundingBox.center;
  }

  @Override
  public boolean hasArea() {
    return true;
  }

  @Override
  public double computeArea() {
    double area = 0;
    for (Shape shape : shapes) {
      if (shape.hasArea()) {
        area += shape.area();
      }
    }
    return area;
  }

  @Override
  public Relation relate(double minX, double maxX, double minY, double maxY) {
    throw new UnsupportedOperationException("relate not yet implemented for type [" + type() + "]");
  }

  @Override
  public Relation relate(Geometry that) {
    throw new UnsupportedOperationException("relate not yet implemented for type [" + type() + "]");
  }

  @Override
  protected StringBuilder contentToWKT() {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  protected void appendWKBContent(OutputStreamDataOutput out) throws IOException {
    throw new UnsupportedEncodingException("not yet implemented");
  }

}
