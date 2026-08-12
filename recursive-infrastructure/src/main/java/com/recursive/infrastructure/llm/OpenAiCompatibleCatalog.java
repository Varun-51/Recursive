package com.recursive.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recursive.domain.ModelInfo;
import com.recursive.domain.RemoteEndpoint;
import com.recursive.domain.RemoteModelCatalog;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link RemoteModelCatalog} implementation for OpenAI-compatible servers.
 * The API key, when present, is sent as a bearer token and never logged.
 */
public class OpenAiCompatibleCatalog implements RemoteModelCatalog {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleCatalog() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<ModelInfo> listModels(RemoteEndpoint endpoint) {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(modelsUrl(endpoint.baseUrl())))
                .timeout(Duration.ofSeconds(30))
                .GET();
        if (endpoint.apiKey() != null) {
            request.header("Authorization", "Bearer " + endpoint.apiKey());
        }
        try {
            HttpResponse<String> response = httpClient.send(request.build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("model catalog responded with HTTP "
                        + response.statusCode());
            }
            return parseModels(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reaching model catalog at "
                    + endpoint.baseUrl(), e);
        } catch (IOException e) {
            throw new IllegalStateException("Could not reach model catalog at "
                    + endpoint.baseUrl(), e);
        }
    }

    private List<ModelInfo> parseModels(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        List<ModelInfo> models = new ArrayList<>();
        if (root.has("data")) {
            for (JsonNode entry : root.get("data")) {
                models.add(new ModelInfo(entry.get("id").asText(), 0L, 0L, false, false));
            }
        }
        return models;
    }

    private static String modelsUrl(String baseUrl) {
        String normalized = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        if (normalized.endsWith("v1/")) {
            return normalized + "models";
        }
        return normalized + "v1/models";
    }
}
