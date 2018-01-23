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

/**
 * Created by nknize on 2/27/17.
 */
public class MultiPolygon extends MultiLine {
  Predicate.PolygonPredicate predicate;

  public MultiPolygon(Polygon... polygons) {
    super(polygons);
  }

  @Override
  public ShapeType type() {
    return ShapeType.MULTIPOLYGON;
  }

  @Override
  public EdgeTree createEdgeTree() {
    predicate = Predicate.PolygonPredicate.create((Polygon[])(this.lines));
    return predicate.tree;
  }

  public boolean pointInside(int encodedLat, int encodedLon) {
    return predicate.test(encodedLat, encodedLon);
  }

//  private EdgeTree createEdgeTree(Polygon... polygons) {
//    EdgeTree components[] = new EdgeTree[polygons.length];
//    for (int i = 0; i < components.length; i++) {
//      Polygon gon = polygons[i];
//      Polygon gonHoles[] = gon.getHoles();
//      EdgeTree holes = null;
//      if (gonHoles.length > 0) {
//        holes = createEdgeTree(gonHoles);
//      }
//      components[i] = new EdgeTree(gon, holes);
//    }
//    return EdgeTree.createTree(components, 0, components.length - 1, false);
//  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    MultiPolygon that = (MultiPolygon) o;
    return predicate.equals(that.predicate);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + predicate.hashCode();
    return result;
  }
}
