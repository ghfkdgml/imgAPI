package com.project.imgapi.perf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PagingPerformanceTest {

    // ==== 환경 설정 (application.properties 대신 상수로도 OK) ====
    private static final String BASE_URL = System.getProperty("perf.baseUrl", "http://localhost:8080");
    private static final long PROJECT_ID = Long.getLong("perf.projectId", 1L);
    private static final int CONCURRENCY = Integer.getInteger("perf.concurrency", 32);
    private static final int DURATION_SECONDS = Integer.getInteger("perf.durationSeconds", 30);
    private static final int PAGE_SIZE = Integer.getInteger("perf.pageSize", 50);
    private static final int OFFSET_MAX_PAGE = Integer.getInteger("perf.offsetMaxPage", 200);

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    @DisplayName("GET /projects/{id}/images Offset vs Cursor p95 비교 (Cursor p95 < 120ms 목표)")
    void compareOffsetVsCursor() throws Exception {
        PerfMetrics offsetM = new PerfMetrics("GET list (offset)");
        PerfMetrics cursorM = new PerfMetrics("GET list (cursor)");

        AtomicBoolean running = new AtomicBoolean(true);
        long start = System.currentTimeMillis();

        try (ExecutorService pool = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("perf-", 0).factory())) {

            for (int i = 0; i < CONCURRENCY; i++) {
                pool.submit(() -> {
                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    while (running.get()) {
                        boolean doCursor = rnd.nextBoolean(); // offset/cursor 번갈아
                        try {
                            if (doCursor) runCursorOnce(cursorM);
                            else runOffsetOnce(offsetM);
                        } catch (Exception e) {
                            if (doCursor) cursorM.fail(); else offsetM.fail();
                        }
                        // 약간 쉼
                        try { Thread.sleep(rnd.nextLong(10, 50)); } catch (InterruptedException ignored) {}
                    }
                });
            }

            Thread.sleep(Duration.ofSeconds(DURATION_SECONDS));
            running.set(false);
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }        

        long elapsed = Math.max(1, System.currentTimeMillis() - start);

        var rOffset  = offsetM.snapshot(elapsed);
        var rCursor  = cursorM.snapshot(elapsed);

        System.out.printf("%n== Paging Results ==%n");
        print(rOffset);
        print(rCursor);

        // 목표: Cursor p95 < 120ms
        assertTrue(rCursor.p95ms() < 120.0,
                "Cursor p95 must be < 120ms but was " + rCursor.p95ms());
    }

    // ===== 시나리오 =====

    private void runOffsetOnce(PerfMetrics m) throws Exception {
        int page = ThreadLocalRandom.current().nextInt(Math.max(1, OFFSET_MAX_PAGE + 1));
        var uri = String.format("%s/projects/%d/images?mode=offset&page=%d&size=%d",
                BASE_URL, PROJECT_ID, page, PAGE_SIZE);
        var req = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        long t0 = System.nanoTime();
        var res = http.send(req, HttpResponse.BodyHandlers.discarding());
        long dt = System.nanoTime() - t0;
        if (res.statusCode() == 200) m.ok(dt); else m.fail();
    }

    private void runCursorOnce(PerfMetrics m) throws Exception {
        String cursor = null;
        int chain = 3; // 연속 3페이지
        int lastStatus = 200;

        long t0 = System.nanoTime();
        for (int i = 0; i < chain; i++) {
            String uri = String.format("%s/projects/%d/images?mode=cursor&size=%d%s",
                    BASE_URL, PROJECT_ID, PAGE_SIZE, (cursor != null ? "&cursor=" + cursor : ""));
            var req = HttpRequest.newBuilder(URI.create(uri))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            lastStatus = res.statusCode();
            if (lastStatus != 200) break;

            // nextCursor 매우 단순 파싱 (JSON 파서 대신)
            String body = res.body();
            int idx = body.indexOf("\"nextCursor\":");
            if (idx >= 0) {
                int s = body.indexOf(':', idx) + 1;
                int e = body.indexOf(',', s);
                if (e < 0) e = body.indexOf('}', s);
                String raw = body.substring(s, e).trim();
                cursor = "null".equals(raw) ? null : raw;
            } else {
                cursor = null;
            }
            if (cursor == null) break;
        }
        long dt = System.nanoTime() - t0;
        if (lastStatus == 200) m.ok(dt); else m.fail();
    }

    private static void print(PerfMetrics.Report r) {
        System.out.printf("[%s] success=%d, error=%d, RPS=%.1f, p50=%.1fms, p95=%.1fms, p99=%.1fms%n",
                r.name(), r.success(), r.error(), r.rps(), r.p50ms(), r.p95ms(), r.p99ms());
    }
}
