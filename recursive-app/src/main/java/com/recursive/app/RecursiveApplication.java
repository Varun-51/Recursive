package com.recursive.app;

import com.recursive.app.ui.MainWindow;
import com.recursive.infrastructure.appconfig.AppConfig;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX entry point. Builds the composition root once and hands the
 * resulting shell to the stage; closing the window closes the root.
 */
public class RecursiveApplication extends Application {

    private CompositionRoot root;

    @Override
    public void start(Stage stage) {
        root = CompositionRoot.build(AppConfig.fromDefaults());
        MainWindow window = new MainWindow(root);
        stage.setScene(new Scene(window.rootNode(), 1200, 800));
        stage.setTitle("Recursive");
        stage.show();
    }

    @Override
    public void stop() {
        if (root != null) {
            root.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
