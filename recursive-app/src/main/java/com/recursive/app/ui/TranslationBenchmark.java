package com.recursive.app.ui;

import com.recursive.app.CompositionRoot;
import com.recursive.application.CompletionEstimator;
import com.recursive.domain.HardwareSpec;
import com.recursive.domain.Job;
import com.recursive.domain.ModelInfo;
import com.recursive.domain.PageStatus;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Pre-flight benchmark for a "Translate all" run. It checks that the
 * machine can currently run the recommended model, estimates the wall time
 * from the resources available right now, and if the estimate exceeds
 * {@link CompletionEstimator#BENCHMARK_MINUTES} it asks the user whether to
 * continue or wait until more resources free up (re-checking every few
 * seconds and starting automatically once the estimate drops).
 */
public final class TranslationBenchmark {

    private final CompositionRoot root;
    private final Consumer<String> status;
    private Timeline waiting;

    /** Outcome of the off-thread analysis. */
    public record StartAnalysis(int pendingPages, Duration estimate,
                                boolean canRunModel, String issue) {
    }

    public TranslationBenchmark(CompositionRoot root, Consumer<String> status) {
        this.root = root;
        this.status = status;
    }

    /** Off-thread: counts pending pages and checks the model fits the free RAM. */
    public StartAnalysis analyze(Job job) {
        long pending = root.pages().findByJobId(job.id()).stream()
                .filter(page -> page.status() != PageStatus.COMPLETED).count();
        boolean canRun = true;
        String issue = null;
        try {
            Optional<ModelInfo> recommended = root.modelService().recommendModel();
            HardwareSpec spec = root.hardware().detect();
            if (recommended.isPresent()
                    && recommended.get().ramRequiredGb() > spec.availableRamGb()) {
                canRun = false;
                issue = recommended.get().name() + " needs " + recommended.get().ramRequiredGb()
                        + " GB RAM, only " + spec.availableRamGb() + " GB is free";
            }
        } catch (RuntimeException e) {
            canRun = false;
            issue = e.getMessage();
        }
        return new StartAnalysis((int) pending,
                root.estimator().estimateRemaining((int) pending,
                        root.throughput().parallelSlots()),
                canRun, issue);
    }

    /** FX thread: turns an analysis into a decision, starting the run via {@code startRun}. */
    public void decide(Job job, StartAnalysis analysis, Consumer<Job> startRun) {
        if (analysis.pendingPages() <= 0) {
            status.accept("No pending pages to translate");
            return;
        }
        if (!analysis.canRunModel()) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "Not enough resources to run the model (" + analysis.issue()
                            + "). Kindly close background tasks to continue.");
            alert.setHeaderText("Insufficient resources");
            alert.getButtonTypes().setAll(ButtonType.OK);
            alert.showAndWait();
            return;
        }
        if (CompletionEstimator.overBenchmark(analysis.estimate())) {
            ButtonType continueNow = new ButtonType("Continue anyway");
            ButtonType waitNow = new ButtonType("Wait for more resources");
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Estimated time: ~" + format(analysis.estimate()) + " with "
                            + root.throughput().parallelSlots() + " parallel workers, which exceeds "
                            + CompletionEstimator.BENCHMARK_MINUTES + " minutes.\n"
                            + "Continue anyway, or wait until more resources become available to"
                            + " cut down the time?");
            alert.setHeaderText("Long run detected");
            alert.getButtonTypes().setAll(waitNow, continueNow, ButtonType.CANCEL);
            Optional<ButtonType> choice = alert.showAndWait();
            if (choice.filter(continueNow::equals).isPresent()) {
                startRun.accept(job);
            } else if (choice.filter(waitNow::equals).isPresent()) {
                waitForResources(job, analysis.pendingPages(), startRun);
            }
            return;
        }
        startRun.accept(job);
    }

    public void stopWaiting() {
        if (waiting != null && waiting.getStatus() == Animation.Status.RUNNING) {
            waiting.stop();
        }
    }

    static String format(Duration duration) {
        long minutes = duration.toMinutes();
        long hours = minutes / 60;
        return hours > 0 ? hours + "h " + (minutes % 60) + "m" : minutes + "m";
    }

    private void waitForResources(Job job, int pendingPages, Consumer<Job> startRun) {
        status.accept("Waiting for more resources \u2026");
        waiting = new Timeline(new KeyFrame(javafx.util.Duration.seconds(5), event -> {
            Duration estimate = root.estimator().estimateRemaining(pendingPages,
                    root.throughput().parallelSlots());
            status.accept("Waiting for more resources \u2026 current ETA ~" + format(estimate)
                    + " (" + root.throughput().parallelSlots() + " workers)");
            if (!CompletionEstimator.overBenchmark(estimate)) {
                waiting.stop();
                status.accept("Resources freed up \u2014 starting with "
                        + root.throughput().parallelSlots() + " workers");
                startRun.accept(job);
            }
        }));
        waiting.setCycleCount(Animation.INDEFINITE);
        waiting.play();
    }
}
