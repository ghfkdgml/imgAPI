package com.project.imgapi.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.imgapi.dto.ImageDtos.Detail;
import com.project.imgapi.dto.ImageDtos.PatchRequest;
import com.project.imgapi.dto.ImageDtos.UploadResponse;
import com.project.imgapi.enums.ImageStatus;
import com.project.imgapi.service.ImageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping
public class ImageController {
    private final ImageService imageService;

    public ImageController(ImageService service){ this.imageService = service; }    

    @PostMapping(value = "/projects/{projectId}/images", consumes = "multipart/form-data")
    public UploadResponse upload(@PathVariable Long projectId,
                                @RequestPart("files") List<MultipartFile> files) {
        return imageService.upload(projectId, files);
    }

    // 이미지 목록 조회: Offset / Cursor
    @GetMapping("/projects/{projectId}/images")
    public Object list(@PathVariable Long projectId,
                        @RequestParam(defaultValue = "offset") String mode,
                        @RequestParam(required = false) ImageStatus status,
                        @RequestParam(required = false) String tags,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(required = false) Long cursor) {
        if ("cursor".equalsIgnoreCase(mode)) {
        return imageService.listCursor(projectId, status, tags, cursor, size);
        }
        return imageService.listOffset(projectId, status, tags, page, size);
    }

    // 이미지 단건 조회 (메타 + 프리사인 URL)
    @GetMapping("/images/{id}")
    public Detail get(@PathVariable Long id,
                        @RequestParam(defaultValue = "600") int expirySec) {
        return imageService.get(id, expirySec);
    }

    // 이미지 수정 (태그, 메모, 상태) + 낙관적 락
    @PatchMapping("/images/{id}")
    public ResponseEntity<?> patch(@PathVariable Long id, @RequestBody @Valid PatchRequest req) {
        imageService.patch(id, req);
        return ResponseEntity.noContent().build();
    }

    // 이미지 삭제 (소프트 삭제)
    @DeleteMapping("/images/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        imageService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
