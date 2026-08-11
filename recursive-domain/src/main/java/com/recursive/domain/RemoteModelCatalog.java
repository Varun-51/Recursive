package com.recursive.domain;

import java.util.List;

/**
 * Port for listing models served by an OpenAI-compatible endpoint at
 * {@code /v1/models}. Implementations speak HTTP and own retry/timeout
 * policy; callers see plain domain models.
 */
public interface RemoteModelCatalog {

    List<ModelInfo> listModels(RemoteEndpoint endpoint);
}
