package com.recursive.infrastructure.database;

import com.recursive.domain.GlossaryRepository;
import com.recursive.domain.GlossaryTerm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link GlossaryRepository}. Locked terms are
 * loaded per job for every translation call, so this path stays indexed
 * and parameterized.
 */
public class JdbcGlossaryRepository implements GlossaryRepository {

    private static final String COLUMNS = """
            id, job_id, source_term, target_term, category, locked,
            occurrences, created_at
            """;

    private final ConnectionProvider connectionProvider;

    public JdbcGlossaryRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public GlossaryTerm save(GlossaryTerm term) {
        String sql = """
                INSERT OR REPLACE INTO glossary_terms (id, job_id, source_term,
                target_term, category, locked, occurrences, created_at)
                VALUES (?,?,?,?,?,?,?,?)
                """;
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, term.id());
            statement.setString(2, term.jobId());
            statement.setString(3, term.sourceTerm());
            statement.setString(4, term.targetTerm());
            statement.setString(5, term.category());
            statement.setInt(6, term.locked() ? 1 : 0);
            statement.setInt(7, term.occurrences());
            statement.setString(8, term.createdAt().toString());
            statement.executeUpdate();
            return term;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save glossary term " + term.id(), e);
        }
    }

    @Override
    public Optional<GlossaryTerm> findById(String id) {
        String sql = "SELECT " + COLUMNS + " FROM glossary_terms WHERE id = ?";
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load glossary term " + id, e);
        }
    }

    @Override
    public List<GlossaryTerm> findByJobId(String jobId) {
        String sql = "SELECT " + COLUMNS + " FROM glossary_terms WHERE job_id = ? ORDER BY source_term";
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, jobId);
            return readAll(statement);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not query glossary of job " + jobId, e);
        }
    }

    @Override
    public List<GlossaryTerm> findLockedByJobId(String jobId) {
        String sql = "SELECT " + COLUMNS
                + " FROM glossary_terms WHERE job_id = ? AND locked = 1 ORDER BY source_term";
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, jobId);
            return readAll(statement);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not query locked glossary of job " + jobId, e);
        }
    }

    @Override
    public void deleteByJobId(String jobId) {
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM glossary_terms WHERE job_id = ?")) {
            statement.setString(1, jobId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete glossary of job " + jobId, e);
        }
    }

    private static List<GlossaryTerm> readAll(PreparedStatement statement) throws SQLException {
        List<GlossaryTerm> terms = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                terms.add(map(resultSet));
            }
        }
        return terms;
    }

    private static GlossaryTerm map(ResultSet resultSet) throws SQLException {
        return new GlossaryTerm(
                resultSet.getString("id"),
                resultSet.getString("job_id"),
                resultSet.getString("source_term"),
                resultSet.getString("target_term"),
                resultSet.getString("category"),
                resultSet.getInt("locked") == 1,
                resultSet.getInt("occurrences"),
                Instant.parse(resultSet.getString("created_at")));
    }
}
