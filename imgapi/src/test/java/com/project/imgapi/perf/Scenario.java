package com.project.imgapi.perf;

import java.net.URI;
import java.net.http.HttpClient;

public interface Scenario {
    String name();
    void runOnce(PerfProperties cfg, PerfMetrics metrics) throws Exception;
}