package com.project.imgapi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.imgapi.dto.ImageDtos.UploadResponse;
import com.project.imgapi.service.ImageService;

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
}
