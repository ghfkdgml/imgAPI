package com.project.imgapi.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.transaction.Transactional;

import com.project.imgapi.dto.ImageDtos.ListItem;
import com.project.imgapi.dto.ImageDtos.UploadResponse;
import com.project.imgapi.entity.ImageAsset;
import com.project.imgapi.entity.Project;
import com.project.imgapi.enums.ImageStatus;
import com.project.imgapi.repository.ImageAssetRepository;
import com.project.imgapi.storage.BlobStorage;
import com.project.imgapi.util.HashUtil;

@Service
public class ImageService {
    private final ImageAssetRepository repo;
    private final BlobStorage storage;
    private final ThumbnailService thumbnailService;

    public ImageService(ImageAssetRepository repo, BlobStorage storage, ThumbnailService thumbnailService) {
        this.repo = repo; this.storage = storage; this.thumbnailService = thumbnailService;
    }    

    @Transactional
    public UploadResponse upload(Long projectId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) throw new IllegalArgumentException("No files");

        List<Long> ids = new ArrayList<>();

        for (MultipartFile f : files) {
            try (
                InputStream forHash = f.getInputStream();
                InputStream forPut  = f.getInputStream()
            ) 
            {
                String hash = HashUtil.sha256Hex(forHash);
                // 중복 업로드 방지 (DB 유니크 제약 + exists 체크)
                if (repo.existsByProjectIdAndContentHashAndSoftDeleteFalse(projectId, hash)) {
                    continue; // 혹은 예외로 처리해도 됨
                }
                String objectKey = storage.putObject("original/%d".formatted(projectId), safeType(f.getContentType()), f.getSize(), forPut);

                ImageAsset a = new ImageAsset();
                Project p = new Project(); p.setId(projectId); a.setProject(p);

                a.setOriginalFilename(Objects.requireNonNullElse(f.getOriginalFilename(), "unnamed"));
                a.setContentType(safeType(f.getContentType()));
                a.setSizeBytes(f.getSize());
                a.setContentHash(hash);
                a.setObjectKey(objectKey);
                a.setStatus(ImageStatus.PROCESSING);

                repo.saveAndFlush(a);
                ids.add(a.getId());

                // 비동기 썸네일 생성 (지수 백오프, 최대 3회)
                thumbnailService.generateAsync(a.getId());
            } catch (DataIntegrityViolationException e) {
                // (project, hash) 유니크 충돌 → 중복 생성 0건 보장
            } catch (IOException io) {
                throw new RuntimeException(io);
            }
        }
        return new UploadResponse(ids);
    }

    private static String safeType(String ct){ 
        return ct==null? "application/octet-stream": ct; 
    }

    private ListItem toItem(ImageAsset a) {
        return new ListItem(a.getId(), a.getOriginalFilename(), a.getStatus(), a.getTags(), a.getSizeBytes(), a.getCreatedAt());
    }

    // 내부용(썸네일 서비스)
    @Transactional
    public Optional<ImageAsset> findEntity(Long id){ 
        return repo.findByIdAndSoftDeleteFalse(id); 
    }

    @Transactional
    public void setThumbnailReady(Long id, String thumbnailKey){
        ImageAsset a = repo.findByIdAndSoftDeleteFalse(id).orElseThrow();

        a.setThumbnailKey(thumbnailKey);
        a.setStatus(ImageStatus.READY);
    }

    @Transactional
    public void setThumbnailFailed(Long id){
        ImageAsset a = repo.findByIdAndSoftDeleteFalse(id).orElseThrow();

        a.setStatus(ImageStatus.FAILED);
    }
}
