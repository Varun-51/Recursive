package com.recursive.application;

import com.recursive.domain.ModelInfo;
import com.recursive.domain.RemoteEndpoint;
import com.recursive.domain.RemoteModelCatalog;

import java.util.List;

/**
 * Convenience entry point for host/port based discovery: builds the
 * endpoint from the two fields the model-connection screen collects, then
 * asks the catalog what the server serves at /v1/models.
 */
public class RemoteModelDiscoveryService {

    private final RemoteModelCatalog catalog;

    public RemoteModelDiscoveryService(RemoteModelCatalog catalog) {
        this.catalog = catalog;
    }

    public List<ModelInfo> discover(String host, int port) {
        return catalog.listModels(RemoteEndpoint.of(host, port));
    }
}
