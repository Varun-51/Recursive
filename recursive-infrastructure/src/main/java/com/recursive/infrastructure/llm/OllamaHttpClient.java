package com.recursive.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Thin HTTP client for the local Ollama API ({@code /api/tags},
 * {@code /api/pull}, {@code /api/generate}). Owns JSON marshalling and the
 * request/response lifecycle; higher-level policy lives in
 * {@link OllamaModelProvider}.
 */
public class OllamaHttpClient {

    public record ModelTag(String name, long sizeBytes) {
    }

    private final URI baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaHttpClient(String baseUrl) {
        this.baseUrl = URI.create(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.objectMapper = new ObjectMapper();
    }

    public static OllamaHttpClient local() {
        return new OllamaHttpClient("http://localhost:11434");
    }

    public boolean isReachable() {
        try {
            return send("api/tags", "GET", "") != null;
        } catch (IOException e) {
            return false;
        }
    }

    public List<ModelTag> listTags() throws IOException {
        JsonNode body = parse(send("api/tags", "GET", ""));
        List<ModelTag> tags = new ArrayList<>();
        if (body.has("models")) {
            for (JsonNode model : body.get("models")) {
                tags.add(new ModelTag(model.get("name").asText(),
                        model.has("size") ? model.get("size").asLong() : 0L));
            }
        }
        return tags;
    }

    /**
     * Pulls a model, feeding download progress percentages to the listener
     * as the server reports them.
     */
    public void pull(String modelTag, Consumer<Double> progressListener) throws IOException {
        String request = "{\"name\":\"" + modelTag + "\",\"stream\":true}";
        HttpResponse<java.io.InputStream> stream;
        try {
            stream = httpClient.send(build("api/pull", "POST", request),
                    HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while pulling " + modelTag, e);
        }
        if (stream.statusCode() != 200) {
            throw new IOException("Ollama pull failed with HTTP " + stream.statusCode());
        }
        try (java.io.InputStream body = stream.body()) {
            byte[] buffer = new byte[4096];
            StringBuilder line = new StringBuilder();
            int read = body.read(buffer);
            while (read != -1) {
                line.append(new String(buffer, 0, read, java.nio.charset.StandardCharsets.UTF_8));
                processLines(line, progressListener);
                read = body.read(buffer);
            }
        }
    }

    public String generate(String modelTag, String prompt) throws IOException {
        JsonNode body = objectMapper.readTree(send("api/generate", "POST",
                "{\"model\":\"" + modelTag + "\",\"prompt\":" + objectMapper.writeValueAsString(prompt)
                        + ",\"stream\":false}"));
        return body.has("response") ? body.get("response").asText() : "";
    }

    private void processLines(StringBuilder pending, Consumer<Double> progressListener) {
        String content = pending.toString();
        int newline = content.indexOf('\n');
        while (newline != -1) {
            String jsonLine = content.substring(0, newline);
            pending.delete(0, newline + 1);
            content = pending.toString();
            try {
                JsonNode event = objectMapper.readTree(jsonLine);
                if (event.has("completed") && event.has("total") && event.get("total").asLong() > 0) {
                    progressListener.accept((double) event.get("completed").asLong()
                            / event.get("total").asLong());
                }
            } catch (IOException e) {
                throw new IllegalStateException("Malformed pull event from Ollama", e);
            }
            newline = content.indexOf('\n');
        }
    }

    private JsonNode parse(String json) throws IOException {
        return json == null ? null : objectMapper.readTree(json);
    }

    private String send(String path, String method, String body) throws IOException {
        try {
            HttpResponse<String> response = httpClient.send(build(path, method, body),
                    HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? response.body() : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while talking to Ollama", e);
        }
    }

    private HttpRequest build(String path, String method, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(baseUrl.resolve(path))
                .timeout(Duration.ofMinutes(10));
        if ("POST".equals(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/json");
        }
        return builder.build();
    }
}
