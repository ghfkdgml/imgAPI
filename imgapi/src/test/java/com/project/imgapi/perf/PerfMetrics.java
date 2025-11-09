package com.project.imgapi.perf;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;

public class PerfMetrics {
    private final String name;
    private final List<Long> latenciesNanos = Collections.synchronizedList(new ArrayList<>());
    private final LongAdder success = new LongAdder();
    private final LongAdder error = new LongAdder();

    public PerfMetrics(String name) { this.name = name; }

    public void ok(long nanos){ latenciesNanos.add(nanos); success.increment(); }
    public void fail(){ error.increment(); }

    public Report snapshot(long durationMillis){
        List<Long> copy;
        synchronized (latenciesNanos){ copy = new ArrayList<>(latenciesNanos); }
        copy.sort(Long::compare);
        long s = success.sum();
        long e = error.sum();
        double rps = s * 1000.0 / Math.max(1, durationMillis);

        long p50 = pct(copy, 0.50);
        long p95 = pct(copy, 0.95);
        long p99 = pct(copy, 0.99);

        return new Report(name, s, e, rps, toMs(p50), toMs(p95), toMs(p99));
    }

    private static double toMs(long nanos){ return nanos <= 0 ? 0 : nanos / 1_000_000.0; }

    private static long pct(List<Long> sorted, double p){
        if (sorted.isEmpty()) return 0;
        int idx = (int)Math.ceil(p * sorted.size()) - 1;
        idx = Math.max(0, Math.min(idx, sorted.size()-1));
        return sorted.get(idx);
    }

    public record Report(String name, long success, long error, double rps,
                         double p50ms, double p95ms, double p99ms) {}
}
