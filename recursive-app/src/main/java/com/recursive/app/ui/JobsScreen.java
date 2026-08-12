package com.recursive.app.ui;

import com.recursive.app.CompositionRoot;
import com.recursive.domain.Job;
import com.recursive.domain.Language;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Job management screen: the job table with lifecycle actions (start,
 * pause, resume, cancel) and the create-job dialog.
 */
public final class JobsScreen implements Screen {

    private static final List<Language> LANGUAGES = List.of(
            Language.of("en", "English"), Language.of("de", "German"),
            Language.of("fr", "French"), Language.of("es", "Spanish"),
            Language.of("it", "Italian"), Language.of("nl", "Dutch"),
            Language.of("pt", "Portuguese"), Language.of("pl", "Polish"),
            Language.of("tr", "Turkish"), Language.of("ru", "Russian"),
            Language.of("zh", "Chinese"), Language.of("ja", "Japanese"));

    private final CompositionRoot root;
    private final TableView<Job> table = new TableView<>();
    private final Label status = new Label();
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public JobsScreen(CompositionRoot root) {
        this.root = root;
    }

    @Override
    public Node build() {
        table.getColumns().add(TableColumns.string("Name", 180, job -> job.name()));
        table.getColumns().add(TableColumns.string("Status", 110, job -> job.status().name()));
        table.getColumns().add(TableColumns.string("Model", 140, job -> job.modelName()));
        table.getColumns().add(TableColumns.string("Pages", 70,
                job -> job.completedPages() + "/" + job.totalPages()));
        table.getColumns().add(TableColumns.string("Blocks", 70,
                job -> job.completedBlocks() + "/" + job.totalBlocks()));
        table.getColumns().add(TableColumns.string("Created", 130,
                job -> timeFormat.format(job.createdAt())));
        table.getColumns().add(TableColumns.string("Source", 240, job -> job.sourceFilePath()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        HBox toolbar = new HBox(8,
                button("New Job", this::createJobDialog),
                button("Start", () -> transition(jobId -> root.jobOrchestrator().start(jobId))),
                button("Pause", () -> transition(jobId -> root.jobOrchestrator().pause(jobId))),
                button("Resume", () -> transition(jobId -> root.jobOrchestrator().resume(jobId))),
                button("Cancel", () -> transition(jobId -> root.jobOrchestrator().cancel(jobId))),
                button("Refresh", this::refresh));
        toolbar.setPadding(new Insets(8));

        VBox panel = new VBox(8, toolbar, table, status);
        panel.setPadding(new Insets(8));
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
        return panel;
    }

    @Override
    public String title() {
        return "Jobs";
    }

    private Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(event -> action.run());
        return button;
    }

    private void refresh() {
        BackgroundTasks.run("jobs-refresh", root.jobs()::findAll,
                jobs -> {
                    table.setItems(FXCollections.observableArrayList(jobs));
                    status.setText(jobs.size() + " job(s)");
                },
                error -> status.setText("Could not load jobs: " + error.getMessage()));
    }

    private void transition(Function<String, Job> action) {
        Job selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            status.setText("Select a job first");
            return;
        }
        BackgroundTasks.run("job-transition",
                () -> action.apply(selected.id()).id(),
                ignored -> refresh(),
                error -> status.setText("Transition failed: " + error.getMessage()));
    }

    private void createJobDialog() {
        Dialog<Job> dialog = new Dialog<>();
        dialog.setTitle("New translation job");
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Create", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);

        TextField name = new TextField();
        name.setPromptText("e.g. Invoices 2026");
        ComboBox<Language> source = new ComboBox<>(FXCollections.observableArrayList(LANGUAGES));
        source.setValue(LANGUAGES.get(0));
        ComboBox<Language> target = new ComboBox<>(FXCollections.observableArrayList(LANGUAGES));
        target.setValue(LANGUAGES.get(1));
        TextField file = new TextField();
        file.setEditable(false);
        Button browse = new Button("Browse\u2026");
        TextField model = new TextField("llama3.1:8b");
        recommendModel(model);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.setPadding(new Insets(12));
        form.addRow(0, new Label("Name"), name);
        form.addRow(1, new Label("Source"), source);
        form.addRow(2, new Label("Target"), target);
        form.addRow(3, new Label("PDF"), new HBox(8, file, browse));
        form.addRow(4, new Label("Model"), model);
        dialog.getDialogPane().setContent(form);

        browse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "PDF documents", "*.pdf"));
            Path picked = chooser.showOpenDialog(dialog.getOwner()).toPath();
            if (picked != null) {
                file.setText(picked.toString());
            }
        });

        dialog.setResultConverter(button -> {
            if (button.getButtonData() != ButtonBar.ButtonData.OK_DONE) {
                return null;
            }
            return createJob(name.getText(), file.getText(), source.getValue(),
                    target.getValue(), model.getText());
        });
        dialog.showAndWait().ifPresent(job -> status.setText("Created: " + job.name()));
    }

    private Job createJob(String name, String fileText, Language source, Language target, String model) {
        if (name.isBlank() || fileText.isBlank() || model.isBlank()) {
            throw new IllegalArgumentException("Name, PDF file and model are required");
        }
        return root.jobOrchestrator().createJob(name, Path.of(fileText), source, target, model);
    }

    private void recommendModel(TextField model) {
        BackgroundTasks.run("model-recommend", root.modelService()::recommendModel,
                recommendation -> recommendation.ifPresent(info -> model.setText(info.name())),
                ignored -> {
                });
    }
}
