package com.project.imgapi.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.*;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Component
public class S3BlobStorage implements BlobStorage {

  private final S3Client s3;
  private final S3Presigner presigner;
  private final String bucket;
  private final int defaultExpiry;

  public S3BlobStorage(
      @Value("${storage.s3.endpoint}") String endpoint,
      @Value("${storage.s3.region}") String region,
      @Value("${storage.s3.accessKey}") String accessKey,
      @Value("${storage.s3.secretKey}") String secretKey,
      @Value("${storage.s3.bucket}") String bucket,
      @Value("${storage.s3.presignExpirySeconds}") int defaultExpiry
  ) {
    AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);
    var conf = S3Configuration.builder().pathStyleAccessEnabled(true).build();

    this.s3 = S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(creds))
        .endpointOverride(java.net.URI.create(endpoint))
        .serviceConfiguration(conf)
        .build();

    this.presigner = S3Presigner.builder()
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(creds))
        .endpointOverride(java.net.URI.create(endpoint))
        .build();

    this.bucket = bucket;
    this.defaultExpiry = defaultExpiry;
    
    try { 
        s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    } 
    catch (Exception ignored) {}
  }

  @Override
  public String putObject(String keyHint, String contentType, long size, InputStream in) {
    String key = keyHint + "/" + UUID.randomUUID();
    s3.putObject(PutObjectRequest.builder()
            .bucket(bucket).key(key)
            .contentType(contentType).build(),
        RequestBody.fromInputStream(in, size));
    return key;
  }

  @Override public void deleteObject(String key) {
    s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
  }

  @Override public URL presignGet(String key, int expirySeconds) {
    var req = GetObjectRequest.builder().bucket(bucket).key(key).build();
    var presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofSeconds(expirySeconds > 0 ? expirySeconds : defaultExpiry))
        .getObjectRequest(req).build());
    return presigned.url();
  }
}
