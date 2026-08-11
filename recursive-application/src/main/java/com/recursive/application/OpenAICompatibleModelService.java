package com.recursive.application;

import com.recursive.domain.ModelInfo;
import com.recursive.domain.RemoteEndpoint;
import com.recursive.domain.RemoteModelCatalog;

import java.util.List;

/**
 * Uses a configured OpenAI-compatible endpoint: lists what it serves and
 * whether it answers at all. Reachability is decided by the catalog port's
 * answer, never by local heuristics.
 */
public class OpenAICompatibleModelService {

    private final RemoteModelCatalog catalog;

    public OpenAICompatibleModelService(RemoteModelCatalog catalog) {
        this.catalog = catalog;
    }

    public List<ModelInfo> availableModels(RemoteEndpoint endpoint) {
        return catalog.listModels(endpoint);
    }

    public boolean isReachable(RemoteEndpoint endpoint) {
        return !catalog.listModels(endpoint).isEmpty();
    }
}
