package com.recursive.domain;

/**
 * An Ollama model considered for translation duty.
 *
 * @param name            model tag, e.g. {@code llama3.1:8b}
 * @param sizeGb          download size
 * @param ramRequiredGb   recommended RAM for comfortable inference
 * @param gpuRequired     true when GPU acceleration is effectively required
 * @param installed       whether {@code ollama list} reported it locally
 */
public record ModelInfo(
        String name,
        long sizeGb,
        long ramRequiredGb,
        boolean gpuRequired,
        boolean installed) {

    public ModelInfo {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Model name must not be blank");
        }
    }
}
