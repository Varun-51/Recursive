package com.recursive.app.ui;

import com.recursive.app.CompositionRoot;
import com.recursive.application.RecursionSettings;
import com.recursive.application.VersionService;
import com.recursive.domain.Confidence;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Settings screen: storage locations (read-only), the recursion policy of
 * the verification loop, and version information.
 */
public final class SettingsScreen implements Screen {

    private final CompositionRoot root;
    private final ObjectProperty<RecursionSettings> recursion;
    private final Label status = new Label();

    public SettingsScreen(CompositionRoot root, ObjectProperty<RecursionSettings> recursion) {
        this.root = root;
        this.recursion = recursion;
    }

    @Override
    public Node build() {
        RecursionSettings current = recursion.get();
        Spinner<Integer> maxDepth = spinner(current.maxDepth(), 1, 10);
        Spinner<Integer> minStablePasses = spinner(current.minStablePasses(), 1, 5);
        TextField confidence = new TextField(String.valueOf(current.confidenceThreshold().asPercent()));
        CheckBox enabled = new CheckBox("Recursive verification enabled");
        enabled.setSelected(current.enabled());

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.addRow(0, new Label("Max retry depth"), maxDepth);
        form.addRow(1, new Label("Stable passes"), minStablePasses);
        form.addRow(2, new Label("Confidence threshold %"), confidence);
        form.addRow(3, new Label(), enabled);

        Button save = new Button("Save recursion settings");
        save.setOnAction(event -> save(maxDepth, minStablePasses, confidence, enabled));

        VBox recursionPanel = new VBox(10,
                new Label("Verification loop"),
                form, new HBox(8, save, status));

        VersionService version = VersionService.defaults();
        GridPane about = new GridPane();
        about.setHgap(12);
        about.setVgap(6);
        about.addRow(0, new Label("Application"), new Label(version.appVersion()));
        about.addRow(1, new Label("Model interface"), new Label(version.modelVersion()));
        about.addRow(2, new Label("Developer"), new Label(version.developerVersion()));

        GridPane storage = new GridPane();
        storage.setHgap(12);
        storage.setVgap(6);
        String analysisRoot = root.config().storagePaths().analysisRoot().toString();
        storage.addRow(0, new Label("Data root"), new Label(root.config().storagePaths().dataRoot().toString()));
        storage.addRow(1, new Label("Dev root"), new Label(root.config().storagePaths().devRoot().toString()));
        storage.addRow(2, new Label("Analysis root"), new Label(analysisRoot));

        VBox panel = new VBox(14,
                new Label("Recursion settings"), recursionPanel,
                new Separator(),
                new Label("Storage"), storage,
                new Separator(),
                new Label("About"), about);
        panel.setPadding(new Insets(16));
        return panel;
    }

    @Override
    public String title() {
        return "Settings";
    }

    private static Spinner<Integer> spinner(int value, int min, int max) {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, value));
        return spinner;
    }

    private void save(Spinner<Integer> maxDepth, Spinner<Integer> minStablePasses,
                      TextField confidence, CheckBox enabled) {
        try {
            double percent = Double.parseDouble(confidence.getText().trim());
            RecursionSettings updated = new RecursionSettings(
                    maxDepth.getValue(), minStablePasses.getValue(),
                    Confidence.fromPercent(percent), enabled.isSelected());
            recursion.set(updated);
            status.setText("Saved");
        } catch (IllegalArgumentException e) {
            status.setText("Invalid input: " + e.getMessage());
        }
    }
}
