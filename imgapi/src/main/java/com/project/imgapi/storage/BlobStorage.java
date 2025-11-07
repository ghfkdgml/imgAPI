package com.project.imgapi.storage;

import java.io.InputStream;
import java.net.URL;

public interface BlobStorage {
  String putObject(String keyHint, String contentType, long size, InputStream in);
  void deleteObject(String key);
  URL presignGet(String key, int expirySeconds);
}
