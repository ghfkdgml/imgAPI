package com.project.imgapi.perf;

import java.util.concurrent.ThreadLocalRandom;

public class UploadScenario implements Scenario {
    @Override public String name(){ return "POST upload"; }

    @Override
    public void runOnce(PerfProperties cfg, PerfMetrics m) throws Exception {
        var sizes = cfg.uploadKb();
        int kb = sizes.get(ThreadLocalRandom.current().nextInt(sizes.size()));
        // 유효한 JPEG 생성
        byte[] img = ValidateImageFactory.jpeg(320, 240);

        long t0 = System.nanoTime();
        try {
            String url = cfg.baseUrl() + "/projects/" + cfg.projectId() + "/images";
            Http.postMultipart(url, "files", "perf.jpg", img, "image/jpeg", cfg.authHeader());
            m.ok(System.nanoTime() - t0);
        } catch (Exception e){
            m.fail();
        }
    }
}