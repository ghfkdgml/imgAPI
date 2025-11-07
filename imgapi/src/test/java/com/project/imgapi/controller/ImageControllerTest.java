package com.project.imgapi.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import org.springframework.test.web.servlet.MockMvc;

import com.project.imgapi.dto.ImageDtos.UploadResponse;
import com.project.imgapi.enums.ImageStatus;
import com.project.imgapi.service.ImageService;

import java.net.URL;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ImageController.class)
class ImageControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ImageService imageService;  // 이제 @MockBean 없이 주입됨

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public ImageService mockImageService() {
            return Mockito.mock(ImageService.class);
        }
    }

    @Test
    @DisplayName("POST /projects/{id}/images 업로드")
    void upload() throws Exception {
        var file = new MockMultipartFile("files", "test.jpg", "image/jpeg", new byte[]{1,2,3});

        Mockito.when(imageService.upload(Mockito.eq(1L), Mockito.anyList()))
                .thenReturn(new UploadResponse(List.of(10L, 11L)));

        mvc.perform(multipart("/projects/{pid}/images", 1L)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids[0]", is(10)));
    }

    
}
