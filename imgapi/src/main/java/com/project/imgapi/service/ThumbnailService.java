package com.project.imgapi.service;

import javax.imageio.ImageIO;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.project.imgapi.entity.ImageAsset;
import com.project.imgapi.storage.BlobStorage;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;

@Service
public class ThumbnailService {

  private final ImageService imageService;
  private final BlobStorage storage;

  public ThumbnailService(ImageService imageService, BlobStorage storage) {
    this.imageService = imageService; this.storage = storage;
  }

  @Async
  @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
  public void generateAsync(Long imageId) {
    ImageAsset a = imageService.findEntity(imageId).orElseThrow();
    try {
      // 원본 다운로드
      URL url = storage.presignGet(a.getObjectKey(), 300);
      BufferedImage src;

      try (InputStream in = url.openStream()) {
        src = ImageIO.read(in);
      }
      
      if (src == null) throw new IOException("unsupported image");

      // 썸네일 생성(가로 512 기준)
      int w = src.getWidth(), h = src.getHeight();
      int tw = 512, th = (int)Math.max(1, (double)h * tw / w);
      BufferedImage out = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
      java.awt.Graphics2D g = out.createGraphics();
      g.drawImage(src, 0, 0, tw, th, null); g.dispose();

      // 업로드
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        ImageIO.write(out, "jpg", baos);
        byte[] bytes = baos.toByteArray();
        try (InputStream tin = new ByteArrayInputStream(bytes)) {
          String key = storage.putObject("thumbnail/%d".formatted(a.getProject().getId()),"image/jpeg", bytes.length, tin);
          imageService.setThumbnailReady(imageId, key);
        }
      }
    } catch (Exception e) {
      imageService.setThumbnailFailed(imageId);
      throw new RuntimeException(e);
    }
  }
}
