/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.rocket.sqlstore.test.types;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;

/**
 * Helper class for mapping tests that need to work with {@link InputStream}, {@link File},
 * {@link Blob}, {@link Clob}, and {@link NClob}.
 */
final class ConversionHelper {

  static Blob makeBlobBytes(String content) throws SQLException {
    Blob result = mock(Blob.class);
    when(result.length()).thenReturn(content != null ? (long) content.length() : 0);
    if (content != null && !content.isEmpty()) {
      when(result.getBytes(0, content.length())).thenReturn(content.getBytes());
    }
    return result;
  }

  static Blob makeBlob(String content) throws SQLException {
    Blob result = mock(Blob.class);
    when(result.length()).thenReturn(content != null ? (long) content.length() : 0);
    if (content != null && !content.isEmpty()) {
      when(result.getBinaryStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
    }
    return result;
  }

  static Clob makeClob(String content) throws SQLException {
    Clob result = mock(Clob.class);
    when(result.length()).thenReturn(content != null ? (long) content.length() : 0);
    if (content != null && !content.isEmpty()) {
      when(result.getCharacterStream()).thenReturn(new StringReader(content));
    }
    return result;
  }

  static NClob makeNclob(String content) throws SQLException {
    NClob result = mock(NClob.class);
    when(result.length()).thenReturn(content != null ? (long) content.length() : 0);
    if (content != null && !content.isEmpty()) {
      when(result.getCharacterStream()).thenReturn(new StringReader(content));
    }
    return result;
  }

  static String asString(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    return new String(bytes);
  }

  static String asString(InputStream is) throws IOException {
    if (is == null) {
      return null;
    }

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    int b;
    while ((b = is.read()) != -1) {
      bytes.write(b);
    }
    return bytes.toString();
  }

  static String asString(Reader reader) throws IOException {
    if (reader == null) {
      return null;
    }

    StringBuilder text = new StringBuilder();
    int b;
    while ((b = reader.read()) != -1) {
      text.appendCodePoint(b);
    }
    return text.toString();
  }

  static String asString(File file) throws IOException {
    StringBuilder sb = new StringBuilder();

    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line = reader.readLine();

      while (line != null && !line.isEmpty()) {
        if (sb.length() > 0) {
          sb.append('\n');
        }
        sb.append(line);
        line = reader.readLine();
      }
    }

    return sb.toString();
  }

  private ConversionHelper() {
    throw new AssertionError("Cannot create an instance of this class");
  }

}
