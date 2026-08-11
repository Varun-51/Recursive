package com.recursive.infrastructure.llm;

import com.recursive.domain.Language;
import com.recursive.domain.ModelInfo;
import com.recursive.domain.ModelProvider;
import com.recursive.domain.ProcessingContext;
import com.recursive.domain.TranslationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * {@link ModelProvider} and {@link TranslationEngine} backed by the local
 * Ollama server. Process lifecycle (detect, start, wait) talks to the CLI;
 * model listing, pulls, and translation talk to the HTTP API.
 */
public class OllamaModelProvider implements ModelProvider, TranslationEngine {

    private static final Logger log = LoggerFactory.getLogger(OllamaModelProvider.class);
    private static final long BYTES_PER_GB = 1024L * 1024L * 1024L;

    private final OllamaHttpClient client;
    private final String baseUrl;

    public OllamaModelProvider(OllamaHttpClient client) {
        this.client = client;
        this.baseUrl = "http://localhost:11434";
    }

    public static OllamaModelProvider local() {
        return new OllamaModelProvider(OllamaHttpClient.local());
    }

    @Override
    public boolean isInstalled() {
        return client.isReachable() || cliExists();
    }

    @Override
    public boolean isRunning() {
        return client.isReachable();
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        try {
            Process server = new ProcessBuilder("ollama", "serve")
                    .redirectErrorStream(true).start();
            log.info("Started Ollama server process {}", server.pid());
        } catch (IOException e) {
            throw new IllegalStateException("Could not start the Ollama server", e);
        }
    }

    @Override
    public boolean waitUntilRunning(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (isRunning()) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @Override
    public List<ModelInfo> listModels() {
        try {
            return client.listTags().stream()
                    .map(tag -> new ModelInfo(tag.name(), tag.sizeBytes() / BYTES_PER_GB,
                            0L, false, true))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Could not list installed models", e);
        }
    }

    @Override
    public void downloadModel(ModelInfo model, Consumer<String> progressListener) {
        try {
            client.pull(model.name(), percent -> progressListener.accept(Math.round(percent * 100) + "%"));
        } catch (IOException e) {
            throw new IllegalStateException("Could not pull model " + model.name(), e);
        }
    }

    @Override
    public boolean isModelInstalled(String modelTag) {
        return listModels().stream().anyMatch(model -> model.name().equals(modelTag));
    }

    @Override
    public Optional<String> translate(String text, Language source, Language target,
                                      ProcessingContext context, String modelName) {
        String prompt = PromptBuilder.translate(text, source.code(), target.code(), context);
        try {
            String response = client.generate(modelName, prompt).trim();
            return response.isEmpty() ? Optional.empty() : Optional.of(response);
        } catch (IOException e) {
            log.warn("Translation via Ollama failed for model {}", modelName, e);
            return Optional.empty();
        }
    }

    private static boolean cliExists() {
        try {
            Process probe = new ProcessBuilder("ollama", "--version").start();
            probe.getInputStream().readAllBytes();
            probe.waitFor();
            return probe.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}
