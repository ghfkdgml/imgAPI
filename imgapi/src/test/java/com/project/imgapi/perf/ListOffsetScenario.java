package com.project.imgapi.perf;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.ThreadLocalRandom;

public class ListOffsetScenario implements Scenario {
    @Override public String name(){ return "GET list (offset)"; }

    @Override
    public void runOnce(PerfProperties cfg, PerfMetrics m) {
        int pageMax = Math.max(1, cfg.offsetMaxPage());
        int page = ThreadLocalRandom.current().nextInt(pageMax);
        String url = cfg.baseUrl() + "/projects/" + cfg.projectId() +
                "/images?mode=offset&page=" + page + "&size=50";
        long t0 = System.nanoTime();
        try {
            Http.get(url, cfg.authHeader());
            m.ok(System.nanoTime()-t0);
        } catch (Exception e){
            System.out.println(e.getMessage());
            m.fail();
        }
    }
}