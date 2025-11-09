package com.project.imgapi.repository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.project.imgapi.entity.ImageAsset;
import com.project.imgapi.enums.ImageStatus;

import java.util.List;
import java.util.Optional;

public interface ImageAssetRepository extends JpaRepository<ImageAsset, Long> {

  @Query("""
   select i from ImageAsset i
   where i.project.id = :projectId
     and i.softDelete = false
     and (:status is null or i.status = :status)
     and (:tags is null or lower(i.tags) like lower(concat('%', :tags, '%')))
  """)
  Page<ImageAsset> findPage(@Param("projectId") Long projectId,
                            @Param("status") ImageStatus status,
                            @Param("tags") String tags,
                            Pageable pageable);

  // Cursor (id DESC, lastId 커서)
  @Query("""
    select i from ImageAsset i
    where i.project.id = :projectId
      and i.softDelete = false
      and (:status is null or i.status = :status)
      and (:lastId is null or i.id < :lastId)
    order by i.id desc
  """)
  List<ImageAsset> scanDesc(@Param("projectId") Long projectId,
                            @Param("status") ImageStatus status,
                            @Param("tags") String tags,
                            @Param("lastId") Long lastId,
                            Pageable limit);

  @EntityGraph(attributePaths = {"project"})
  Optional<ImageAsset> findByIdAndSoftDeleteFalse(Long id);

  boolean existsByProjectIdAndContentHashAndSoftDeleteFalse(Long projectId, String contentHash);
}
