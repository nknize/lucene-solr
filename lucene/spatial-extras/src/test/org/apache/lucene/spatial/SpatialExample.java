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
package org.apache.lucene.spatial;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.geo.GeoUtils;
import org.apache.lucene.geo.geometry.Circle;
import org.apache.lucene.geo.geometry.GeoShape;
import org.apache.lucene.geo.geometry.Point;
import org.apache.lucene.geo.parsers.WKTParser;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialArgsParser;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.LuceneTestCase;

/**
 * This class serves as example code to show how to use the Lucene spatial
 * module.
 */
public class SpatialExample extends LuceneTestCase {

  //Note: Test invoked via TestTestFramework.spatialExample()

  public static void main(String[] args) throws Exception {
    new SpatialExample().test();
  }

  public void test() throws Exception {
    init();
    indexPoints();
    search();
  }

  /**
   * The Spatial4j {@link SpatialContext} is a sort of global-ish singleton
   * needed by Lucene spatial.  It's a facade to the rest of Spatial4j, acting
   * as a factory for {@link GeoShape}s and provides access to reading and writing
   * them from Strings.
   */
  private SpatialContext ctx;//"ctx" is the conventional variable name

  /**
   * The Lucene spatial {@link SpatialStrategy} encapsulates an approach to
   * indexing and searching shapes, and providing distance values for them.
   * It's a simple API to unify different approaches. You might use more than
   * one strategy for a shape as each strategy has its strengths and weaknesses.
   * <p />
   * Note that these are initialized with a field name.
   */
  private SpatialStrategy strategy;

  private Directory directory;

  protected void init() {
    //Typical geospatial context
    //  These can also be constructed from SpatialContextFactory
    this.ctx = SpatialContext.GEO;

    int maxLevels = 11;//results in sub-meter precision for geohash
    //TODO demo lookup by detail distance
    //  This can also be constructed from SpatialPrefixTreeFactory
    SpatialPrefixTree grid = new GeohashPrefixTree(ctx, maxLevels);

    this.strategy = new RecursivePrefixTreeStrategy(grid, "myGeoField");

    this.directory = new RAMDirectory();
  }

  private void indexPoints() throws Exception {
    IndexWriterConfig iwConfig = new IndexWriterConfig(null);
    IndexWriter indexWriter = new IndexWriter(directory, iwConfig);

    //Spatial4j is x-y order for arguments
    indexWriter.addDocument(newSampleDocument(
        2, new Point(33.77, -80.93)));

    //Spatial4j has a WKT parser which is also "x y" order
    indexWriter.addDocument(newSampleDocument(
        4, WKTParser.parse("POINT(60.9289094 -50.7693246)")));

    indexWriter.addDocument(newSampleDocument(
        20, new Point(0.1,0.1), new Point(0, 0)));

    indexWriter.close();
  }

  private Document newSampleDocument(int id, GeoShape... shapes) {
    Document doc = new Document();
    doc.add(new StoredField("id", id));
    doc.add(new NumericDocValuesField("id", id));
    //Potentially more than one shape in this field is supported by some
    // strategies; see the javadocs of the SpatialStrategy impl to see.
    for (GeoShape shape : shapes) {
      for (Field f : strategy.createIndexableFields(shape)) {
        doc.add(f);
      }
      //store it too; the format is up to you
      //  (assume point in this example)
      Point pt = (Point) shape;
      doc.add(new StoredField(strategy.getFieldName(), pt.lon()+" "+pt.lat()));
    }

    return doc;
  }

  private void search() throws Exception {
    IndexReader indexReader = DirectoryReader.open(directory);
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);
    Sort idSort = new Sort(new SortField("id", SortField.Type.INT));

    //--Filter by circle (<= distance from a point)
    {
      //Search with circle
      //note: SpatialArgs can be parsed from a string
      SpatialArgs args = new SpatialArgs(SpatialOperation.INTERSECTS,
          new Circle(-80.0, 33.0, GeoUtils.distanceToDegrees(200, GeoUtils.EARTH_MEAN_RADIUS_METERS / 1000d)));
      Query query = strategy.makeQuery(args);
      TopDocs docs = indexSearcher.search(query, 10, idSort);
      assertDocMatchedIds(indexSearcher, docs, 2);
      //Now, lets get the distance for the 1st doc via computing from stored point value:
      // (this computation is usually not redundant)
      Document doc1 = indexSearcher.doc(docs.scoreDocs[0].doc);
      String doc1Str = doc1.getField(strategy.getFieldName()).stringValue();
      //assume doc1Str is "x y" as written in newSampleDocument()
      int spaceIdx = doc1Str.indexOf(' ');
      double x = Double.parseDouble(doc1Str.substring(0, spaceIdx));
      double y = Double.parseDouble(doc1Str.substring(spaceIdx+1));
      Point cntr = args.getShape().getCenter();
      double doc1DistDEG =  SpatialContext.calculateDistance(cntr.lat(), cntr.lon(), y, x);
      assertEquals(121.6d, GeoUtils.degreesToDistance(doc1DistDEG, GeoUtils.EARTH_MEAN_RADIUS_METERS / 1000d), 0.1);
      //or more simply:
      assertEquals(121.6d, doc1DistDEG * GeoUtils.DEG_TO_METERS / 1000d, 0.1);
    }
    //--Match all, order by distance ascending
    {
      Point pt = new Point(-50, 60);
      DoubleValuesSource valueSource = strategy.makeDistanceValueSource(pt, GeoUtils.DEG_TO_METERS / 1000d);//the distance (in km)
      Sort distSort = new Sort(valueSource.getSortField(false)).rewrite(indexSearcher);//false=asc dist
      TopDocs docs = indexSearcher.search(new MatchAllDocsQuery(), 10, distSort);
      assertDocMatchedIds(indexSearcher, docs, 4, 20, 2);
      //To get the distance, we could compute from stored values like earlier.
      // However in this example we sorted on it, and the distance will get
      // computed redundantly.  If the distance is only needed for the top-X
      // search results then that's not a big deal. Alternatively, try wrapping
      // the ValueSource with CachingDoubleValueSource then retrieve the value
      // from the ValueSource now. See LUCENE-4541 for an example.
    }
    //demo arg parsing
    {
      SpatialArgs args = new SpatialArgs(SpatialOperation.INTERSECTS,
          new Circle(-80.0, 33.0, 1));
      SpatialArgs args2 = new SpatialArgsParser().parse("Intersects(BUFFER(POINT(-80 33),1))", ctx);
      assertEquals(args.toString(),args2.toString());
    }

    indexReader.close();
  }

  private void assertDocMatchedIds(IndexSearcher indexSearcher, TopDocs docs, int... ids) throws IOException {
    int[] gotIds = new int[Math.toIntExact(docs.totalHits)];
    for (int i = 0; i < gotIds.length; i++) {
      gotIds[i] = indexSearcher.doc(docs.scoreDocs[i].doc).getField("id").numericValue().intValue();
    }
    assertArrayEquals(ids,gotIds);
  }

}
