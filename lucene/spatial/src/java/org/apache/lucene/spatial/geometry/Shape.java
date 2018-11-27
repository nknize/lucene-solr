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
import java.io.IOException;

import org.apache.lucene.geo.parsers.WKBParser;
import org.apache.lucene.geo.parsers.WKTParser;
import org.apache.lucene.store.OutputStreamDataOutput;

public abstract class Shape implements Geometry {
  protected final Rectangle boundingBox;
  protected double area;
  protected EdgeTree edgeTree = null;
  protected final Point center;

  protected Shape() {
    this.boundingBox = computeBoundingBox();
    this.center = computeCenter();
    this.area = Double.NaN;
  }

  /** abstract method to compute the bounding box for the implemented shape */
  protected abstract Rectangle computeBoundingBox();
  /** abstract method to compute the area of the implemented shape */
  protected abstract double computeArea();
  /** abstract method to create the WKT for the implemented shape */
  protected abstract StringBuilder contentToWKT();
  /** abstract method to create the body of the WKB for the implemented shape */
  protected abstract void appendWKBContent(OutputStreamDataOutput out) throws IOException;

  /** returns bounding box */
  public Rectangle getBoundingBox() {
    return boundingBox;
  }

  protected Point computeCenter() {
    return boundingBox.center;
  }

  @Override
  public double left() {
    return boundingBox.left();
  }

  @Override
  public double right() {
    return boundingBox.right();
  }

  @Override
  public double bottom() {
    return boundingBox.bottom();
  }

  @Override
  public double top() {
    return boundingBox.top();
  }

  @Override
  public Point center() {
    return boundingBox.center();
  }

  @Override
  public double area() {
    if (hasArea()) {
      if (Double.isNaN(area)) {
        area = computeArea();
      }
      return area;
    }
    throw new UnsupportedOperationException(type() + " does not have an area");
  }

  @Override
  public String toWKT() {
    StringBuilder sb = new StringBuilder();
    sb.append(type().wktName());
    sb.append(WKTParser.SPACE);
    sb.append(contentToWKT());
    return sb.toString();
  }


  @Override
  public ByteArrayOutputStream toWKB(ByteArrayOutputStream reuse) {
    if (reuse == null) {
      reuse = new ByteArrayOutputStream();
    }
    try (OutputStreamDataOutput out = new OutputStreamDataOutput(reuse)) {
      out.writeVInt(WKBParser.ByteOrder.XDR.ordinal()); // byteOrder
      out.writeVInt(type().wkbOrdinal());     // shapeType ordinal
      appendWKBContent(out);
    } catch (IOException e) {
      throw new RuntimeException(e);  // not possible
    }
    return reuse;
  }

  /** interface for shapes that are connected by their vertices */
  interface ConnectedComponent {
    EdgeTree createEdgeTree();
  }
}
