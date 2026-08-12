package com.recursive.app.ui;

import com.recursive.app.CompositionRoot;
import com.recursive.domain.Block;
import com.recursive.domain.Job;
import com.recursive.domain.Page;
import com.recursive.domain.ValidationStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Pipeline screen for one job: ingest the PDF, translate page by page,
 * review individual blocks, and export the finished translation. The
 * "Translate all" entry point delegates its benchmark — model/RAM check,
 * ETA estimate, continue-or-wait decision — to {@link TranslationBenchmark}.
 */
public final class TranslateScreen implements Screen {

    private final CompositionRoot root;
    private final MainWindow window;
    private final TranslationBenchmark benchmark;
    private final ComboBox<Job> jobSelector = new ComboBox<>();
    private final TableView<Page> pages = new TableView<>();
    private final TableView<Block> blocks = new TableView<>();
    private final TextArea editor = new TextArea();
    private final Label status = new Label();
    private final Label output = new Label();
    private final Button translatePage;
    private final Button translateAll;
    private volatile boolean translationActive;

    public TranslateScreen(CompositionRoot root, MainWindow window) {
        this.root = root;
        this.window = window;
        this.translatePage = new Button("Translate page");
        this.translateAll = new Button("Translate all");
        this.benchmark = new TranslationBenchmark(root, status::setText);
    }

    @Override
    public Node build() {
        jobSelector.setPromptText("Select job");
        jobSelector.setMinWidth(220);
        jobSelector.setOnAction(event -> loadPages());

        Button ingest = new Button("Ingest PDF");
        ingest.setOnAction(event -> ingest());

        translatePage.setOnAction(event -> translateSelectedPage());
        translateAll.setOnAction(event -> translateAll());

        Button export = new Button("Export PDF");
        export.setOnAction(event -> export());

        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> refreshJobs());

        HBox toolbar = new HBox(8, jobSelector, ingest, translatePage, translateAll, export, refresh);
        toolbar.setPadding(new Insets(8));

        SplitPane split = new SplitPane(pagesPanel(), blocksPanel());
        split.setDividerPositions(0.3);

        Button save = new Button("Save edit");
        save.setOnAction(event -> saveEditedBlock());
        editor.setPrefRowCount(4);
        editor.setPromptText("Select a block to review and edit its translation");
        HBox editorBar = new HBox(8, editor, save);
        HBox.setHgrow(editor, Priority.ALWAYS);

        VBox panel = new VBox(8, toolbar, split, editorBar, status, output);
        panel.setPadding(new Insets(8));
        VBox.setVgrow(split, Priority.ALWAYS);

