package com.recursive.domain;

import java.util.List;

/**
 * Port for everything Ollama-flavored: process presence, running state,
 * installed models, and model pulls. Implementations wrap the CLI and the
 * local HTTP API; callers never see process plumbing.
 */
public interface ModelProvider {

    boolean isInstalled();

    boolean isRunning();

    /**
     * Starts the Ollama server process. Returns immediately; readiness is
     * probed with {@link #waitUntilRunning(java.time.Duration)}.
     */
    void start();

    boolean waitUntilRunning(java.time.Duration timeout);

    List<ModelInfo> listModels();

    /**
     * Pulls a model in-process. {@code progressListener} receives progress
     * strings (percentages when the CLI reports them); it may be called
     * concurrently from the pull thread.
     */
    void downloadModel(ModelInfo model, java.util.function.Consumer<String> progressListener);

    boolean isModelInstalled(String modelTag);
}
