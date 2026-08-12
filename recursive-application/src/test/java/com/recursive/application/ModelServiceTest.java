package com.recursive.application;

import com.recursive.domain.HardwareDetector;
import com.recursive.domain.HardwareSpec;
import com.recursive.domain.ModelInfo;
import com.recursive.domain.ModelProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ModelServiceTest {

    private static final HardwareSpec FITTING_HARDWARE =
            new HardwareSpec(32, 24, 8, "RTX 3060", 12288, 200, true);

    @Test
    void filtersModelsThatDoNotFitAvailableRam() {
        ModelProvider provider = provider(
                new ModelInfo("llama3.1:8b", 5, 8, false, true),
                new ModelInfo("mixtral:8x7b", 26, 48, false, true));
        ModelService service = new ModelService(provider, hardware(FITTING_HARDWARE));

        assertThat(service.listFittingModels())
                .extracting(ModelInfo::name)
                .containsExactly("llama3.1:8b");
    }

    @Test
    void excludesGpuModelsWhenHostHasNoGpu() {
        HardwareSpec noGpu = new HardwareSpec(32, 24, 8, null, 0, 200, true);
        ModelProvider provider = provider(
                new ModelInfo("nomic-embed-text", 0, 2, false, true),
                new ModelInfo("llava:13b", 9, 16, true, true));
        ModelService service = new ModelService(provider, hardware(noGpu));

        assertThat(service.listFittingModels())
                .extracting(ModelInfo::name)
                .containsExactly("nomic-embed-text");
    }

    @Test
    void recommendsFirstInstalledFittingModel() {
        ModelProvider provider = provider(
                new ModelInfo("llama3.1:8b", 5, 8, false, true),
                new ModelInfo("llama3.2:3b", 2, 4, false, false));
        ModelService service = new ModelService(provider, hardware(FITTING_HARDWARE));

        assertThat(service.recommendModel())
                .map(ModelInfo::name)
                .contains("llama3.1:8b");
    }

    @Test
    void canRunRequiresInstallationAndFit() {
        ModelProvider provider = provider(
                new ModelInfo("llama3.1:8b", 5, 8, false, true));
        ModelService service = new ModelService(provider, hardware(FITTING_HARDWARE));

        assertThat(service.canRun("llama3.1:8b")).isTrue();
        assertThat(service.canRun("mistral:7b")).isFalse();
    }

    private static ModelProvider provider(ModelInfo... models) {
        return new ModelProvider() {
            private final List<ModelInfo> installed = List.of(models);

            @Override
            public boolean isInstalled() {
                return true;
            }

            @Override
            public boolean isRunning() {
                return true;
            }

            @Override
            public void start(int parallelSlots) {
            }

            @Override
            public boolean waitUntilRunning(Duration timeout) {
                return true;
            }

            @Override
            public List<ModelInfo> listModels() {
                return installed;
            }

            @Override
            public void downloadModel(ModelInfo model, Consumer<String> progressListener) {
            }

            @Override
            public boolean isModelInstalled(String modelTag) {
                return installed.stream().anyMatch(m -> m.name().equals(modelTag));
            }
        };
    }

    private static HardwareDetector hardware(HardwareSpec spec) {
        return () -> spec;
    }
}
