package com.recursive.application;

import com.recursive.domain.ModelInfo;
import com.recursive.domain.RemoteEndpoint;
import com.recursive.domain.RemoteModelCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteModelServicesTest {

    @Test
    void discoveryBuildsEndpointAndAsksCatalog() {
        RemoteModelCatalog catalog = endpoint -> List.of(
                new ModelInfo("qwen2.5:7b", 5, 8, false, true));
        RemoteModelDiscoveryService service = new RemoteModelDiscoveryService(catalog);

        List<ModelInfo> models = service.discover("localhost", 1234);

        assertThat(models).extracting(ModelInfo::name).containsExactly("qwen2.5:7b");
    }

    @Test
    void discoveryRejectsBadPort() {
        RemoteModelDiscoveryService service = new RemoteModelDiscoveryService(endpoint -> List.of());
        assertThatThrownBy(() -> service.discover("localhost", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.discover("localhost", 70000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void configuredEndpointServiceReportsReachability() {
        RemoteModelCatalog catalog = endpoint -> List.of(
                new ModelInfo("gpt-oss:20b", 12, 24, false, true));
        OpenAICompatibleModelService service = new OpenAICompatibleModelService(catalog);

        assertThat(service.isReachable(RemoteEndpoint.of("192.168.1.5", 8080))).isTrue();
        assertThat(service.availableModels(RemoteEndpoint.of("192.168.1.5", 8080))).hasSize(1);
    }

    @Test
    void emptyCatalogMeansUnreachable() {
        OpenAICompatibleModelService service = new OpenAICompatibleModelService(endpoint -> List.of());
        assertThat(service.isReachable(RemoteEndpoint.of("192.168.1.5", 8080))).isFalse();
    }

    @Test
    void endpointRejectsNonHttpUrls() {
        assertThatThrownBy(() -> new RemoteEndpoint("ftp://x", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RemoteEndpoint(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}