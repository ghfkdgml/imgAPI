package com.project.imgapi.perf;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

public class ListCursorScenario implements Scenario {
    @Override public String name(){ return "GET list (cursor)"; }

    @Override
    public void runOnce(PerfProperties cfg, PerfMetrics m) throws Exception {
        String cursor = null;
        int chain = Math.max(1, cfg.cursorPageChain());
        long t0 = System.nanoTime();
        int status = 200;

        try {
            for (int i=0;i<chain;i++){
                String url = cfg.baseUrl() + "/projects/" + cfg.projectId() + "/images?mode=cursor&size=50" +
                        (cursor != null ? "&cursor="+cursor : "");
                var body = Http.get(url, cfg.authHeader());
                var jsonOpt = Http.parseJson(body);
                if (jsonOpt.isEmpty()){ status = 500; break; }
                var json = jsonOpt.get();
                var next = json.get("nextCursor");
                cursor = (next==null || next.isNull()) ? null : next.asText();
                if (cursor == null) break;
            }
        } catch (Exception e){
            System.out.println(e.getMessage());
            status = 599;
        }
        if (status==200) m.ok(System.nanoTime()-t0); else m.fail();
    }
}
