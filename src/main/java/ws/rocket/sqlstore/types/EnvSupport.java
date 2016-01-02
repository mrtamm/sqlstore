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

package ws.rocket.sqlstore.types;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helping class that provides support for handling LOBs: copying the data to an array, stream or
 * file. The instance is created on first use and it stores a couple of settings, which can be tuned
 * using JVM system properties.
 * <p>
 * There are two non-mandatory system properties that this class relies on:
 * <ul>
 * <li><code>sqlstore.lob.tempDir</code> -- directory path where LOB files may be temporarily stored
 * (defaults to <em>tmp.dir</em>);
 * <li><code>sqlstore.lob.valueMaxLength</code> -- a positive integer value specifying the upper
 * limit how much LOB data can be stored in a byte array (BLOB) and String (CLOB) (defaults to 500
 * MB). The rest of the LOB data is not read.
 * </ul>
 */
public final class EnvSupport {

  private static final Logger LOG = LoggerFactory.getLogger(EnvSupport.class);

  private static final int DEFAULT_MAX_LOB_VALUE_LENGTH = 500 * 1024 * 1024;

  private static EnvSupport instance;

  /**
   * Provides the instance to use. The instance is created on first use.
   *
   * @return a not null instance.
   */
  public static EnvSupport getInstance() {
    if (instance == null) {
      instance = new EnvSupport();
    }
    return instance;
  }

  /**
   * Provides a buffering input stream for given file. This method assumes that the file is valid
   * and can be read. The file will be read using the system encoding.
   *
   * @param f The file where data is streamed from.
   * @return The buffering file stream to use.
   * @throws FileNotFoundException Raised by <code>FileInputStream</code>.
   */
  public static InputStream fileInStream(File f) throws FileNotFoundException {
    return new BufferedInputStream(new FileInputStream(f));
  }

  /**
   * Provides a buffering reader for given file. This method assumes that the file is valid and can
   * be read. The file will be read using the system encoding.
   *
   * @param f The file where data is read from.
   * @return The buffering file reader to use.
   * @throws FileNotFoundException Raised by <code>FileReader</code>.
   */
  public static Reader fileReader(File f) throws FileNotFoundException {
    return new BufferedReader(new FileReader(f));
  }

  /**
   * Validates whether the given file is actually a readable file with content.
   *
   * @param f The value to check, possibly null.
   * @return Boolean true when input is a readable file with content.
   */
  public static boolean isFileAvailable(File f) {
    return f != null && f.exists() && f.isFile() && f.canRead() && f.length() > 0;
  }

  private static int resolveMaxLength(String propName) {
    String propVal = System.getProperty(propName);
    String defaultValStr = Integer.toString(DEFAULT_MAX_LOB_VALUE_LENGTH);

    if (propVal == null || propVal.isEmpty()
        || !Character.isDigit(propVal.charAt(0))
        || propVal.length() > defaultValStr.length()
        || propVal.length() == defaultValStr.length() && propVal.compareTo(defaultValStr) > 0) {
      return DEFAULT_MAX_LOB_VALUE_LENGTH;
    }

    try {
      return Integer.parseInt(propVal);
    } catch (NumberFormatException e) {
      System.err.println("Bad integer value for System property " + propName + ".");
    }

    return DEFAULT_MAX_LOB_VALUE_LENGTH;
  }

  private static File resolveTempDir(String propName) {
    String propVal = System.getProperty(propName);

    if (propVal == null || propVal.isEmpty()) {
      return null;
    }

    File dir = new File(propVal);
    if (!dir.exists()) {
      LOG.error("The path specified by Java system property '{}' does not exist.", propName);
    } else if (!dir.isDirectory()) {
      LOG.error("The path specified by Java system property '{}' is not a directory.", propName);
    } else {
      return dir;
    }

    return null;
  }

  private final File tempDir;

  private final int lobMaxLength;

  private EnvSupport() {
    this.tempDir = resolveTempDir("sqlstore.lob.tempDir");
    this.lobMaxLength = resolveMaxLength("sqlstore.lob.valueMaxLength");
  }

  /**
   * Provides an integer specifying how much LOB data should be read into memory (as byte array or
   * string). This method returns -1 when the LOB data is basically null (empty).
   * <p>
   * The return value depends on the internally configured upper limit, which defaults to 500 MB,
   * but can also be configured using system property <code>sqlstore.lob.valueMaxLength</code>. When
   * the actual LOB size exceeds the upper limit, the return value will be the upper limit.
   * Otherwise, it will be the actual LOB size.
   *
   * @param lobLength The actual LOB size.
   * @return The amount of LOB data to read, or -1.
   */
  public int getLobSizeForArray(long lobLength) {
    if (lobLength <= 0) {
      return -1;
    }
    return lobLength > this.lobMaxLength ? this.lobMaxLength : (int) lobLength;
  }

