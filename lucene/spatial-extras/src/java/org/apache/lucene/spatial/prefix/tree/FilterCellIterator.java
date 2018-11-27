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
package org.apache.lucene.spatial.prefix.tree;

import org.apache.lucene.spatial.geometry.Geometry;
import org.apache.lucene.spatial.geometry.Geometry.Relation;
import org.apache.lucene.spatial.geometry.Rectangle;

import java.util.Iterator;

/**
 * A filtering iterator of Cells. Those not matching the provided shape (disjoint) are
 * skipped. If {@code shapeFilter} is null then all cells are returned.
 *
 * @lucene.internal
 */
class FilterCellIterator extends CellIterator {
  final Iterator<Cell> baseIter;
  final Geometry shapeFilter;

  FilterCellIterator(Iterator<Cell> baseIter, Geometry shapeFilter) {
    this.baseIter = baseIter;
    this.shapeFilter = shapeFilter;
  }

  @Override
  public boolean hasNext() {
    thisCell = null;
    if (nextCell != null)//calling hasNext twice in a row
      return true;
    while (baseIter.hasNext()) {
      nextCell = baseIter.next();
      if (shapeFilter == null) {
        return true;
      } else {
        Rectangle r = nextCell.getRectangle();
        Relation rel = shapeFilter.relate(r.left(), r.right(), r.bottom(), r.top());
        if (rel.intersects() == true) {
          nextCell.setShapeRel(rel.transpose());
          if (rel == Relation.CONTAINS)
            nextCell.setLeaf();
          return true;
        }
      }
    }
    return false;
  }

}
