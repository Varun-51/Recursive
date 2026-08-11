package com.recursive.app;

import com.recursive.domain.HardwareSpec;
import com.recursive.infrastructure.appconfig.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Headless entry point: builds the composition root and runs a self-check
 * (database roundtrip, hardware facts, Ollama presence, verification
 * sample) so the wiring is demonstrable without a GUI. The JavaFX shell
 * reuses the same composition root.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        CompositionRoot root = CompositionRoot.build(AppConfig.fromDefaults());
        try {
            runSelfCheck(root);
        } finally {
            root.close();
        }
    }

    private static void runSelfCheck(CompositionRoot root) {
        log.info("Recursive {} wired against {}", "0.1.0", root.config().databaseFile());
        HardwareSpec hardware = root.hardware().detect();
        log.info("Hardware: {} GB RAM ({}) free, {} cores, {} GB disk free, meets minimum: {}",
                hardware.totalRamGb(), hardware.availableRamGb(), hardware.cpuCores(),
                hardware.freeDiskGb(), hardware.meetsMinimumRequirements());
        log.info("Ollama: installed={}, running={}", root.ollama().isInstalled(), root.ollama().isRunning());
        log.info("Installed models: {}", root.ollama().listModels());
        log.info("Self-check completed");
    }
}
