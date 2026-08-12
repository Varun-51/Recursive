package com.recursive.app.ui;

import com.recursive.app.CompositionRoot;
import com.recursive.domain.ModelInfo;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Model management: locally available Ollama models (with a recommendation
 * for the current hardware) and remote OpenAI-compatible catalogs.
 */
public final class ModelsScreen implements Screen {

    private final CompositionRoot root;
    private final Label recommendation = new Label("No recommendation yet");
    private final TableView<ModelInfo> localModels = new TableView<>();
    private final TableView<ModelInfo> remoteModels = new TableView<>();
    private final Label remoteStatus = new Label();
    private final TextField host = new TextField("localhost");
    private final TextField port = new TextField("11434");
    private final TextField baseUrl = new TextField();
    private final PasswordField apiKey = new PasswordField();

    public ModelsScreen(CompositionRoot root) {
        this.root = root;
    }

    @Override
    public Node build() {
        localModels.getColumns().add(TableColumns.string("Model", 200, ModelInfo::name));
        localModels.getColumns().add(TableColumns.string("Size GB", 80,
                model -> String.valueOf(model.sizeGb())));
        localModels.getColumns().add(TableColumns.string("RAM GB", 80,
                model -> String.valueOf(model.ramRequiredGb())));
        localModels.getColumns().add(TableColumns.string("Installed", 90,
                model -> model.installed() ? "yes" : "no"));
        localModels.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(localModels, Priority.ALWAYS);

        remoteModels.getColumns().add(TableColumns.string("Model", 260, ModelInfo::name));
        remoteModels.getColumns().add(TableColumns.string("RAM GB", 80,
                model -> String.valueOf(model.ramRequiredGb())));
        remoteModels.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(remoteModels, Priority.ALWAYS);

        Button refresh = new Button("Refresh local models");
        refresh.setOnAction(event -> loadLocal());

        VBox localPanel = new VBox(8,
                new Label("Local Ollama models"), recommendation,
                new HBox(8, refresh), localModels);

        host.setPrefWidth(140);
        port.setPrefWidth(90);
        Button discover = new Button("Discover");
        discover.setOnAction(event -> discoverRemote());
        GridPane endpointForm = new GridPane();
        endpointForm.setHgap(8);
        endpointForm.setVgap(8);
        endpointForm.addRow(0, new Label("Host"), host, new Label("Port"), port, discover);
        endpointForm.addRow(1, new Label("Base URL"), baseUrl);
        endpointForm.addRow(2, new Label("API key"), apiKey);
        Button check = new Button("Check reachability");
        check.setOnAction(event -> checkReachability());
        Button list = new Button("List remote models");
        list.setOnAction(event -> listRemoteModels());

        VBox remotePanel = new VBox(8,
                new Label("Remote OpenAI-compatible catalog"),
                endpointForm, new HBox(8, check, list), remoteStatus, remoteModels);

        VBox panel = new VBox(12, localPanel, new Separator(), remotePanel);
        panel.setPadding(new Insets(16));

        loadLocal();
        return panel;
    }

    @Override
    public String title() {
        return "Models";
    }

    private void loadLocal() {
        BackgroundTasks.run("models-local", root.modelService()::listFittingModels,
                models -> {
                    localModels.setItems(FXCollections.observableArrayList(models));
                    recommendation.setText(models.size() + " model(s) fit this hardware");
                },
                error -> recommendation.setText("Could not load models: " + error.getMessage()));
    }

    private void discoverRemote() {
        remoteStatus.setText("Discovering\u2026");
        BackgroundTasks.run("models-discover",
                () -> root.remoteModelDiscovery().discover(host.getText(), parsePort()),
                models -> {
                    remoteModels.setItems(FXCollections.observableArrayList(models));
                    remoteStatus.setText(models.size() + " model(s) found");
                },
                error -> remoteStatus.setText("Discovery failed: " + error.getMessage()));
    }

    private void checkReachability() {
        BackgroundTasks.run("models-check",
                () -> root.openAiCompatibleModelService().isReachable(endpoint()),
                reachable -> remoteStatus.setText(reachable ? "Reachable" : "Not reachable"),
                error -> remoteStatus.setText("Check failed: " + error.getMessage()));
    }

    private void listRemoteModels() {
        BackgroundTasks.run("models-remote", () -> root.openAiCompatibleModelService().availableModels(endpoint()),
                models -> {
                    remoteModels.setItems(FXCollections.observableArrayList(models));
                    remoteStatus.setText(models.size() + " model(s) listed");
                },
                error -> remoteStatus.setText("Listing failed: " + error.getMessage()));
    }

    private int parsePort() {
        try {
            return Integer.parseInt(port.getText().trim());
        } catch (NumberFormatException e) {
            remoteStatus.setText("Port must be a number");
            throw new IllegalArgumentException("Port must be a number");
        }
    }

    private com.recursive.domain.RemoteEndpoint endpoint() {
        String url = baseUrl.getText().isBlank()
                ? "http://" + host.getText() + ":" + port.getText()
                : baseUrl.getText();
        String key = apiKey.getText().isBlank() ? null : apiKey.getText();
        return new com.recursive.domain.RemoteEndpoint(url, key);
    }
}
