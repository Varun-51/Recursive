package com.recursive.domain;

/**
 * Address of an OpenAI-compatible remote endpoint (Ollama remote, LM
 * Studio, vLLM, ...). {@code apiKey} stays nullable: local servers run
 * without authentication, and null means "no key to send".
 */
public record RemoteEndpoint(String baseUrl, String apiKey) {

    public RemoteEndpoint {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new IllegalArgumentException("baseUrl must be http(s): " + baseUrl);
        }
    }

    public static RemoteEndpoint of(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be in [1, 65535]");
        }
        return new RemoteEndpoint("http://" + host + ":" + port, null);
    }
}
