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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.geo.geometry.GeoShape;
import org.apache.lucene.geo.geometry.Point;
import org.apache.lucene.geo.geometry.Rectangle;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.spatial.SpatialContext;
import org.apache.lucene.spatial.SpatialTestCase;
import org.apache.lucene.spatial.prefix.TermQueryPrefixTreeStrategy;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SpatialPrefixTreeTest extends SpatialTestCase {

  //TODO plug in others and test them
  private SpatialContext ctx;
  private SpatialPrefixTree trie;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    ctx = SpatialContext.GEO;
  }

  @Test
  public void testCellTraverse() {
    trie = new GeohashPrefixTree(ctx,4);

    Cell prevC = null;
    Cell c = trie.getWorldCell();
    assertEquals(0, c.getLevel());
    assertEquals(ctx.getWorldBounds(), c.getShape());
    while (c.getLevel() < trie.getMaxLevels()) {
      prevC = c;
      List<Cell> subCells = new ArrayList<>();
      CellIterator subCellsIter = c.getNextLevelCells(null);
      while (subCellsIter.hasNext()) {
        subCells.add(subCellsIter.next());
      }
      c = subCells.get(random().nextInt(subCells.size()-1));

      assertEquals(prevC.getLevel()+1,c.getLevel());
      Rectangle prevNShape = (Rectangle) prevC.getShape();
      GeoShape s = c.getShape();
      Rectangle sbox = s.getBoundingBox();
      assertTrue(prevNShape.getWidth() > sbox.getWidth());
      assertTrue(prevNShape.getHeight() > sbox.getHeight());
    }
  }
  /**
   * A PrefixTree pruning optimization gone bad, applicable when optimize=true.
   * See <a href="https://issues.apache.org/jira/browse/LUCENE-4770">LUCENE-4770</a>.
   */
  @Test
  public void testBadPrefixTreePrune() throws Exception {

    trie = new QuadPrefixTree(ctx, 12);
    TermQueryPrefixTreeStrategy strategy = new TermQueryPrefixTreeStrategy(trie, "geo");
    Document doc = new Document();
    doc.add(new TextField("id", "1", Store.YES));

    GeoShape area = new Rectangle(48.54, 48.56, -122.82, -122.78);

    Field[] fields = strategy.createIndexableFields(area, 0.025);
    for (Field field : fields) {
      doc.add(field);
    }
    addDocument(doc);

    Point upperleft = new Point(48.54, -122.88);
    Point lowerright = new Point(48.62, -122.82);

    Query query = strategy.makeQuery(new SpatialArgs(SpatialOperation.INTERSECTS,
        new Rectangle(lowerright.lat(), lowerright.lon(), upperleft.lat(), upperleft.lon())));

    commit();

    TopDocs search = indexSearcher.search(query, 10);
    ScoreDoc[] scoreDocs = search.scoreDocs;
    for (ScoreDoc scoreDoc : scoreDocs) {
      System.out.println(indexSearcher.doc(scoreDoc.doc));
    }

    assertEquals(1, search.totalHits);
  }

}