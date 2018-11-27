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
package org.apache.lucene.spatial.query;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.spatial.geometry.Geometry;
import org.apache.lucene.spatial.geometry.Geometry.Relation;
import org.apache.lucene.spatial.geometry.Rectangle;

/**
 * A predicate that compares a stored geometry to a supplied geometry. It's enum-like. For more
 * explanation of each predicate, consider looking at the source implementation
 * of {@link #evaluate(Geometry, Geometry)}. It's important
 * to be aware that Lucene-spatial makes no distinction of shape boundaries, unlike many standardized
 * definitions. Nor does it make dimensional distinctions (e.g. line vs polygon).
 * You can lookup a predicate by "Covers" or "Contains", for example, and you will get the
 * same underlying predicate implementation.
 *
 * @see <a href="http://en.wikipedia.org/wiki/DE-9IM">DE-9IM at Wikipedia, based on OGC specs</a>
 * @see <a href="http://edndoc.esri.com/arcsde/9.1/general_topics/understand_spatial_relations.htm">
 *   ESRIs docs on spatial relations</a>
 *
 * @lucene.experimental
 */
public enum SpatialOperation {

  // Geometry Operations
  BBOX_INTERSECTS("BBoxIntersects") {
    /** Bounding box of the *indexed* shape, then {@link #INTERSECTS}. */
    @Override
    public boolean evaluate(Geometry indexedShape, Geometry queryShape) {
      return indexedShape.getBoundingBox().relate(queryShape) == Relation.INTERSECTS;
    }
  },
  BBOX_WITHIN("BBoxWithin", "BBoxCoveredBy") {
    /** Bounding box of the *indexed* shape, then {@link #WITHIN}. */
    @Override
    public boolean evaluate(Geometry indexedShape, Geometry queryShape) {
      Rectangle bbox = indexedShape.getBoundingBox();
      return indexedShape.getBoundingBox().relate(queryShape) == Relation.WITHIN || bbox.equals(queryShape);
    }
  },
  CONTAINS("Contains", "Covers") {
    /** Meets the "Covers" OGC definition (boundary-neutral). */
    @Override
    public boolean evaluate(Geometry indexedShape, Geometry queryShape) {
      return indexedShape.relate(queryShape) == Relation.CONTAINS || indexedShape.equals(queryShape);
    }
  },
  INTERSECTS("Intersects") {
    /** Meets the "Intersects" OGC definition. */
    @Override
    public boolean evaluate(Geometry indexedShape, Geometry queryShape) {
      return indexedShape.relate(queryShape) == Relation.INTERSECTS;
    }
  },
  EQUALS("Equals") {
    /** Meets the "Equals" OGC definition. */
    @Override
    public boolean evaluate(Geometry indexedShape, Geometry queryShape) {
      return indexedShape.equals(queryShape);
    }
  },
  DISJOINT("Disjoint") {
    @Override
    public boolean evaluate(Geometry indexedShape, Geometry queryShape) {
      return indexedShape.relate(queryShape) != Relation.INTERSECTS;
    }
  },
  WITHIN("Within", "CoveredBy") {
    /** Meets the "CoveredBy" OGC definition (boundary-neutral). */
    @Override
    public boolean evaluate(Geometry indexedShape, Geometry queryShape) {
      return indexedShape.relate(queryShape) == Relation.WITHIN || indexedShape.equals(queryShape);
    }
  },
  OVERLAPS("Overlaps") {
    /** Almost meets the "Overlaps" OGC definition, but boundary-neutral (boundary==interior). */
    @Override
    public boolean evaluate(Geometry indexedShape, Geometry queryShape) {
      return indexedShape.relate(queryShape) == Relation.INTERSECTS;//not Contains or Within or Disjoint
    }
  };

  /** operation name used in query strings */
  private final String name;
  /** operation aliases used in query strings */
  private final List<String> aliases;
  /** utility map for retrieving operation by name */
  protected static final Map<String, SpatialOperation> REGISTRY = new HashMap<>();//has aliases

  static {
    for (SpatialOperation op : values()) {
      REGISTRY.put(op.name().toUpperCase(Locale.ROOT), op);
      op.registerAliases();
    }
  }

  /**
   * ctor for creating a new spatial relation operation.
   * @param name name of the operation. (not null)
   * @param aliases optional aliased names
   */
  SpatialOperation(String name, String... aliases) {
    this.name = name;
    this.aliases = Arrays.asList(aliases);
  }

  /** utility method for registering aliases to the map */
  private void registerAliases() {
    for (String alias : aliases) {
      REGISTRY.put(alias.toUpperCase(Locale.ROOT), this);
    }
  }

  /** retrieve operation by registered name or alias */
  public static SpatialOperation forName(String v ) {
    String name = v.toUpperCase(Locale.ROOT);
    if (REGISTRY.containsKey(name)) {
      return REGISTRY.get(name);
    }
    throw new IllegalArgumentException("SpatialOperation  [" + name + "] not found");
  }

  /**
   * Returns whether the relationship between indexedShape and queryShape is
   * satisfied by this operation.
   */
  public abstract boolean evaluate(Geometry indexedShape, Geometry queryShape);

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
