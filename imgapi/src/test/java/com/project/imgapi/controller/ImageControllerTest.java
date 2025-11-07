package com.project.imgapi.controller;



import com.project.imgapi.enums.ImageStatus;
import com.project.imgapi.service.ImageService;
import com.project.imgapi.dto.ImageDtos.*;

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

import java.net.URI;
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
    ImageService imageService;

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

    @Test
    @DisplayName("GET /projects/{id}/images?mode=offset")
    void listOffset() throws Exception {
        var items = List.of(
                new ListItem(1L, "a.jpg", ImageStatus.READY, "t1", 33L, Instant.now())
        );

        Mockito.when(imageService.listOffset(Mockito.eq(1L), Mockito.any(), Mockito.any(), Mockito.eq(0), Mockito.eq(20)))
                .thenReturn(new OffsetList(items, 1, 0, 20));

        mvc.perform(get("/projects/{pid}/images", 1L)
                        .param("mode", "offset")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    @DisplayName("GET /projects/{id}/images?mode=cursor")
    void listCursor() throws Exception {
        var items = List.of(
                new ListItem(100L, "x.jpg", ImageStatus.READY, null, 111L, Instant.now())
        );

        Mockito.when(imageService.listCursor(Mockito.eq(1L), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq(50)))
                .thenReturn(new CursorList(items, 99L));

        mvc.perform(get("/projects/{pid}/images", 1L)
                        .param("mode", "cursor")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor", is(99)));
    }

    @Test
    @DisplayName("GET /images/{id}")
    void getDetail() throws Exception {
        var detail = new Detail(
                7L, 1L, "orig.jpg", "image/jpeg", 12345L,
                "t1", "memo", ImageStatus.READY,
                new URI("http://origin").toURL(),
                new URI("http://thumb").toURL(),
                Instant.now(), Instant.now(), 1L
        );

        Mockito.when(imageService.get(Mockito.eq(7L), Mockito.eq(600)))
                .thenReturn(detail);

        mvc.perform(get("/images/{id}", 7L)
                        .param("expirySec", "600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7)));
    }

    @Test
    @DisplayName("PATCH /images/{id}")
    void patch_ok() throws Exception {
        Mockito.doNothing().when(imageService).patch(Mockito.eq(7L), Mockito.any());

        mvc.perform(patch("/images/{id}", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tags":"t1","memo":"m","status":"READY","version":1}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /images/{id}")
    void delete_() throws Exception {
        Mockito.doNothing().when(imageService).softDelete(Mockito.eq(9L));

        mvc.perform(delete("/images/{id}", 9L))
                .andExpect(status().isNoContent());
    }
}
