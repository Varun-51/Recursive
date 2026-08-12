package com.recursive.app.ui;

import com.recursive.app.CompositionRoot;
import com.recursive.application.RecursionSettings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Application shell: a sidebar of navigation entries on the left, the
 * active screen in the center, and shared state (recursion settings) that
 * several screens read. Screens are built lazily on first selection.
 */
public final class MainWindow {

    private final CompositionRoot root;
    private final ObjectProperty<RecursionSettings> recursion =
            new SimpleObjectProperty<>(RecursionSettings.defaults());
    private final Map<String, Screen> screens = new LinkedHashMap<>();
    private final Map<String, Node> built = new LinkedHashMap<>();
    private final StackPane content = new StackPane();
    private final Label header = new Label();
    private final BorderPane shell = new BorderPane();

    public MainWindow(CompositionRoot root) {
        this.root = root;
        register(new DashboardScreen(root));
        register(new SetupScreen(root));
        register(new JobsScreen(root));
        register(new TranslateScreen(root, this));
        register(new ModelsScreen(root));
        register(new SettingsScreen(root, recursion));
        register(new LogsScreen(root.logService()));
        layout();
        select("Dashboard");
    }

    public ObjectProperty<RecursionSettings> recursionSettings() {
        return recursion;
    }

    public BorderPane rootNode() {
        return shell;
    }

    private void register(Screen screen) {
        screens.put(screen.title(), screen);
    }

    private void layout() {
        shell.setLeft(navigation());
        shell.setTop(headerBar());
        shell.setCenter(content);
    }

    private Node navigation() {
        VBox nav = new VBox(6);
        nav.setPadding(new Insets(12));
        nav.setPrefWidth(170);
        nav.setStyle("-fx-background-color: #1f2937;");
        Label app = new Label("Recursive");
        app.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        VBox.setMargin(app, new Insets(0, 0, 8, 0));
        nav.getChildren().add(app);
        for (String name : screens.keySet()) {
            Button button = new Button(name);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setStyle("-fx-alignment: CENTER-LEFT;");
            button.setOnAction(event -> select(name));
            nav.getChildren().add(button);
        }
        return nav;
    }

    private Node headerBar() {
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(10, header, spacer);
        bar.setPadding(new Insets(10, 16, 10, 16));
        bar.setStyle("-fx-border-color: transparent transparent #d1d5db transparent;");
        return bar;
    }

    private void select(String name) {
        Node node = built.computeIfAbsent(name, key -> screens.get(key).build());
        content.getChildren().setAll(node);
        header.setText(name);
    }
}
