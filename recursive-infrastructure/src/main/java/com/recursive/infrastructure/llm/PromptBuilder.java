package com.recursive.infrastructure.llm;

import com.recursive.domain.ProcessingContext;

/**
 * Builds the system-and-user prompt for one translation call. Glossary
 * terms are appended as a locked mapping so the model keeps them verbatim;
 * the contract only needs the final prompt text.
 */
public final class PromptBuilder {

    private PromptBuilder() {
    }

    public static String translate(String text, String sourceCode, String targetCode,
                                   ProcessingContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Translate the following text from ").append(sourceCode)
                .append(" to ").append(targetCode)
                .append(". Output only the translation, no explanations.\n");
        appendContext(prompt, "previous text", context.previousBlockText());
        appendContext(prompt, "next text", context.nextBlockText());
        appendContext(prompt, "section heading", context.sectionHeading());
        if (!context.glossaryTerms().isEmpty()) {
            prompt.append("Keep these terms untranslated and use them verbatim:\n");
            for (var term : context.glossaryTerms()) {
                prompt.append("- ").append(term.sourceTerm()).append(" -> ").append(term.targetTerm()).append('\n');
            }
        }
        prompt.append("\nText to translate:\n").append(text);
        return prompt.toString();
    }

    private static void appendContext(StringBuilder prompt, String label, String content) {
        if (content != null && !content.isBlank()) {
            prompt.append("Context (").append(label).append("): ").append(content).append('\n');
        }
    }
}
