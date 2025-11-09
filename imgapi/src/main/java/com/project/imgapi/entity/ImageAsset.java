package com.project.imgapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

import com.project.imgapi.enums.ImageStatus;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
  name = "image_asset",
  indexes = {
    @Index(
      name = "idx_project_softdel_status_id_desc",
      columnList = "project_id, soft_delete, status, id DESC"
    ),
    @Index(name = "idx_tags", columnList = "tags")
  },
  uniqueConstraints = {
    @UniqueConstraint(
      name = "uk_project_hash",
      columnNames = {"project_id", "content_hash"}
    )
  }
)
public class ImageAsset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="project_id", nullable=false)
    private Project project;

    @Column(name="original_filename", nullable=false) 
    private String originalFilename;
    @Column(name="content_type", nullable=false) 
    private String contentType;
    @Column(name="size_bytes", nullable=false) 
    private long sizeBytes;

    @Column(name="content_hash", nullable=false, length=64) 
    private String contentHash;

    @Column(name="object_key", nullable=false) 
    private String objectKey;          // S3 원본
    @Column(name="thumbnail_key") 
    private String thumbnailKey;                    // S3 썸네일

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private ImageStatus status = ImageStatus.PROCESSING;

    @Column(nullable=false) 
    private boolean softDelete = false;

    private String tags;
    private String memo;

    @Version private Long version;
    @Column(nullable=false) private Instant createdAt = Instant.now();
    @Column(nullable=false) private Instant updatedAt = Instant.now();

    @PreUpdate void touch(){ updatedAt = Instant.now(); }
}
