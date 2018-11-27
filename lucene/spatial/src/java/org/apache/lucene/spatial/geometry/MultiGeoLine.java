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

class MultiGeoLine extends GeoShape implements ConnectedComponent {
  protected GeoLine[] lines;

  public MultiGeoLine(GeoLine... lines) {
    this.lines = lines.clone();
  }

  @Override
  protected Rectangle computeBoundingBox() {
    // compute bounding box
    double minLat = Double.POSITIVE_INFINITY;
    double maxLat = Double.NEGATIVE_INFINITY;
    double minLon = Double.POSITIVE_INFINITY;
    double maxLon = Double.NEGATIVE_INFINITY;
    for (GeoLine l : lines) {
      minLat = Math.min(l.bottom(), minLat);
      maxLat = Math.max(l.top(), maxLat);
      minLon = Math.min(l.left(), minLon);
      maxLon = Math.max(l.right(), maxLon);
    }
    return new Rectangle(minLon, maxLon, minLat, maxLat);
  }

  @Override
  protected Point computeCenter() {
    return this.boundingBox.center();
  }

  public int length() {
    return lines.length;
  }

  public GeoLine get(int index) {
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
      GeoLine line = lines[i];
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
        sb.append(MultiPoint.coordinatesToWKT(lines[0].lons, lines[0].lats));
      }
      for (int i = 1; i < lines.length; ++i) {
        sb.append(WKTParser.COMMA);
        sb.append(MultiPoint.coordinatesToWKT(lines[i].lons, lines[i].lats));
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
      GeoLine line = lines[i];
      Line.lineToWKB(line.lons, line.lats, out, true);
    }
  }
}
