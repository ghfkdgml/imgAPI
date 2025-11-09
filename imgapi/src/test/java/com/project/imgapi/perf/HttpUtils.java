package com.project.imgapi.perf;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpUtils {
    public static HttpRequest.Builder reqBuilder(URI uri, String authHeader) {
        HttpRequest.Builder b = HttpRequest.newBuilder(uri);
        if (authHeader != null && !authHeader.isBlank()) {
            // "Authorization: Bearer xxx" or just "Bearer xxx"
            if (authHeader.contains(":")) {
                int i = authHeader.indexOf(':');
                b.header(authHeader.substring(0, i).trim(), authHeader.substring(i+1).trim());
            } else {
                b.header("Authorization", authHeader.trim());
            }
        }
        return b;
    }

    public static HttpRequest multipartPost(URI uri, String authHeader, String fieldName, String filename, String contentType, byte[] data) {
        final String boundary = "----PERFTEST_" + System.nanoTime();
        final var body = buildMultipart(boundary, fieldName, filename, contentType, data);
        return reqBuilder(uri, authHeader)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
    }

    private static byte[] buildMultipart(String boundary, String fieldName, String filename, String contentType, byte[] data) {
        String sep = "--" + boundary + "\r\n";
        String disp = "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"\r\n";
        String ct  = "Content-Type: " + contentType + "\r\n\r\n";
        String end = "\r\n--" + boundary + "--\r\n";

        byte[] head = (sep + disp + ct).getBytes(StandardCharsets.UTF_8);
        byte[] tail = end.getBytes(StandardCharsets.UTF_8);

        byte[] all = new byte[head.length + data.length + tail.length];
        System.arraycopy(head, 0, all, 0, head.length);
        System.arraycopy(data, 0, all, head.length, data.length);
        System.arraycopy(tail, 0, all, head.length + data.length, tail.length);
        return all;
    }

    public static HttpRequest get(URI uri, String authHeader) {
        return reqBuilder(uri, authHeader).GET().build();
    }

    public static HttpRequest patchJson(URI uri, String authHeader, String json) {
        return reqBuilder(uri, authHeader)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    public static HttpRequest delete(URI uri, String authHeader) {
        return reqBuilder(uri, authHeader).DELETE().build();
    }
}
