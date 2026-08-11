package com.recursive.infrastructure.database;

import com.recursive.domain.Job;
import com.recursive.domain.JobRepository;
import com.recursive.domain.JobStatus;
import com.recursive.domain.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link JobRepository}. Writes use INSERT OR
 * REPLACE (SQLite upsert); every query is parameterized. The pool inside
 * {@link ConnectionProvider} is shared, so concurrent workers may call
 * this repository safely.
 */
public class JdbcJobRepository implements JobRepository {

    private static final String COLUMNS = """
            id, name, source_file_path, source_lang_code, source_lang_name,
            target_lang_code, target_lang_name, model_name, configuration_json,
            status, total_pages, completed_pages, total_blocks, completed_blocks,
            validated_blocks, failed_blocks, error_message, created_at, updated_at
            """;

    private final ConnectionProvider connectionProvider;

    public JdbcJobRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Job save(Job job) {
        String sql = """
                INSERT OR REPLACE INTO jobs (%s) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """.formatted(COLUMNS);
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, job.id());
            statement.setString(2, job.name());
            statement.setString(3, job.sourceFilePath());
            statement.setString(4, job.sourceLanguage().code());
            statement.setString(5, job.sourceLanguage().name());
            statement.setString(6, job.targetLanguage().code());
            statement.setString(7, job.targetLanguage().name());
            statement.setString(8, job.modelName());
            statement.setString(9, job.configurationJson());
            statement.setString(10, job.status().name());
            statement.setInt(11, job.totalPages());
            statement.setInt(12, job.completedPages());
            statement.setInt(13, job.totalBlocks());
            statement.setInt(14, job.completedBlocks());
            statement.setInt(15, job.validatedBlocks());
            statement.setInt(16, job.failedBlocks());
            statement.setString(17, job.errorMessage());
            statement.setString(18, job.createdAt().toString());
            statement.setString(19, job.updatedAt().toString());
            statement.executeUpdate();
            return job;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save job " + job.id(), e);
        }
    }

    @Override
    public Optional<Job> findById(String id) {
        String sql = "SELECT " + COLUMNS + " FROM jobs WHERE id = ?";
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load job " + id, e);
        }
    }

    @Override
    public List<Job> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM jobs ORDER BY created_at DESC";
        return queryAll(sql);
    }

    @Override
    public List<Job> findByStatus(JobStatus status) {
        String sql = "SELECT " + COLUMNS + " FROM jobs WHERE status = ? ORDER BY created_at DESC";
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            return readAll(statement);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not query jobs by status", e);
        }
    }

    @Override
    public void delete(String id) {
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM jobs WHERE id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete job " + id, e);
        }
    }

    private List<Job> queryAll(String sql) {
        try (Connection connection = connectionProvider.connection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return readAll(resultSet);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not query jobs", e);
        }
    }

    private static List<Job> readAll(PreparedStatement statement) throws SQLException {
        List<Job> jobs = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                jobs.add(map(resultSet));
            }
        }
        return jobs;
    }

    private static List<Job> readAll(ResultSet resultSet) throws SQLException {
        List<Job> jobs = new ArrayList<>();
        while (resultSet.next()) {
            jobs.add(map(resultSet));
        }
        return jobs;
    }

    private static Job map(ResultSet resultSet) throws SQLException {
        return new Job(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("source_file_path"),
                new Language(resultSet.getString("source_lang_code"),
                        resultSet.getString("source_lang_name")),
                new Language(resultSet.getString("target_lang_code"),
                        resultSet.getString("target_lang_name")),
                resultSet.getString("model_name"),
                resultSet.getString("configuration_json"),
                JobStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("total_pages"),
                resultSet.getInt("completed_pages"),
                resultSet.getInt("total_blocks"),
                resultSet.getInt("completed_blocks"),
                resultSet.getInt("validated_blocks"),
                resultSet.getInt("failed_blocks"),
                resultSet.getString("error_message"),
                Instant.parse(resultSet.getString("created_at")),
                Instant.parse(resultSet.getString("updated_at")));
    }
}
