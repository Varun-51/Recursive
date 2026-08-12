package com.recursive.infrastructure.database;

import com.recursive.domain.Page;
import com.recursive.domain.PageRepository;
import com.recursive.domain.PageStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link PageRepository}. Pages stream through the
 * pool from the ingestion and translation workers; writes are upserts.
 */
public class JdbcPageRepository implements PageRepository {

    private static final String COLUMNS = """
            id, job_id, page_number, status, extracted_json, translated_json,
            validation_json, retry_count, error_message, created_at, updated_at
            """;

    private final ConnectionProvider connectionProvider;

    public JdbcPageRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Page save(Page page) {
        String sql = """
                INSERT OR REPLACE INTO pages (id, job_id, page_number, status,
                extracted_json, translated_json, validation_json, retry_count,
                error_message, created_at, updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, page.id());
            statement.setString(2, page.jobId());
            statement.setInt(3, page.pageNumber());
            statement.setString(4, page.status().name());
            statement.setString(5, page.extractedJson());
            statement.setString(6, page.translatedJson());
            statement.setString(7, page.validationJson());
            statement.setInt(8, page.retryCount());
            statement.setString(9, page.errorMessage());
            statement.setString(10, page.createdAt().toString());
            statement.setString(11, page.updatedAt().toString());
            statement.executeUpdate();
            return page;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save page " + page.id(), e);
        }
    }

    @Override
    public Optional<Page> findById(String id) {
        String sql = "SELECT " + COLUMNS + " FROM pages WHERE id = ?";
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load page " + id, e);
        }
    }

    @Override
    public List<Page> findByJobId(String jobId) {
        String sql = "SELECT " + COLUMNS + " FROM pages WHERE job_id = ? ORDER BY page_number";
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, jobId);
            return readAll(statement);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not query pages of job " + jobId, e);
        }
    }

    @Override
    public List<Page> findIncompleteByJobId(String jobId) {
        String sql = "SELECT " + COLUMNS + " FROM pages WHERE job_id = ? AND status <> ? ORDER BY page_number";
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, jobId);
            statement.setString(2, PageStatus.COMPLETED.name());
            return readAll(statement);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not query incomplete pages of job " + jobId, e);
        }
    }

    @Override
    public void deleteByJobId(String jobId) {
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM pages WHERE job_id = ?")) {
            statement.setString(1, jobId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete pages of job " + jobId, e);
        }
    }

    private static List<Page> readAll(PreparedStatement statement) throws SQLException {
        List<Page> pages = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                pages.add(map(resultSet));
            }
        }
        return pages;
    }

    private static Page map(ResultSet resultSet) throws SQLException {
        return new Page(
                resultSet.getString("id"),
                resultSet.getString("job_id"),
                resultSet.getInt("page_number"),
                PageStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("extracted_json"),
                resultSet.getString("translated_json"),
                resultSet.getString("validation_json"),
                resultSet.getInt("retry_count"),
                resultSet.getString("error_message"),
                Instant.parse(resultSet.getString("created_at")),
                Instant.parse(resultSet.getString("updated_at")));
    }
}
