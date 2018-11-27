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
package org.apache.lucene.spatial.geometry;

import java.util.function.Function;

import org.apache.lucene.geo.GeoUtils;
import org.apache.lucene.spatial.geometry.Geometry.Relation;
import org.apache.lucene.spatial.util.GeoRelationUtils;
import org.apache.lucene.util.SloppyMath;

import static org.apache.lucene.geo.GeoEncodingUtils.decodeLatitude;
import static org.apache.lucene.geo.GeoEncodingUtils.decodeLongitude;
import static org.apache.lucene.geo.GeoEncodingUtils.encodeLatitude;
import static org.apache.lucene.geo.GeoEncodingUtils.encodeLatitudeCeil;
import static org.apache.lucene.geo.GeoEncodingUtils.encodeLongitude;
import static org.apache.lucene.geo.GeoEncodingUtils.encodeLongitudeCeil;

/**
 * Used to speed up point-in-polygon and point-distance computations
 *
 * @lucene.experimental
 */
abstract class Predicate {
  static final int ARITY = 64;

  final int latShift, lonShift;
  final int latBase, lonBase;
  final int maxLatDelta, maxLonDelta;
  final byte[] relations;

  protected Predicate(Rectangle boundingBox, Function<Rectangle, Relation> boxToRelation) {
    final int minLat = encodeLatitudeCeil(boundingBox.bottom());
    final int maxLat = encodeLatitude(boundingBox.top());
    final int minLon = encodeLongitudeCeil(boundingBox.left());
    final int maxLon = encodeLongitude(boundingBox.right());

    int latShift = 1;
    int lonShift = 1;
    int latBase = 0;
    int lonBase = 0;
    int maxLatDelta = 0;
    int maxLonDelta = 0;
    byte[] relations;

    if (maxLat < minLat || (boundingBox.left() <= boundingBox.right() && maxLon < minLon)) {
      // the box cannot match any quantized point
      relations = new byte[0];
    } else {
      {
        long minLat2 = (long) minLat - Integer.MIN_VALUE;
        long maxLat2 = (long) maxLat - Integer.MIN_VALUE;
        latShift = computeShift(minLat2, maxLat2);
        latBase = (int) (minLat2 >>> latShift);
        maxLatDelta = (int) (maxLat2 >>> latShift) - latBase + 1;
        assert maxLatDelta > 0;
      }
      {
        long minLon2 = (long) minLon - Integer.MIN_VALUE;
        long maxLon2 = (long) maxLon - Integer.MIN_VALUE;
        if (boundingBox.left() > boundingBox.right()) {
          maxLon2 += 1L << 32; // wrap
        }
        lonShift = computeShift(minLon2, maxLon2);
        lonBase = (int) (minLon2 >>> lonShift);
        maxLonDelta = (int) (maxLon2 >>> lonShift) - lonBase + 1;
        assert maxLonDelta > 0;
      }

      relations = new byte[maxLatDelta * maxLonDelta];
      for (int i = 0; i < maxLatDelta; ++i) {
        for (int j = 0; j < maxLonDelta; ++j) {
          final int boxMinLat = ((latBase + i) << latShift) + Integer.MIN_VALUE;
          final int boxMinLon = ((lonBase + j) << lonShift) + Integer.MIN_VALUE;
          final int boxMaxLat = boxMinLat + (1 << latShift) - 1;
          final int boxMaxLon = boxMinLon + (1 << lonShift) - 1;

          relations[i * maxLonDelta + j] = (byte) boxToRelation.apply(new Rectangle(
              decodeLatitude(boxMinLat), decodeLatitude(boxMaxLat),
              decodeLongitude(boxMinLon), decodeLongitude(boxMaxLon))).ordinal();
        }
      }
    }
    this.latShift = latShift;
    this.lonShift = lonShift;
    this.latBase = latBase;
    this.lonBase = lonBase;
    this.maxLatDelta = maxLatDelta;
    this.maxLonDelta = maxLonDelta;
    this.relations = relations;
  }




  /** Compute the minimum shift value so that
   * {@code (b>>>shift)-(a>>>shift)} is less that {@code ARITY}. */
  private static int computeShift(long a, long b) {
    assert a <= b;
    // We enforce a shift of at least 1 so that when we work with unsigned ints
    // by doing (lat - MIN_VALUE), the result of the shift (lat - MIN_VALUE) >>> shift
    // can be used for comparisons without particular care: the sign bit has
    // been cleared so comparisons work the same for signed and unsigned ints
    for (int shift = 1; ; ++shift) {
      final long delta = (b >>> shift) - (a >>> shift);
      if (delta >= 0 && delta < ARITY) {
        return shift;
      }
    }
  }
}