        refreshJobs();
        return panel;
    }

    private VBox pagesPanel() {
        pages.getColumns().add(TableColumns.string("Page", 60,
                page -> String.valueOf(page.pageNumber())));
        pages.getColumns().add(TableColumns.string("Status", 110, page -> page.status().name()));
        pages.getColumns().add(TableColumns.string("Retries", 70,
                page -> String.valueOf(page.retryCount())));
        pages.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        pages.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> loadBlocks());
        VBox panel = new VBox(6, new Label("Pages"), pages);
        VBox.setVgrow(pages, Priority.ALWAYS);
        return panel;
    }

    private VBox blocksPanel() {
        blocks.getColumns().add(TableColumns.string("Original", 300, Block::originalText));
        blocks.getColumns().add(TableColumns.string("Translated", 300, block -> block.translatedText()));
        blocks.getColumns().add(TableColumns.string("Validation", 110,
                block -> block.validationStatus() == null ? "" : block.validationStatus().name()));
        blocks.getColumns().add(TableColumns.string("Confidence", 90,
                block -> block.confidenceScore() == null ? "" : String.valueOf(block.confidenceScore())));
        blocks.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        blocks.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> showInEditor(selected));
        VBox panel = new VBox(6, new Label("Blocks"), blocks);
        VBox.setVgrow(blocks, Priority.ALWAYS);
        return panel;
    }

    @Override
    public String title() {
        return "Translate";
    }

    private void refreshJobs() {
        BackgroundTasks.run("jobs-load", root.jobs()::findAll,
                jobs -> {
                    jobSelector.setItems(FXCollections.observableArrayList(jobs));
                    status.setText(jobs.size() + " job(s)");
                },
                error -> status.setText("Could not load jobs: " + error.getMessage()));
    }

    private void loadPages() {
        Job job = jobSelector.getValue();
        if (job == null) {
            return;
        }
        BackgroundTasks.run("pages-load", () -> root.pages().findByJobId(job.id()),
                list -> pages.setItems(FXCollections.observableArrayList(list)),
                error -> status.setText("Could not load pages: " + error.getMessage()));
    }

    private void loadBlocks() {
        Page page = pages.getSelectionModel().getSelectedItem();
        if (page == null) {
            blocks.setItems(FXCollections.emptyObservableList());
            return;
        }
        BackgroundTasks.run("blocks-load", () -> root.blocks().findByPageId(page.id()),
                list -> blocks.setItems(FXCollections.observableArrayList(list)),
                error -> status.setText("Could not load blocks: " + error.getMessage()));
    }

    private void ingest() {
        Job job = jobSelector.getValue();
        if (job == null) {
            status.setText("Select a job first");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose the PDF to ingest");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF documents", "*.pdf"));
        Path picked = chooser.showOpenDialog(jobSelector.getScene().getWindow()).toPath();
        if (picked == null) {
            return;
        }
        Optional<String> password = new TextInputDialog().showAndWait();
        BackgroundTasks.run("ingest",
                () -> {
                    root.jobOrchestrator().start(job.id());
                    return root.parsingService().ingest(job.id(), picked, password.orElse(null));
                },
                count -> {
                    status.setText("Ingested " + count + " pages from " + picked.getFileName());
                    loadPages();
                    refreshJobs();
                },
                error -> status.setText("Ingest failed: " + error.getMessage()));
    }

    private void translateAll() {
        Job job = jobSelector.getValue();
        if (job == null) {
            status.setText("Select a job first");
            return;
        }
        if (translationActive) {
            status.setText("A translation is already running");
            return;
        }
        benchmark.stopWaiting();
        BackgroundTasks.run("translate-all-check", () -> benchmark.analyze(job),
                analysis -> benchmark.decide(job, analysis, this::startTranslateAll),
                error -> status.setText("Could not start: " + error.getMessage()));
    }

    private void startTranslateAll(Job job) {
        if (translationActive) {
            return;
        }
        translationActive = true;
        root.estimator().reset();
        long started = System.nanoTime();
        translatePage.setDisable(true);
        translateAll.setDisable(true);
        BackgroundTasks.run("translate-all",
                () -> {
                    root.translationRunner().translateJob(job.id(), job.sourceLanguage(),
                            job.targetLanguage(), job.modelName(), window.recursionSettings().get(),
                            progress -> Platform.runLater(() -> {
                                root.estimator().recordPage(progress.completedPages(),
                                        (System.nanoTime() - started) / 1_000_000);
                                long remaining = progress.totalPages() - progress.completedPages();
                                status.setText("Translated " + progress.completedPages() + "/"
                                        + progress.totalPages() + " pages \u2026 ~" + TranslationBenchmark.format(
                                        root.estimator().estimateRemaining((int) remaining,
                                                root.throughput().parallelSlots())) + " left");
                            }));
                    return null;
                },
                ignored -> finishTranslateAll("All pending pages translated"),
                error -> finishTranslateAll("Translation failed: " + error.getMessage()));
    }

    private void finishTranslateAll(String message) {
        translationActive = false;
        translatePage.setDisable(false);
        translateAll.setDisable(false);
        status.setText(message);
        loadPages();
        refreshJobs();
    }

    private void translateSelectedPage() {
        Job job = jobSelector.getValue();
        Page page = pages.getSelectionModel().getSelectedItem();
        if (job == null || page == null) {
            status.setText("Select a job and a page first");
            return;
        }
        BackgroundTasks.run("translate",
                () -> {
                    root.translationRunner().translatePage(job.id(), page.id(),
                            job.sourceLanguage(), job.targetLanguage(), job.modelName(),
                            window.recursionSettings().get());
                    return null;
                },
                ignored -> {
                    status.setText("Page " + page.pageNumber() + " translated");
                    loadPages();
                    loadBlocks();
                    refreshJobs();
                },
                error -> status.setText("Translation failed: " + error.getMessage()));
    }

    private void export() {
        Job job = jobSelector.getValue();
        if (job == null) {
            status.setText("Select a job first");
            return;
        }
        BackgroundTasks.run("export", () -> root.exportService().export(job.id()),
                path -> output.setText("Exported to " + path),
                error -> output.setText("Export failed: " + error.getMessage()));
    }

    private void showInEditor(Block block) {
        if (block == null) {
            editor.clear();
            return;
        }
        editor.setText(block.translatedText() == null ? "" : block.translatedText());
    }

    private void saveEditedBlock() {
        Block block = blocks.getSelectionModel().getSelectedItem();
        if (block == null) {
            status.setText("Select a block first");
            return;
        }
        block.setTranslatedText(editor.getText());
        if (block.translatedText() != null && !block.translatedText().isBlank()
                && block.validationStatus() == null) {
            block.setValidationStatus(ValidationStatus.NEEDS_REVIEW);
        }
        root.blocks().save(block);
        loadBlocks();
    }
}
