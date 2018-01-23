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

import java.util.Arrays;

import org.apache.lucene.geo.geometry.GeoShape.ConnectedComponent;
import org.apache.lucene.index.PointValues.Relation;

/**
 * Created by nknize on 2/27/17.
 */
public class MultiLine extends GeoShape implements ConnectedComponent {
  EdgeTree tree;
  Line[] lines;

  public MultiLine(Line... lines) {
    this.lines = lines.clone();
    // compute bounding box
    double minLat = Double.POSITIVE_INFINITY;
    double maxLat = Double.NEGATIVE_INFINITY;
    double minLon = Double.POSITIVE_INFINITY;
    double maxLon = Double.NEGATIVE_INFINITY;
    for (Line l : lines) {
      minLat = Math.min(l.minLat, minLat);
      maxLat = Math.max(l.maxLat, maxLat);
      minLon = Math.min(l.minLon, minLon);
      maxLon = Math.max(l.maxLon, maxLon);
    }
  }

  @Override
  public ShapeType type() {
    return ShapeType.MULTILINESTRING;
  }

  public EdgeTree createEdgeTree() {
    EdgeTree components[] = new EdgeTree[lines.length];
    for (int i = 0; i < components.length; i++) {
      Line line = lines[i];
      components[i] = new EdgeTree(line);
    }
    return EdgeTree.createTree(components, 0, components.length - 1, false);
  }

  public Relation relate(double minLat, double maxLat, double minLon, double maxLon) {
    if (tree == null) {
      tree = createEdgeTree();
    }
    return tree.relate(minLat, maxLat, minLon, maxLon);
  }

  public Relation relate(GeoShape shape) {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    MultiLine multiLine = (MultiLine) o;

    if (!tree.equals(multiLine.tree)) return false;
    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    return Arrays.equals(lines, multiLine.lines);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + tree.hashCode();
    result = 31 * result + Arrays.hashCode(lines);
    return result;
  }
}
