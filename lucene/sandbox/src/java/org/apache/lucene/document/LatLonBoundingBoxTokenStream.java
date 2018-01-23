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
package org.apache.lucene.document;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;

final class LatLonBoundingBoxTokenStream extends TokenStream {
  private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
  private boolean isInit = false;

  public LatLonBoundingBoxTokenStream() {
    super(new LatLonBBoxAttributeFactory(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY));
  }

  public LatLonBoundingBoxTokenStream setBoundingBox(final BytesRef bytes) {
    isInit = true;
    return this;
  }

  @Override
  public void reset() {
    if (isInit == false) {
      throw new IllegalStateException("call setGeoCode() before usage");
    }
  }

  @Override
  public boolean incrementToken() {
    // this will only clear all other attributes in this TokenStream
    clearAttributes();
    return true;
  }

  public interface LatLonBoundingBoxTermAttribute extends Attribute {
    //int positionInc
    void init();

  }
//
//  private static final class LatLonBoundingBoxAttribute extends AttributeImpl implements TermToBytesRefAttribute {
//    private BytesRefBuilder bytes = new BytesRefBuilder();
//
//    public LatLonBoundingBoxAttribute() {
//
//    }
//  }

  // just a wrapper to prevent adding CTA
  private static final class LatLonBBoxAttributeFactory extends AttributeFactory {
    private final AttributeFactory delegate;

    LatLonBBoxAttributeFactory(AttributeFactory delegate) {
      this.delegate = delegate;
    }

    @Override
    public AttributeImpl createAttributeInstance(Class<? extends Attribute> attClass) {
      if (CharTermAttribute.class.isAssignableFrom(attClass)) {
        throw new IllegalArgumentException("GeoPointTokenStream does not support CharTermAttribute.");
      }
      return delegate.createAttributeInstance(attClass);
    }
  }

//  private static final class LatLonBoundingBox
}
