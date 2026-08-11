package com.recursive.infrastructure.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.Closeable;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Owns the HikariCP pool over the SQLite database file. All repositories
 * and the schema initializer draw connections from here; the pool is
 * closed once at application shutdown.
 */
public class ConnectionProvider implements Closeable {

    private final HikariDataSource dataSource;

    public ConnectionProvider(Path databaseFile) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath().normalize());
        config.setMaximumPoolSize(8);
        config.setPoolName("recursive-sqlite");
        config.setConnectionTestQuery("SELECT 1");
        config.setInitializationFailTimeout(-1);
        config.setConnectionInitSql("PRAGMA foreign_keys = ON");
        this.dataSource = new HikariDataSource(config);
    }

    public Connection connection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not obtain a database connection", e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
