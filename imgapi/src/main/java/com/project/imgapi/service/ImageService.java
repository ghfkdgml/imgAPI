package com.project.imgapi.service;

import java.io.*;
import java.net.URL;
import java.time.Instant;
import java.util.*;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.transaction.Transactional;

import com.project.imgapi.dto.ImageDtos.CursorList;
import com.project.imgapi.dto.ImageDtos.Detail;
import com.project.imgapi.dto.ImageDtos.ListItem;
import com.project.imgapi.dto.ImageDtos.OffsetList;
import com.project.imgapi.dto.ImageDtos.PatchRequest;
import com.project.imgapi.dto.ImageDtos.UploadResponse;
import com.project.imgapi.entity.ImageAsset;
import com.project.imgapi.entity.Project;
import com.project.imgapi.enums.ImageStatus;
import com.project.imgapi.repository.ImageAssetRepository;
import com.project.imgapi.storage.BlobStorage;
import com.project.imgapi.util.HashUtil;

@Service
public class ImageService {
    private final ImageAssetRepository imageAssetRepo;
    private final BlobStorage storage;
    private final ThumbnailService thumbnailService;

    public ImageService(ImageAssetRepository repo, BlobStorage storage, @Lazy ThumbnailService thumbnailService) {
        this.imageAssetRepo = repo; this.storage = storage; this.thumbnailService = thumbnailService;
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
                if (imageAssetRepo.existsByProjectIdAndContentHashAndSoftDeleteFalse(projectId, hash)) {
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

                imageAssetRepo.saveAndFlush(a);
                ids.add(a.getId());

                // 비동기 썸네일 생성 (지수 백오프, 최대 3회)
                thumbnailService.generateAsync(a.getId());
            } catch (DataIntegrityViolationException e) {
                throw new RuntimeException(e);
                // (project, hash) 유니크 충돌 → 중복 생성 0건 보장
            } catch (IOException io) {
                throw new RuntimeException(io);
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return new UploadResponse(ids);
    }

    @Transactional
    public void softDelete(Long id) {
        ImageAsset a = imageAssetRepo.findByIdAndSoftDeleteFalse(id).orElseThrow(() -> new IllegalArgumentException("not found"));
        a.setSoftDelete(true);
        a.setStatus(ImageStatus.DELETED);
    }

    @Transactional
    public void patch(Long id, PatchRequest req) {
        ImageAsset a = imageAssetRepo.findByIdAndSoftDeleteFalse(id).orElseThrow(() -> new IllegalArgumentException("not found"));
        if (req.version()!=null && !req.version().equals(a.getVersion()))
        throw new org.springframework.dao.OptimisticLockingFailureException("version_conflict");
        if (req.tags()!=null) a.setTags(req.tags());
        if (req.memo()!=null) a.setMemo(req.memo());
        if (req.status()!=null) a.setStatus(req.status());
    }

    @Transactional
    public Detail get(Long id, int presignExpirySeconds) {
        ImageAsset a = imageAssetRepo.findByIdAndSoftDeleteFalse(id).orElseThrow(() -> new IllegalArgumentException("not found"));
        URL orig = a.getObjectKey()==null? null : storage.presignGet(a.getObjectKey(), presignExpirySeconds);
        URL thum = a.getThumbnailKey()==null? null : storage.presignGet(a.getThumbnailKey(), presignExpirySeconds);
        return new Detail(
            a.getId(), a.getProject().getId(), a.getOriginalFilename(), a.getContentType(), a.getSizeBytes(),
            a.getTags(), a.getMemo(), a.getStatus(), orig, thum, a.getCreatedAt(), a.getUpdatedAt(), a.getVersion()
        );
    }

    @Transactional
    public OffsetList listOffset(Long projectId, ImageStatus status, String tags, int page, int size) {
        Page<ImageAsset> p = imageAssetRepo.findPage(projectId, status, tags, PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "id")));
        List<ListItem> items = p.getContent().stream().map(this::toItem).toList();
        return new OffsetList(items, p.getTotalElements(), page, size);
    }

    @Transactional
    public CursorList listCursor(Long projectId, ImageStatus status, String tags, Long cursor, int size) {
        List<ImageAsset> rows = imageAssetRepo.scanDesc(projectId, status, tags, cursor, PageRequest.of(0, Math.min(size, 200)));
        Long next = rows.isEmpty() ? null : rows.get(rows.size()-1).getId();
        return new CursorList(rows.stream().map(this::toItem).toList(), next);
    }

    private static String safeType(String ct){ 
        return ct==null? "application/octet-stream": ct; 
    }

    private ListItem toItem(ImageAsset a) {
        return new ListItem(a.getId(), a.getOriginalFilename(), a.getStatus(), a.getTags(), a.getSizeBytes(), a.getCreatedAt());
    }    
}
