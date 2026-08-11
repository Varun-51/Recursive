package com.recursive.infrastructure.database;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Brings the database to life at startup: creates the parent directory and
 * the pool, then applies the schema. One call from the app entry point;
 * repositories depend on the provider, not on this class.
 */
public class DatabaseInitializer implements Closeable {

    private final Path databaseFile;
    private final ConnectionProvider connectionProvider;
    private final SchemaInitializer schemaInitializer;

    public DatabaseInitializer(Path databaseFile) {
        this.databaseFile = databaseFile.toAbsolutePath().normalize();
        this.connectionProvider = new ConnectionProvider(this.databaseFile);
        this.schemaInitializer = new SchemaInitializer();
    }

    public void initialize() {
        try {
            Files.createDirectories(databaseFile.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Could not create the database directory", e);
        }
        schemaInitializer.initialize(connectionProvider.connection());
    }

    public ConnectionProvider connectionProvider() {
        return connectionProvider;
    }

    @Override
    public void close() {
        connectionProvider.close();
    }
}
