package com.recursive.app.ui;

import com.recursive.application.LogService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

/**
 * Live log viewer: seeds with the most recent entries and appends new ones
 * as the services produce them.
 */
public final class LogsScreen implements Screen {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final LogService logService;
    private final TextArea area = new TextArea();
    private final ComboBox<String> levelFilter = new ComboBox<>();
    private LogService.Level minimum;

    public LogsScreen(LogService logService) {
        this.logService = logService;
    }

    @Override
    public Node build() {
        area.setEditable(false);
        area.setStyle("-fx-font-family: 'Consolas', monospace;");
        VBox.setVgrow(area, Priority.ALWAYS);

        levelFilter.getItems().setAll("All levels", "INFO", "WARN", "ERROR");
        levelFilter.setValue("All levels");
        levelFilter.setOnAction(event -> applyFilter());

        Button clear = new Button("Clear");
        clear.setOnAction(event -> area.clear());

        HBox toolbar = new HBox(8, new Label("Filter"), levelFilter, clear);
        toolbar.setPadding(new Insets(8));

        VBox panel = new VBox(8, toolbar, area);
        panel.setPadding(new Insets(8));

        seed();
        logService.addListener(this::append);
        return panel;
    }

    @Override
    public String title() {
        return "Logs";
    }

    private void applyFilter() {
        String selected = levelFilter.getValue();
        if (selected == null || selected.equals("All levels")) {
            minimum = null;
        } else {
            minimum = LogService.Level.valueOf(selected);
        }
        area.clear();
        logService.recent(500).stream().filter(this::visible).forEach(this::appendNow);
    }

    private boolean visible(LogService.LogEntry entry) {
        return minimum == null || entry.level().compareTo(minimum) >= 0;
    }

    private void seed() {
        logService.recent(500).stream().filter(this::visible).forEach(this::appendNow);
    }

    private void append(LogService.LogEntry entry) {
        if (visible(entry)) {
            Platform.runLater(() -> appendNow(entry));
        }
    }

    private void appendNow(LogService.LogEntry entry) {
        area.appendText(TIME.format(entry.timestamp()) + " " + entry.level() + " "
                + entry.source() + " | " + entry.message() + "\n");
    }
}
