package com.recursive.application;

import com.recursive.domain.HardwareDetector;
import com.recursive.domain.HardwareSpec;
import com.recursive.domain.ModelInfo;
import com.recursive.domain.ModelProvider;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Decides which installed models are safe to run on this machine. A model
 * fits when its RAM requirement fits the available RAM and, when it needs a
 * GPU, the host reports one.
 */
public class ModelService {

    private final ModelProvider modelProvider;
    private final HardwareDetector hardwareDetector;

    public ModelService(ModelProvider modelProvider, HardwareDetector hardwareDetector) {
        this.modelProvider = modelProvider;
        this.hardwareDetector = hardwareDetector;
    }

    public List<ModelInfo> listFittingModels() {
        HardwareSpec hardware = hardwareDetector.detect();
        return modelProvider.listModels().stream()
                .filter(model -> fitsOn(model, hardware))
                .collect(Collectors.toList());
    }

    public Optional<ModelInfo> recommendModel() {
        return listFittingModels().stream()
                .filter(ModelInfo::installed)
                .findFirst();
    }

    public boolean canRun(String modelTag) {
        if (!modelProvider.isInstalled() || !modelProvider.isModelInstalled(modelTag)) {
            return false;
        }
        HardwareSpec hardware = hardwareDetector.detect();
        return modelProvider.listModels().stream()
                .filter(model -> model.name().equals(modelTag))
                .anyMatch(model -> fitsOn(model, hardware));
    }

    private static boolean fitsOn(ModelInfo model, HardwareSpec hardware) {
        boolean ramFits = model.ramRequiredGb() <= hardware.availableRamGb();
        boolean gpuFits = !model.gpuRequired()
                || (hardware.gpuModel() != null && hardware.gpuVramMb() > 0);
        return ramFits && gpuFits;
    }
}
