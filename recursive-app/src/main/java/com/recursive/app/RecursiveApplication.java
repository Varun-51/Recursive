package com.recursive.app;

import com.recursive.domain.HardwareSpec;
import com.recursive.infrastructure.appconfig.AppConfig;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX shell. Phase 1 shows the wiring status and host facts; the full
 * screen set (jobs, model selection, review) replaces this in Phase 2.
 */
public class RecursiveApplication extends Application {

    @Override
    public void start(Stage stage) {
        CompositionRoot root = CompositionRoot.build(AppConfig.fromDefaults());
        HardwareSpec hardware = root.hardware().detect();

        VBox panel = new VBox(10);
        panel.getChildren().add(new Label("Recursive — Phase 1 composition OK"));
        panel.getChildren().add(new Label("RAM: " + hardware.totalRamGb() + " GB ("
                + hardware.availableRamGb() + " GB available), " + hardware.cpuCores() + " cores"));
        panel.getChildren().add(new Label("Ollama installed: " + root.ollama().isInstalled()
                + ", running: " + root.ollama().isRunning()));

        stage.setScene(new Scene(panel, 640, 240));
        stage.setTitle("Recursive");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
