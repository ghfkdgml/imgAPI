package com.project.imgapi.perf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.ByteArrayBody;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Http {
    public static CloseableHttpClient client(){ return HttpClientHolder.INSTANCE; }
    private static class HttpClientHolder {
        static final CloseableHttpClient INSTANCE =
                org.apache.hc.client5.http.impl.classic.HttpClients.custom()
                        .setDefaultRequestConfig(RequestConfig.custom()
                                .setConnectTimeout(Timeout.of(10, TimeUnit.SECONDS))
                                .build())
                        .disableAutomaticRetries()
                        .build();
    }

    public static void setAuthHeader(org.apache.hc.core5.http.HttpMessage req, String authHeader){
        if (authHeader != null && !authHeader.isBlank()){
            String[] kv = authHeader.split(":", 2);
            if (kv.length == 2) req.addHeader(kv[0].trim(), kv[1].trim());
            else req.addHeader("Authorization", authHeader.trim());
        }
        req.addHeader("Accept", "application/json");
    }

    public static String postMultipart(String url, String fieldName, String filename, byte[] bytes, String contentType, String authHeader) throws Exception {
        HttpPost post = new HttpPost(url);
        setAuthHeader(post, authHeader);
        MultipartEntityBuilder mb = MultipartEntityBuilder.create()
                .setCharset(StandardCharsets.UTF_8)
                .setMode(HttpMultipartMode.STRICT)
                .addPart(fieldName, new ByteArrayBody(bytes, ContentType.parse(contentType), filename));
        post.setEntity(mb.build());
        return client().execute(post, resp -> EntityUtils.toString(resp.getEntity()));
    }

    public static String get(String url, String authHeader) throws Exception {
        HttpGet get = new HttpGet(url);
        setAuthHeader(get, authHeader);
        return client().execute(get, resp -> EntityUtils.toString(resp.getEntity()));
    }

    public static Optional<JsonNode> parseJson(String s){
        try {
            return Optional.of(MapperHolder.OM.readTree(s));
        } catch (Exception ignored){ return Optional.empty(); }
    }
    private static class MapperHolder { static final ObjectMapper OM = new ObjectMapper(); }
}
