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

import org.apache.lucene.spatial.geometry.parsers.WKTParser;
import org.apache.lucene.spatial.geometry.Predicate.PolygonPredicate;
import org.apache.lucene.store.OutputStreamDataOutput;

public class MultiGeoPolygon extends MultiGeoLine {
  PolygonPredicate predicate;

  public MultiGeoPolygon(GeoPolygon... polygons) {
    super(polygons);
  }

  @Override
  public ShapeType type() {
    return ShapeType.MULTIPOLYGON;
  }

  @Override
  public int length() {
    return lines.length;
  }

  @Override
  public GeoPolygon get(int index) {
    checkVertexIndex(index);
    return (GeoPolygon)(lines[index]);
  }

  @Override
  public EdgeTree createEdgeTree() {
    GeoPolygon[] polygons = (GeoPolygon[])this.lines;
    this.edgeTree = GeoPolygon.createEdgeTree(polygons);
    predicate = Predicate.PolygonPredicate.create(this.boundingBox, edgeTree);
    return predicate.tree;
  }

  public boolean pointInside(int encodedLat, int encodedLon) {
    return predicate.test(encodedLat, encodedLon);
  }

  @Override
  public boolean hasArea() {
    return true;
  }

  @Override
  public double computeArea() {
    assertEdgeTree();
    return this.edgeTree.getArea();
  }

  protected void assertEdgeTree() {
    if (this.edgeTree == null) {
      final GeoPolygon[] polygons = (GeoPolygon[])this.lines;
      edgeTree = GeoPolygon.createEdgeTree(polygons);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    MultiGeoPolygon that = (MultiGeoPolygon) o;
    return predicate.equals(that.predicate);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + predicate.hashCode();
    return result;
  }

  @Override
  protected StringBuilder contentToWKT() {
    final StringBuilder sb = new StringBuilder();
    GeoPolygon[] polygons = (GeoPolygon[]) lines;
    if (polygons.length == 0) {
      sb.append(WKTParser.EMPTY);
    } else {
      sb.append(WKTParser.LPAREN);
      if (polygons.length > 0) {
        sb.append(GeoPolygon.polygonToWKT(polygons[0]));
      }
      for (int i = 1; i < polygons.length; ++i) {
        sb.append(WKTParser.COMMA);
        sb.append(GeoPolygon.polygonToWKT(polygons[i]));
      }
      sb.append(WKTParser.RPAREN);
    }
    return sb;
  }

  @Override
  protected void appendWKBContent(OutputStreamDataOutput out) throws IOException {
    int numPolys = length();
    out.writeVInt(numPolys);
    for (int i = 0; i < numPolys; ++i) {
      GeoPolygon polygon = this.get(i);
      GeoPolygon.polygonToWKB(polygon, out, true);
    }
  }



}
