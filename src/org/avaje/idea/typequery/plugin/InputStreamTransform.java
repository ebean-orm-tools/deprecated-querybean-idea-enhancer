package org.avaje.idea.typequery.plugin;

import org.avaje.ebean.typequery.agent.Transformer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;


/**
 * Utility object that handles input streams for reading and writing.
 */
public class InputStreamTransform {

  private final Transformer transformer;

  private final ClassLoader classLoader;

  public InputStreamTransform(Transformer transformer, ClassLoader classLoader) {
    this.transformer = transformer;
    this.classLoader = classLoader;
  }

  /**
   * Transform a file.
   */
  public byte[] transform(String className, File file) throws IOException, IllegalClassFormatException {
    try {
      return transform(className, new FileInputStream(file));

    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Transform a input stream.
   */
  public byte[] transform(String className, InputStream is) throws IOException, IllegalClassFormatException {

    try {
      byte[] classBytes = readBytes(is);
      return transformer.transform(classLoader, className, null, null, classBytes);

    } finally {
      if (is != null) {
        is.close();
      }
    }
  }

  public static byte[] readBytes(InputStream is) throws IOException {

    BufferedInputStream bis = new BufferedInputStream(is);
    ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);

    byte[] buf = new byte[1028];

    int len;
    while ((len = bis.read(buf, 0, buf.length)) > -1) {
      baos.write(buf, 0, len);
    }
    baos.flush();
    baos.close();
    return baos.toByteArray();
  }
}
