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

public class GeoShapeTermsEnum {
  private long term;       // long encoded quad term
  private int maxLevels;   // max depth to traverse (spatial resolution)

  public GeoShapeTermsEnum(long term, int maxLevels) {
    this.term = term;
    this.maxLevels = maxLevels;
  }

  /** returns the current level of the quad tree */
  private int getLevel() {
    return (int) ((term >>> 1) & 0x1FL);
  }

  /** returns the binary shift to access bits at provided level */
  private int getShiftForLevel(final int level) {
    return 64 - (level << 1);
  }

  /** returns true if at the last term for the given level (e.g., 11111111, for level 4) */
  public boolean isEnd(final int level) {
    int shift = getShiftForLevel(level);
    return term != 0x0L && ((((0x1L << (level << 1)) - 1) - (term >>> shift)) == 0x0L);
  }

  /** returns true if this is a leaf term */
  private boolean isLeaf() {
    return (term & 0x1L) == 0x1L;
  }

  /** returns true if this is the last quad for the provided shift/level */
  private boolean isLastQuad(final int shift) {
    return ((term >>> shift) & 0x3L) == 0x3L;
  }

  /** advances to the next sibling at the given shift/level */
  private long nextSibling(final int shift) {
    return term + (0x1L << shift);
  }

  /** advances the level bits */
  private long nextLevel() {
    return ((term >>> 1) + 0x1L) << 1;
  }

  /** returns true iff last term at maxLevels, or at the current level if descend is not requested */
  public boolean hasNext(boolean requestDescend) {
    // base case: can't go further
    final int level = getLevel();
    if ((requestDescend == false && isEnd(level)) || isEnd(maxLevels)) {
      return false;
    }
    return true;
  }

  /** returns the next cell using an iterative algorithm */
  public long nextCell(boolean requestDescend) {
    if (hasNext(requestDescend) == false) {
      throw new IllegalStateException("requested next cell without first calling hasNext");
    }
    final int level = getLevel();
    final int shift = getShiftForLevel(level);
    long newTerm;
    // if descend requested && we're not at the maxLevel
    if (level == 0 || (requestDescend == true && isLeaf() == false && level != maxLevels)) {
      // simple case: increment level bits (next level)
      newTerm = nextLevel();
    } else { // we're not descending or we can't descend
      // nextSibling advances to next quad up the stack (pops up)
      newTerm = nextSibling(shift);
      if (isLastQuad(shift) == true) {
        // adjust level for popping up (newTerm is already popped up)
        newTerm = ((newTerm >>> 1) - (Long.numberOfTrailingZeros(newTerm >>> shift) >>> 1)) << 1;
      }
    }
    term = newTerm;
    return term;
  }

  public static void main(String[] args) {
    GeoShapeTermsEnum te = new GeoShapeTermsEnum(0L, 5);

    int i=0;
    while (te.hasNext(true)) {
      System.out.println(i++ + " " + termToString(te.nextCell(true)));
    }
  }

  public static String termToString(final long term) {
    return String.format("%64s", Long.toBinaryString(term)).replace(' ', '0');
  }
}