  /**
   * Copies the stream data into a temporary file (which will hopefully be deleted no later than on
   * JVM exit). This method also checks beforehand whether there is enough space on the disk of the
   * folder where the temporary file will be placed. When the check fails, it will be logged, and
   * this method will return null instead.
   *
   * @param src The LOB data source.
   * @param length The expected length of LOB data source.
   * @return The file where data was copied, or null when the file cannot be created.
   */
  public File copyToFile(InputStream src, long length) {
    File result = null;

    if (!hasEnoughSpace(length)) {
      LOG.error("Detected low free disk space for storing a LOB of {} bytes. Writing to disk "
          + "is skipped for LOB data where free space is less than twice the data size.", length);
    } else {
      result = createFile();

      if (result != null) {
        copyContent(src, result);
        result.deleteOnExit();
      }
    }

    return result;
  }

  /**
   * Copies the reader data into a temporary file (which will hopefully be deleted no later than on
   * JVM exit). This method also checks beforehand whether there is enough space on the disk of the
   * folder where the temporary file will be placed. When the check fails, it will be logged, and
   * this method will return null instead.
   *
   * @param src The LOB data source.
   * @param length The expected length of LOB data source.
   * @return The file where data was copied, or null when the file cannot be created.
   */
  public File copyToFile(Reader src, long length) {
    File result = null;

    if (!hasEnoughSpace(length)) {
      LOG.error("Detected low free disk space for storing a LOB of {} bytes. Writing to disk "
          + "is skipped for LOB data where free space is less than twice the data size.", length);
    } else {
      result = createFile();

      if (result != null) {
        copyContent(src, result);
        result.deleteOnExit();
      }
    }

    return result;
  }

  /**
   * Copies stream data into a temporary file and returns a stream that reads data from the file.
   * This method basically extends the {@link #copyContent(InputStream, File)} method by returning a
   * stream instead of the file, when available.
   *
   * @param src The LOB data source.
   * @param length The expected length of LOB data source.
   * @return The stream for reading the LOB content from a temporary file, or null when the file
   * cannot be created.
   */
  public InputStream copyToFileForStream(InputStream src, long length) {
    try {
      File destFile = copyToFile(src, length);
      return destFile != null ? fileInStream(destFile) : null;
    } catch (FileNotFoundException e) {
      LOG.warn("File was not found after being created?", e);
    }
    return null;
  }

  /**
   * Copies reader data into a temporary file and returns a reader that reads data from the file.
   * This method basically extends the {@link #copyContent(Reader, File)} method by returning a
   * reader instead of the file, when available.
   *
   * @param src The LOB data source.
   * @param length The expected length of LOB data source.
   * @return The reader for reading the LOB content from a temporary file, or null when the file
   * cannot be created.
   */
  public InputStream copyToFileForStream(Reader src, long length) {
    try {
      File destFile = copyToFile(src, length);
      return destFile != null ? new FileDeletingInputStream(destFile) : null;
    } catch (FileNotFoundException e) {
      LOG.warn("File was not found after being created?", e);
    }
    return null;
  }

  private boolean hasEnoughSpace(long size) {
    if (this.tempDir == null) {
      return true;
    } else if (size < 0) {
      return false;
    }

    long freeSpace = this.tempDir.getFreeSpace();
    if (freeSpace <= size) {
      return false;
    } else if (size * 2 > 0 && freeSpace <= size * 2) {
      return false;
    }

    return true;
  }

  private File createFile() {
    try {
      return File.createTempFile("tmp_sqlstore", ".lob", this.tempDir);
    } catch (IOException e) {
      LOG.error("Failed to create a temporary file at {}.", this.tempDir, e);
    }
    return null;
  }

  private void copyContent(InputStream src, File file) {
    try (InputStream is = new BufferedInputStream(src);
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {

      byte[] buffer = new byte[1024];
      int c;

      while ((c = is.read(buffer)) != -1) {
        os.write(buffer, 0, c);
      }

      os.flush();
    } catch (IOException e) {
      LOG.error("Failed to copy BLOB data to a temporary file.", e);
    }
  }

  private void copyContent(Reader src, File file) {
    try (Reader r = new BufferedReader(src);
        Writer w = new BufferedWriter(new FileWriter(file))) {

      char[] buffer = new char[1024];
      int c;

      while ((c = r.read(buffer)) != -1) {
        w.write(buffer, 0, c);
      }

      w.flush();
    } catch (IOException e) {
      LOG.error("Failed to copy CLOB data to a temporary file.", e);
    }
  }

  private static class FileDeletingInputStream extends FilterInputStream {

    private final File src;

    FileDeletingInputStream(File src) throws FileNotFoundException {
      super(fileInStream(src));
      this.src = src;
    }

    @Override
    public int read() throws IOException {
      int val = super.read();

      if (val == -1) {
        close();
      }

      return val;
    }

    @Override
    public void close() throws IOException {
      try {
        super.close();
      } finally {
        if (this.src.exists()) {
          this.src.delete();
        }
      }
    }

  }

}
