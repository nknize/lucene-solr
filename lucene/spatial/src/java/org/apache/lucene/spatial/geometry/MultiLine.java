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

import org.apache.lucene.spatial.geometry.Shape.ConnectedComponent;
import org.apache.lucene.spatial.geometry.parsers.WKTParser;
import org.apache.lucene.store.OutputStreamDataOutput;

class MultiLine extends Shape implements ConnectedComponent {
  protected Line[] lines;

  public MultiLine(Line... lines) {
    this.lines = lines.clone();
  }

  @Override
  protected Rectangle computeBoundingBox() {
    // compute bounding box
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    for (Line l : lines) {
      minX = Math.min(l.left(), minX);
      maxX = Math.max(l.right(), maxX);
      minY = Math.min(l.bottom(), minY);
      maxY = Math.max(l.top(), maxY);
    }
    return new Rectangle(minX, maxX, minY, maxY);
  }

  @Override
  protected Point computeCenter() {
    return boundingBox.center;
  }

  public int length() {
    return lines.length;
  }

  public Line get(int index) {
    checkVertexIndex(index);
    return lines[index];
  }

  protected void checkVertexIndex(final int i) {
    if (i >= lines.length) {
      throw new IllegalArgumentException("Index " + i + " is outside the bounds of the " + lines.length + " shapes");
    }
  }

  @Override
  public ShapeType type() {
    return ShapeType.MULTILINESTRING;
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
  public EdgeTree createEdgeTree() {
    EdgeTree components[] = new EdgeTree[lines.length];
    for (int i = 0; i < components.length; i++) {
      Line line = lines[i];
      components[i] = new EdgeTree(line);
    }
    return EdgeTree.createTree(components, 0, components.length - 1, false);
  }

  @Override
  public Relation relate(double minLat, double maxLat, double minLon, double maxLon) {
    if (edgeTree == null) {
      edgeTree = createEdgeTree();
    }
    return edgeTree.relate(minLat, maxLat, minLon, maxLon).transpose();
  }

  public Relation relate(Geometry shape) {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    MultiLine multiLine = (MultiLine) o;

    if (!edgeTree.equals(multiLine.edgeTree)) return false;
    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    return Arrays.equals(lines, multiLine.lines);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + edgeTree.hashCode();
    result = 31 * result + Arrays.hashCode(lines);
    return result;
  }

  @Override
  protected StringBuilder contentToWKT() {
    final StringBuilder sb = new StringBuilder();
    if (lines.length == 0) {
      sb.append(WKTParser.EMPTY);
    } else {
      sb.append(WKTParser.LPAREN);
      if (lines.length > 0) {
        sb.append(MultiPoint.coordinatesToWKT(lines[0].xVals, lines[0].yVals));
      }
      for (int i = 1; i < lines.length; ++i) {
        sb.append(WKTParser.COMMA);
        sb.append(MultiPoint.coordinatesToWKT(lines[i].xVals, lines[i].yVals));
      }
      sb.append(WKTParser.RPAREN);
    }
    return sb;
  }

  @Override
  protected void appendWKBContent(OutputStreamDataOutput out) throws IOException {
    int numLines = length();
    out.writeVInt(numLines);
    for (int i = 0; i < numLines; ++i) {
      Line line = lines[i];
      Line.lineToWKB(line.xVals, line.yVals, out, true);
    }
  }
}
