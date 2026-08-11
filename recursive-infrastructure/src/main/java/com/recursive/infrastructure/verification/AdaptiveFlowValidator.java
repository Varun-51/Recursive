package com.recursive.infrastructure.verification;

import com.recursive.domain.Confidence;
import com.recursive.domain.ProcessingContext;
import com.recursive.domain.SemanticValidator;
import com.recursive.domain.ValidationReport;
import com.recursive.domain.ValidationStatus;
import com.recursive.infrastructure.verification.checks.NegationCheck;
import com.recursive.infrastructure.verification.checks.NumberPreservationCheck;
import com.recursive.infrastructure.verification.checks.TerminologyCheck;
import com.recursive.infrastructure.verification.checks.UnitPreservationCheck;
import io.github.varun51.adaptiveflow.Task;
import io.github.varun51.adaptiveflow.WorkflowBuilder;
import io.github.varun51.adaptiveflow.WorkflowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link SemanticValidator} implementation that runs the verification
 * stages (numbers, units, terminology, negation) as a deterministic
 * AdaptiveFlow pipeline. Meaning-level review by an LLM is Phase 2; the
 * checks here are exact, so failures are genuine re-translation guidance.
 */
public class AdaptiveFlowValidator implements SemanticValidator {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveFlowValidator.class);

    @Override
    public Optional<ValidationReport> validate(String originalText, String translatedText,
                                               ProcessingContext context) {
        WorkflowResult result = workflow(originalText, translatedText, context).execute();
        List<String> issues = collectIssues(result);
        if (issues.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ValidationReport(ValidationStatus.NEEDS_REVIEW,
                issues, Confidence.of(0.0)));
    }

    private static WorkflowBuilder workflow(String original, String translation,
                                            ProcessingContext context) {
        Task<List<String>> numbers = ignored -> NumberPreservationCheck.run(original, translation);
        Task<List<String>> units = ignored -> UnitPreservationCheck.run(original, translation);
        Task<List<String>> terms = ignored -> TerminologyCheck.run(translation, context);
        Task<List<String>> negation = ignored -> NegationCheck.run(original, translation);
        return WorkflowBuilder.builder("verify-block")
                .task("numbers", numbers)
                .then("units", units)
                .then("terms", terms)
                .then("negation", negation);
    }

    private static List<String> collectIssues(WorkflowResult result) {
        List<String> issues = new ArrayList<>();
        for (String stage : List.of("numbers", "units", "terms", "negation")) {
            var taskResult = result.taskResult(stage);
            if (taskResult == null) {
                log.warn("Verification stage {} produced no result", stage);
                continue;
            }
            @SuppressWarnings("unchecked")
            List<String> stageIssues = (List<String>) taskResult.output();
            if (stageIssues != null) {
                issues.addAll(stageIssues);
            }
        }
        return issues;
    }
}
