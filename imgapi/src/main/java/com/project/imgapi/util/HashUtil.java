package com.project.imgapi.util;

import java.io.InputStream;
import java.security.MessageDigest;

public class HashUtil {
  public static String sha256Hex(InputStream in) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
      byte[] digest = md.digest();
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
