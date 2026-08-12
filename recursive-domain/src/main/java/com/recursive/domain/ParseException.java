package com.recursive.domain;

/**
 * Raised when a document cannot be parsed: corrupted file, unsupported
 * format, or a password that does not unlock the document. Carries a
 * user-presentable message; document content never travels in it.
 */
public class ParseException extends RuntimeException {

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
