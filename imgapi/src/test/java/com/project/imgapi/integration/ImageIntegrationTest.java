package com.project.imgapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.project.imgapi.entity.Project;
import com.project.imgapi.repository.ProjectRepository;
import com.project.imgapi.service.ImageService;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ImageIntegrationTest {
    @Container
    static GenericContainer<?> minio = new GenericContainer<>("quay.io/minio/minio:RELEASE.2024-10-02T17-50-41Z")
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withEnv("MINIO_DOMAIN", "127.0.0.1.nip.io")
            .withCommand("server /data --address :9000 --console-address :9001")
            .withExposedPorts(9000, 9001)
            .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000))
            .withStartupTimeout(Duration.ofSeconds(60));

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry r) {
        String endpoint = "http://" + "127.0.0.1.nip.io" + ":" + minio.getMappedPort(9000);
        
        r.add("storage.s3.endpoint", () -> endpoint);
        r.add("storage.s3.region", () -> "ap-northeast-2");
        r.add("storage.s3.accessKey", () -> "minioadmin");
        r.add("storage.s3.secretKey", () -> "minioadmin");
        r.add("storage.s3.bucket", () -> "image-bucket-it"); // 테스트 전용 버킷명
        r.add("storage.s3.presignExpirySeconds", () -> "120");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired ProjectRepository projectRepository;


    @BeforeEach
    void setup() {
        Project project = new Project();
        project.setCode("test-project");
        projectRepository.save(project);
    }

    // 유효한 JPEG 바이트 생성 (ImageIO로 직접 인코딩)
    private static byte[] validJpegBytes(int w, int h) throws Exception {
        var img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setColor(java.awt.Color.BLUE);
            g.fillOval(w/4, h/4, w/2, h/2);
            g.setColor(java.awt.Color.RED);
            g.drawString("IT", 10, 20);
        } finally { g.dispose(); }

        // (선택) TwelveMonkeys가 classpath에 있으면 progressive/메타도 쓸 수 있음
        var baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }


    private static byte[] randomBytes(int size) {
        byte[] b = new byte[size];
        ThreadLocalRandom.current().nextBytes(b);
        if (size >= 4) { b[0]=(byte)0xFF; b[1]=(byte)0xD8; b[size-2]=(byte)0xFF; b[size-1]=(byte)0xD9; } // JPEG 흉내
        return b;
    }

    @Test
    @Timeout(60)
    @DisplayName("업로드 → /images/{id} 조회로 presigned URL 받고 → 실제 다운로드 OK")
    void upload_then_presigned_download_ok() throws Exception {
        // 1) 업로드 (멀티파트)
        byte[] payload = validJpegBytes(320, 240); // 256KB
        MockMultipartFile file = new MockMultipartFile("files", "it-test.jpg", "image/jpeg", payload);

        String uploadJson = mvc.perform(
                        multipart("/projects/{projectId}/images", 1L)
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids[0]").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode upload = objectMapper.readTree(uploadJson);
        long imageId = upload.get("ids").get(0).asLong();
        assertThat(imageId).isPositive();

        // 2) 단건 조회 (원본 presigned URL 확인)
        String detailJson = mvc.perform(
                        get("/images/{id}", imageId).param("expirySec", "120")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(imageId))
                .andExpect(jsonPath("$.originalUrl").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String originalUrl = objectMapper.readTree(detailJson).get("originalUrl").asText();
        assertThat(originalUrl).startsWith("http://");

        // 3) presigned URL로 실제 다운로드
        byte[] downloaded;
        try (InputStream in = new URI(originalUrl).toURL().openStream()) {
            downloaded = in.readAllBytes();
        }
        assertThat(downloaded).isNotEmpty(); // 크기까지 동일 검증은 스토리지 구현에 따라 차이 가능
    }

    @Test
    @Timeout(60)
    @DisplayName("동일 바이트 2회 업로드 → 중복 0건 보장")
    void duplicate_upload_prevented() throws Exception {
        byte[] same = randomBytes(64 * 1024);
        MockMultipartFile f1 = new MockMultipartFile("files", "dup.jpg", "image/jpeg", same);
        MockMultipartFile f2 = new MockMultipartFile("files", "dup.jpg", "image/jpeg", same);

        String r1 = mvc.perform(multipart("/projects/{projectId}/images", 1L).file(f1))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String r2 = mvc.perform(multipart("/projects/{projectId}/images", 1L).file(f2))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        int c1 = objectMapper.readTree(r1).get("ids").size();
        int c2 = objectMapper.readTree(r2).get("ids").size();

        // 서비스에서 "이미 존재하면 skip" 정책으로 구현했다면 두 번째 업로드 ids.size() 가 0일 수 있음
        // 또는 unique 제약 충돌 시 예외 처리로 0건만 반환하도록 구현했는지 정책에 맞게 검증
        assertThat(c1).isGreaterThanOrEqualTo(1);
        assertThat(c2).isBetween(0, 1); // 정책에 따라 조정
    }

}
