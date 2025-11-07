package com.project.imgapi.repository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.project.imgapi.entity.ImageAsset;
import com.project.imgapi.enums.ImageStatus;

import java.util.List;
import java.util.Optional;

public interface ImageAssetRepository extends JpaRepository<ImageAsset, Long> {
  @EntityGraph(attributePaths = {"project"})
  Optional<ImageAsset> findByIdAndSoftDeleteFalse(Long id);

  boolean existsByProjectIdAndContentHashAndSoftDeleteFalse(Long projectId, String contentHash);
}
