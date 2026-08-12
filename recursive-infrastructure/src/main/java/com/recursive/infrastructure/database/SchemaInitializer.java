package com.recursive.infrastructure.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the SQLite schema on first startup. Every statement is
 * idempotent (IF NOT EXISTS); the JSON columns stay TEXT blobs whose shape
 * is owned by the application layer.
 */
public class SchemaInitializer {

    private static final String CREATE_JOBS = """
            CREATE TABLE IF NOT EXISTS jobs (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                source_file_path TEXT NOT NULL,
                source_lang_code TEXT NOT NULL,
                source_lang_name TEXT NOT NULL,
                target_lang_code TEXT NOT NULL,
                target_lang_name TEXT NOT NULL,
                model_name TEXT NOT NULL,
                configuration_json TEXT,
                status TEXT NOT NULL,
                total_pages INTEGER NOT NULL,
                completed_pages INTEGER NOT NULL,
                total_blocks INTEGER NOT NULL,
                completed_blocks INTEGER NOT NULL,
                validated_blocks INTEGER NOT NULL,
                failed_blocks INTEGER NOT NULL,
                error_message TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )
            """;

    private static final String CREATE_PAGES = """
            CREATE TABLE IF NOT EXISTS pages (
                id TEXT PRIMARY KEY,
                job_id TEXT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
                page_number INTEGER NOT NULL,
                status TEXT NOT NULL,
                extracted_json TEXT,
                translated_json TEXT,
                validation_json TEXT,
                retry_count INTEGER NOT NULL,
                error_message TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )
            """;

    private static final String CREATE_BLOCKS = """
            CREATE TABLE IF NOT EXISTS blocks (
                id TEXT PRIMARY KEY,
                page_id TEXT NOT NULL REFERENCES pages(id) ON DELETE CASCADE,
                block_index INTEGER NOT NULL,
                content_type TEXT NOT NULL,
                original_text TEXT NOT NULL,
                position_x REAL NOT NULL,
                position_y REAL NOT NULL,
                position_w REAL NOT NULL,
                position_h REAL NOT NULL,
                font_name TEXT,
                font_size REAL,
                font_style TEXT,
                reading_order INTEGER NOT NULL,
                translated_text TEXT,
                validation_status TEXT,
                confidence_score REAL,
                retry_count INTEGER NOT NULL,
                context_json TEXT,
                validation_issues_json TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )
            """;

    private static final String CREATE_IMAGES = """
            CREATE TABLE IF NOT EXISTS images (
                id TEXT PRIMARY KEY,
                page_id TEXT NOT NULL REFERENCES pages(id) ON DELETE CASCADE,
                image_index INTEGER NOT NULL,
                position_x REAL NOT NULL,
                position_y REAL NOT NULL,
                position_w REAL NOT NULL,
                position_h REAL NOT NULL,
                image_data BLOB NOT NULL,
                original_format TEXT,
                created_at TEXT NOT NULL
            )
            """;

    private static final String CREATE_GLOSSARY = """
            CREATE TABLE IF NOT EXISTS glossary_terms (
                id TEXT PRIMARY KEY,
                job_id TEXT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
                source_term TEXT NOT NULL,
                target_term TEXT NOT NULL,
                category TEXT,
                locked INTEGER NOT NULL,
                occurrences INTEGER NOT NULL,
                created_at TEXT NOT NULL
            )
            """;

    public void initialize(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_JOBS);
            statement.execute(CREATE_PAGES);
            statement.execute(CREATE_BLOCKS);
            statement.execute(CREATE_IMAGES);
            statement.execute(CREATE_GLOSSARY);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize the database schema", e);
        }
    }
}
