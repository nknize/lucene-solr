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

import java.util.Arrays;

import org.apache.lucene.spatial.geometry.Shape.ConnectedComponent;

class GeoLine extends MultiGeoPoint implements ConnectedComponent {
  public GeoLine(double[] lats, double[] lons) {
    super(lats, lons);
  }

  public int numPoints() {
    return lats.length;
  }

  @Override
  public EdgeTree createEdgeTree() {
    return new EdgeTree(this);
  }

  @Override
  public Relation relate(Geometry that) {
    throw new UnsupportedOperationException("GeoLine does not yet support relation to other geometry");
  }

  @Override
  public Relation relate(double minX, double maxX, double minY, double maxY) {
    if (edgeTree == null) {
      edgeTree = createEdgeTree();
    }
    return edgeTree.relate(minX, maxX, minY, maxY);
  }

  @Override
  public boolean equals(Object other) {
    if (super.equals(other) == false) return false;
    GeoLine o = getClass().cast(other);
    return Arrays.equals(lats, o.lats) && Arrays.equals(lons, o.lons);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Arrays.hashCode(lats);
    result = 31 * result + Arrays.hashCode(lons);
    return result;
  }
}
