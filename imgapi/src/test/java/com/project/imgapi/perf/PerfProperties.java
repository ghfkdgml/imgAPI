package com.project.imgapi.perf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "perf")
public record PerfProperties(
        String baseUrl,
        long projectId,
        String authHeader,
        int concurrency,
        int durationSeconds,
        List<Integer> uploadKb,
        double uploadDuplicateRate,
        int cursorPageChain,
        int offsetMaxPage,
        int presignExpirySec
) {}
