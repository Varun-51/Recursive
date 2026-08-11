package com.recursive.infrastructure.llm;

import com.recursive.domain.ModelInfo;
import com.recursive.domain.RemoteEndpoint;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleCatalogTest {

    private HttpServer server;
    private AtomicReference<String> authorizationHeader;
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        authorizationHeader = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/v1/models", exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{\"data\":[{\"id\":\"gpt-4o\"},{\"id\":\"llama-3.1\"}]}");
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void listsModelsWithoutKey() {
        OpenAiCompatibleCatalog catalog = new OpenAiCompatibleCatalog();

        List<ModelInfo> models = catalog.listModels(RemoteEndpoint.of("127.0.0.1", port));

        assertThat(models).extracting(ModelInfo::name).containsExactly("gpt-4o", "llama-3.1");
        assertThat(models).allSatisfy(model -> assertThat(model.installed()).isFalse());
        assertThat(authorizationHeader.get()).isNull();
    }

    @Test
    void sendsBearerTokenWhenKeyGiven() {
        OpenAiCompatibleCatalog catalog = new OpenAiCompatibleCatalog();

        catalog.listModels(new RemoteEndpoint("http://127.0.0.1:" + port + "/v1", "secret-key"));

        assertThat(authorizationHeader.get()).isEqualTo("Bearer secret-key");
    }

    @Test
    void failsLoudlyOnServerError() throws IOException {
        server.removeContext("/v1/models");
        server.createContext("/v1/models", exchange -> respond(exchange, 500, "boom"));
        OpenAiCompatibleCatalog catalog = new OpenAiCompatibleCatalog();

        assertThatThrownBy(() -> catalog.listModels(RemoteEndpoint.of("127.0.0.1", port)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("model catalog");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
