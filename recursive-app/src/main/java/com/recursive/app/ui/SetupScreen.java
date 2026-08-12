package com.recursive.app.ui;

import com.recursive.app.CompositionRoot;
import com.recursive.domain.HardwareSpec;
import com.recursive.domain.ModelInfo;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.util.Optional;

/**
 * First-run guide: checks the hardware, starts the local Ollama server when
 * it is installed but stopped, and pulls the recommended model when it is
 * missing. Every step degrades to a visible status message, never a crash.
 */
public final class SetupScreen implements Screen {

    private static final Duration SERVER_TIMEOUT = Duration.ofSeconds(10);

    private final CompositionRoot root;
    private final Label hardwareLabel = new Label("Detecting\u2026");
    private final Label ollamaStatus = new Label();
    private final Button startServer = new Button("Start Ollama");
    private final Label modelLabel = new Label();
    private final Button pullModel = new Button("Pull recommended model");
    private final Label progress = new Label();
    private final Label message = new Label();

    private Optional<ModelInfo> recommendation = Optional.empty();

    public SetupScreen(CompositionRoot root) {
        this.root = root;
    }

    @Override
    public Node build() {
        startServer.setDisable(true);
        startServer.setOnAction(event -> startOllama());
        pullModel.setDisable(true);
        pullModel.setOnAction(event -> pullRecommended());

        HBox serverRow = new HBox(8, ollamaStatus, startServer);
        HBox modelRow = new HBox(8, modelLabel, pullModel);

        GridPane facts = new GridPane();
        facts.setHgap(12);
        facts.setVgap(6);
        facts.addRow(0, new Label("Hardware"), hardwareLabel);

        VBox panel = new VBox(12,
                new Label("First-run setup"),
                facts,
                new Separator(),
                new Label("Ollama server"), serverRow,
                new Separator(),
                new Label("Translation model"), modelRow, progress,
                message);
        panel.setPadding(new Insets(16));

        refresh();
        return panel;
    }

    @Override
    public String title() {
        return "Setup";
    }

    private void refresh() {
        BackgroundTasks.run("setup-hardware", root.hardware()::detect,
                this::showHardware,
                error -> hardwareLabel.setText("Could not detect hardware: " + error.getMessage()));
        BackgroundTasks.run("setup-server", () -> root.ollama().isInstalled(),
                installed -> {
                    if (installed) {
                        checkServerRunning();
                    } else {
                        ollamaStatus.setText("Ollama is not installed");
                        startServer.setDisable(true);
                    }
                },
                error -> ollamaStatus.setText("Could not check Ollama: " + error.getMessage()));
        loadRecommendation();
    }

    private void showHardware(HardwareSpec hardware) {
        hardwareLabel.setText(hardware.totalRamGb() + " GB RAM (" + hardware.availableRamGb()
                + " GB available), " + hardware.cpuCores() + " cores");
    }

    private void checkServerRunning() {
        BackgroundTasks.run("setup-server-check", root.ollama()::isRunning,
                running -> {
                    ollamaStatus.setText(running ? "Ollama is running" : "Ollama is installed but stopped");
                    startServer.setDisable(running);
                },
                error -> ollamaStatus.setText("Could not check Ollama: " + error.getMessage()));
    }

    private void startOllama() {
        startServer.setDisable(true);
        ollamaStatus.setText("Starting\u2026");
        BackgroundTasks.run("setup-server-start", () -> {
                    root.ollama().start();
                    return root.ollama().waitUntilRunning(SERVER_TIMEOUT);
                },
                running -> {
                    ollamaStatus.setText(running ? "Ollama is running" : "Server did not answer within 10 seconds");
                    startServer.setDisable(running);
                    loadRecommendation();
                },
                error -> {
                    ollamaStatus.setText("Could not start Ollama: " + error.getMessage());
                    startServer.setDisable(false);
                });
    }

    private void loadRecommendation() {
        BackgroundTasks.run("setup-recommend", root.modelService()::recommendModel,
                found -> {
                    recommendation = found;
                    if (found.isPresent()) {
                        ModelInfo model = found.get();
                        modelLabel.setText("Recommended: " + model.name() + " (" + model.sizeGb() + " GB)");
                        checkInstalled(model);
                    } else {
                        modelLabel.setText("No model fits this hardware");
                        pullModel.setDisable(true);
                    }
                },
                error -> modelLabel.setText("Could not recommend a model: " + error.getMessage()));
    }

    private void checkInstalled(ModelInfo model) {
        BackgroundTasks.run("setup-model-check", () -> root.ollama().isModelInstalled(model.name()),
                installed -> {
                    pullModel.setDisable(installed);
                    pullModel.setText(installed ? "Model installed" : "Pull recommended model");
                },
                error -> pullModel.setDisable(true));
    }

    private void pullRecommended() {
        ModelInfo model = recommendation.orElse(null);
        if (model == null) {
            return;
        }
        pullModel.setDisable(true);
        progress.setText("Pulling " + model.name() + "\u2026");
        BackgroundTasks.run("setup-model-pull",
                () -> {
                    root.ollama().downloadModel(model, percent ->
                            Platform.runLater(() -> progress.setText("Pulling " + model.name() + ": " + percent)));
                    return model.name();
                },
                name -> {
                    progress.setText("Installed " + name);
                    message.setText("Everything is ready \u2014 create a job in the Jobs screen.");
                    checkInstalled(model);
                },
                error -> {
                    progress.setText("Pull failed: " + error.getMessage());
                    pullModel.setDisable(false);
                });
    }
}
