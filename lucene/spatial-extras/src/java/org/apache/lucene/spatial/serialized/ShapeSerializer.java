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
package org.apache.lucene.spatial.serialized;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.lucene.spatial.geometry.GeoCircle;
import org.apache.lucene.spatial.geometry.Geometry;
import org.apache.lucene.spatial.geometry.Point;
import org.apache.lucene.spatial.geometry.Rectangle;
import org.apache.lucene.spatial.geometry.ShapeType;
import org.locationtech.spatial4j.shape.Shape;

/**
 * Utility class to serialize from either {@link Shape} or
 * {@link Geometry} types.
 *
 * Used for backwards compatibility to deprecate and remove the
 * dependency of {@link Shape} types
 */
public class ShapeSerializer {

  enum LegacyShapeType {
    NULL, POINT, RECTANGLE, CIRCLE, SHAPE_COLLECTION;

    public byte ordinalByte() {
      return (byte)ordinal();
    }
  }

  /** writes WKB formatting */
  public static void writeFromGeoShape(Geometry shape, ByteArrayOutputStream out) {
    shape.toWKB(out);
  }

  public static Geometry readFromShape(DataInput in) throws IOException {
    byte type = in.readByte();
    if (type == LegacyShapeType.POINT.ordinalByte()) {
      return readPoint(in);
    } else if (type == LegacyShapeType.RECTANGLE.ordinalByte()) {
      return readRectangle(in);
    } else if (type == LegacyShapeType.CIRCLE.ordinalByte()) {
      return readCircle(in);
    }
    throw new IllegalArgumentException("unsupported shape type for unmarshalling");
  }

  /** writes S4J proprietary formatting */
  public static void writeFromShape(Geometry shape, ByteArrayOutputStream baos) throws IOException {
    DataOutputStream out = new DataOutputStream(baos);
    ShapeType type = shape.type();
    switch (type) {
      case POINT:
        writePoint((Point)shape, out);
        break;
      case ENVELOPE:
        writeRectangle((Rectangle) shape, out);
        break;
      case CIRCLE:
        writeCircle((GeoCircle) shape, out);
        break;
      default:
        throw new IllegalArgumentException("shape type: " + type + " not supported for marshalling");
    }
  }

  public static Point readPoint(DataInput in) throws IOException {
    return new Point(in.readDouble(), in.readDouble());
  }

  private static void writePoint(Point point, DataOutputStream out) throws IOException {
    out.writeByte(LegacyShapeType.POINT.ordinal());
    out.writeDouble(point.x());
    out.writeDouble(point.y());
  }

  public static Rectangle readRectangle(DataInput in) throws IOException {
    double minLon = in.readDouble();
    double maxLon = in.readDouble();
    double minLat = in.readDouble();
    double maxLat = in.readDouble();
    return new Rectangle(minLat, maxLat, minLon, maxLon);
  }

  private static void writeRectangle(Rectangle rect, DataOutputStream out) throws IOException {
    out.writeByte(LegacyShapeType.RECTANGLE.ordinal());
    out.writeDouble(rect.left());
    out.writeDouble(rect.right());
    out.writeDouble(rect.bottom());
    out.writeDouble(rect.top());
  }

  public static GeoCircle readCircle(DataInput in) throws IOException {
    double lon = in.readDouble();
    double lat = in.readDouble();
    return new GeoCircle(lat, lon, in.readDouble());
  }

  private static void writeCircle(GeoCircle circle, DataOutputStream out) throws IOException {
    out.writeByte(LegacyShapeType.CIRCLE.ordinal());
    writePoint(circle.center(), out);
    out.writeDouble(circle.getRadiusMeters());
  }
}
