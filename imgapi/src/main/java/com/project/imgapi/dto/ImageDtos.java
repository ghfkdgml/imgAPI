package com.project.imgapi.dto;

import jakarta.validation.constraints.Size;
import java.net.URL;
import java.time.Instant;
import java.util.List;

import com.project.imgapi.enums.ImageStatus;

public class ImageDtos {
  public record UploadResponse(List<Long> ids) {}
  public record Detail(Long id, Long projectId, String filename, String contentType, long sizeBytes,
                       String tags, String memo, ImageStatus status,
                       URL originalUrl, URL thumbnailUrl,
                       Instant createdAt, Instant updatedAt, Long version) {}
  public record PatchRequest(
      @Size(max=1000) String tags,
      @Size(max=2000) String memo,
      ImageStatus status,
      Long version
  ) {}
  public record ListItem(Long id, String filename, ImageStatus status, String tags, long sizeBytes, Instant createdAt) {}
  public record OffsetList(List<ListItem> items, long total, int page, int size) {}
  public record CursorList(List<ListItem> items, Long nextCursor) {}
}
