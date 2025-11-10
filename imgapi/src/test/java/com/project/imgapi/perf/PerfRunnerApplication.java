package com.project.imgapi.perf;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootApplication
@ActiveProfiles("perf")
@EnableConfigurationProperties(PerfProperties.class)
public class PerfRunnerApplication implements CommandLineRunner {

    public static void main(String[] args) {
        new SpringApplicationBuilder(PerfRunnerApplication.class)
        .profiles("perf")                            // perf 프로필 사용
        .web(WebApplicationType.NONE)                // 웹 서버 비활성화
        .run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        var cfg = AppContext.get(PerfProperties.class);

        var scenarios = List.of(
                new UploadScenario(),
                new ListCursorScenario(),
                new ListOffsetScenario()
        );

        var metrics = scenarios.stream().collect(
                java.util.stream.Collectors.toMap(Scenario::name, s -> new PerfMetrics(s.name()))
        );

        System.out.printf(
                "== PERF START ==%nbaseUrl=%s, projectId=%d, concurrency=%d, duration=%ds%n",
                cfg.baseUrl(), cfg.projectId(), cfg.concurrency(), cfg.durationSeconds()
        );

        long startAt = System.currentTimeMillis();
        AtomicBoolean running = new AtomicBoolean(true);

        try (var pool = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("perf-", 0).factory())) {
            for (int i=0;i<cfg.concurrency();i++){
                pool.submit(() -> {
                    var rnd = ThreadLocalRandom.current();
                    while (running.get()){
                        var s = scenarios.get(rnd.nextInt(scenarios.size()));
                        try {
                            s.runOnce(cfg, metrics.get(s.name()));
                        } catch (Exception e){
                            metrics.get(s.name()).fail();
                        }
                        // 과격 폭격 방지
                        try {
                            Thread.sleep(rnd.nextLong(20, 60));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            Thread.sleep(Duration.ofSeconds(cfg.durationSeconds()));
            running.set(false);
            pool.shutdown();
        }

        long duration = Math.max(1, System.currentTimeMillis()-startAt);

        System.out.println("\n== RESULT ==");
        metrics.values().stream()
                .map(m -> m.snapshot(duration))
                .forEach(r -> System.out.printf(
                        "[%s] success=%d error=%d RPS=%.1f p50=%.1fms p95=%.1fms p99=%.1fms%n",
                        r.name(), r.success(), r.error(), r.rps(), r.p50ms(), r.p95ms(), r.p99ms()
                ));

        // 간단 임계치 알림(과제 요구)
        var reports = metrics.values().stream().map(m -> m.snapshot(duration))
                .collect(java.util.stream.Collectors.toMap(PerfMetrics.Report::name, r -> r));
        thresholdHints(reports);
        System.out.println("== DONE ==");
    }

    private void thresholdHints(Map<String, PerfMetrics.Report> r) {
        var cursor = r.get("GET list (cursor)");
        if (cursor != null && cursor.p95ms() > 120.0)
            System.out.printf("WARN: Cursor 목록 p95=%.1fms (목표 < 120ms)\n", cursor.p95ms());

        var upload = r.get("POST upload");
        if (upload != null && upload.p95ms() > 300.0)
            System.out.printf("WARN: 업로드 p95=%.1fms (목표 < 300ms)\n", upload.p95ms());
    }
}
