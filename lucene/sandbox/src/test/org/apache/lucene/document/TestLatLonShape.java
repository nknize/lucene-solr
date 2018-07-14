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
package org.apache.lucene.document;

import com.carrotsearch.randomizedtesting.generators.RandomNumbers;
import org.apache.lucene.geo.GeoTestUtil;
import org.apache.lucene.geo.Polygon;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestUtil;

/** Test case for indexing polygons and querying by bounding box */
public class TestLatLonShape extends LuceneTestCase {
  protected static String FIELDNAME = "field";
  protected void addPolygonsToDoc(String field, Document doc, Polygon polygon) {
    Field[] fields = LatLonShape.createIndexableFields(field, polygon);
    for (Field f : fields) {
      doc.add(f);
    }
  }

  protected Query newRectQuery(String field, double minLat, double maxLat, double minLon, double maxLon) {
    return LatLonShape.newBoxQuery(field, minLat, maxLat, minLon, maxLon);
  }

  public void testRandomPolygons() throws Exception {
    long avgIdxTime = 0;
    int numVertices;
    int numPolys = RandomNumbers.randomIntBetween(random(), 50, 100);

    Directory dir = newDirectory();
    RandomIndexWriter writer = new RandomIndexWriter(random(), dir);
    long start, end;

    Polygon polygon;
    Document document;
    System.out.println("generating " + numPolys + " polygons");
    for (int i = 0; i < numPolys;) {
      document = new Document();
      numVertices = TestUtil.nextInt(random(), 200000, 500000);
      polygon = GeoTestUtil.createRegularPolygon(0, 0, atLeast(1000000), numVertices);
      System.out.println("adding polygon " + i);
      start = System.currentTimeMillis();
      addPolygonsToDoc(FIELDNAME, document, polygon);
      writer.addDocument(document);
      end = System.currentTimeMillis();
      avgIdxTime += ((end - start) - avgIdxTime) / ++i;
    }
    System.out.println("avg index time: " + avgIdxTime);

    // search within 50km and verify we found our doc
    IndexReader reader = writer.getReader();
    IndexSearcher searcher = newSearcher(reader);
    start = System.currentTimeMillis();
    assertEquals(0, searcher.count(newRectQuery("field", -89.9, -89.8, -179.9, -179.8d)));
    end = System.currentTimeMillis();

    System.out.println("search: " + (end - start));

    reader.close();
    writer.close();
    dir.close();
  }

  /** test we can search for a point */
  public void testBasicIntersects() throws Exception {
    Directory dir = newDirectory();
    RandomIndexWriter writer = new RandomIndexWriter(random(), dir);

    int numVertices = TestUtil.nextInt(random(), 200000, 500000);

    // add a random polygon without a hole
    Polygon p = GeoTestUtil.createRegularPolygon(0, 90, atLeast(1000000), numVertices);
    Document document = new Document();
    addPolygonsToDoc(FIELDNAME, document, p);
    writer.addDocument(document);

    // add a random polygon with a hole
    Polygon inner = new Polygon(new double[] {-1d, -1d, 1d, 1d, -1d},
        new double[] {-91d, -89d, -89d, -91.0, -91.0});
    Polygon outer = GeoTestUtil.createRegularPolygon(0, -90, atLeast(1000000), numVertices);

    document = new Document();
    addPolygonsToDoc(FIELDNAME, document, new Polygon(outer.getPolyLats(), outer.getPolyLons(), inner));
    writer.addDocument(document);

    ////// search /////
    // search an intersecting bbox
    IndexReader reader = writer.getReader();
    IndexSearcher searcher = newSearcher(reader);
    Query q = newRectQuery(FIELDNAME, -1d, 1d, p.minLon, p.maxLon);
    assertEquals(1, searcher.count(q));

    // search a disjoint bbox
    q = newRectQuery(FIELDNAME, p.minLat-1d, p.minLat+1, p.minLon-1d, p.minLon+1d);
    assertEquals(0, searcher.count(q));

    // search a bbox in the hole
    q = newRectQuery(FIELDNAME, inner.minLat + 1e-6, inner.maxLat - 1e-6, inner.minLon + 1e-6, inner.maxLon - 1e-6);
    assertEquals(0, searcher.count(q));

    reader.close();
    writer.close();
    dir.close();
  }
}
