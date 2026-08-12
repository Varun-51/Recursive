package com.recursive.app.ui;

import com.recursive.app.CompositionRoot;
import com.recursive.domain.HardwareSpec;
import com.recursive.infrastructure.llm.OllamaModelProvider;
import com.recursive.infrastructure.system.SystemMonitor;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Overview screen: host capabilities, Ollama state, and on-disk locations.
 * All probing runs off the FX thread; the grid is repopulated when the
 * probe returns.
 */
public final class DashboardScreen implements Screen {

    private final CompositionRoot root;
    private final GridPane grid = new GridPane();
    private final Map<String, Label> values = new LinkedHashMap<>();

    public DashboardScreen(CompositionRoot root) {
        this.root = root;
    }

    @Override
    public Node build() {
        addRow("RAM", spec -> spec.totalRamGb() + " GB total / " + spec.availableRamGb() + " GB available");
        addRow("CPU", spec -> spec.cpuCores() + " cores");
        addRow("GPU", spec -> spec.gpuModel() == null ? "none" : spec.gpuModel());
        addRow("Disk", spec -> spec.freeDiskGb() + " GB free");
        addRow("Ollama", ignored -> root.ollama().isInstalled() ? "installed" : "not installed");
        addRow("Ollama server", ignored -> root.ollama().isRunning() ? "running" : "stopped");
        addRow("Data root", ignored -> root.config().storagePaths().dataRoot().toString());
        addRow("Database", ignored -> root.config().databaseFile().toString());

        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> probe());

        VBox panel = new VBox(12, grid, refresh);
        panel.setPadding(new Insets(16));
        probe();
        return panel;
    }

    @Override
    public String title() {
        return "Dashboard";
    }

    private void addRow(String label, Function<HardwareSpec, String> formatter) {
        Label key = new Label(label);
        key.setStyle("-fx-font-weight: bold;");
        Label value = new Label("\u2026");
        int row = values.size();
        grid.addRow(row, key, value);
        values.put(label, value);
        formatters.put(label, formatter);
    }

    private final Map<String, Function<HardwareSpec, String>> formatters = new LinkedHashMap<>();

    private void probe() {
        BackgroundTasks.run("dashboard-probe", this::snapshot,
                this::render, error -> values.values().forEach(label -> label.setText("unavailable")));
    }

    private Map<String, String> snapshot() {
        SystemMonitor monitor = root.hardware();
        HardwareSpec spec = monitor.detect();
        OllamaModelProvider ollama = root.ollama();
        Map<String, String> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Function<HardwareSpec, String>> entry : formatters.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().apply(spec));
        }
        snapshot.put("Ollama", ollama.isInstalled() ? "installed" : "not installed");
        snapshot.put("Ollama server", ollama.isRunning() ? "running" : "stopped");
        return snapshot;
    }

    private void render(Map<String, String> snapshot) {
        snapshot.forEach((key, value) -> {
            Label label = values.get(key);
            if (label != null) {
                label.setText(value);
            }
        });
    }
}
