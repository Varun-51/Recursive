package com.recursive.infrastructure.database;

import com.recursive.domain.Block;
import com.recursive.domain.BlockContentType;
import com.recursive.domain.BlockRepository;
import com.recursive.domain.FontInfo;
import com.recursive.domain.FontStyle;
import com.recursive.domain.Position;
import com.recursive.domain.ValidationStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link BlockRepository}. Reads serve the worker
 * pool (unprocessed blocks) and the reconstructor (reading order); every
 * query is parameterized.
 */
public class JdbcBlockRepository implements BlockRepository {

    private static final String COLUMNS = """
            id, page_id, block_index, content_type, original_text,
            position_x, position_y, position_w, position_h,
            font_name, font_size, font_style, reading_order,
            translated_text, validation_status, confidence_score, retry_count,
            context_json, validation_issues_json, created_at, updated_at
            """;

    private final ConnectionProvider connectionProvider;

    public JdbcBlockRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Block save(Block block) {
        String sql = """
                INSERT OR REPLACE INTO blocks (%s)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """.formatted(COLUMNS);
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, block.id());
            statement.setString(2, block.pageId());
            statement.setInt(3, block.blockIndex());
            statement.setString(4, block.contentType().name());
            statement.setString(5, block.originalText());
            statement.setFloat(6, block.position().x());
            statement.setFloat(7, block.position().y());
            statement.setFloat(8, block.position().width());
            statement.setFloat(9, block.position().height());
            statement.setString(10, block.fontInfo() == null ? null : block.fontInfo().name());
            statement.setFloat(11, block.fontInfo() == null ? 0f : block.fontInfo().size());
            statement.setString(12, block.fontInfo() == null ? null : block.fontInfo().style().name());
            statement.setInt(13, block.readingOrder());
            statement.setString(14, block.translatedText());
            statement.setString(15, block.validationStatus() == null ? null : block.validationStatus().name());
            statement.setDouble(16, block.confidenceScore() == null ? Double.NaN : block.confidenceScore());
            statement.setInt(17, block.retryCount());
            statement.setString(18, block.contextJson());
            statement.setString(19, block.validationIssuesJson());
            statement.setString(20, block.createdAt().toString());
            statement.setString(21, block.updatedAt().toString());
            statement.executeUpdate();
            return block;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save block " + block.id(), e);
        }
    }

    @Override
    public Optional<Block> findById(String id) {
        String sql = "SELECT " + COLUMNS + " FROM blocks WHERE id = ?";
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load block " + id, e);
        }
    }

    @Override
    public List<Block> findByPageId(String pageId) {
        String sql = "SELECT " + COLUMNS + " FROM blocks WHERE page_id = ? ORDER BY reading_order";
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pageId);
            return readAll(statement);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not query blocks of page " + pageId, e);
        }
    }

    @Override
    public List<Block> findUnprocessedByPageId(String pageId) {
        String sql = "SELECT " + COLUMNS
                + " FROM blocks WHERE page_id = ? AND translated_text IS NULL ORDER BY reading_order";
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pageId);
            return readAll(statement);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not query unprocessed blocks of page " + pageId, e);
        }
    }

    @Override
    public void deleteByPageId(String pageId) {
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM blocks WHERE page_id = ?")) {
            statement.setString(1, pageId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete blocks of page " + pageId, e);
        }
    }

    private static List<Block> readAll(PreparedStatement statement) throws SQLException {
        List<Block> blocks = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                blocks.add(map(resultSet));
            }
        }
        return blocks;
    }

    private static Block map(ResultSet resultSet) throws SQLException {
        double confidence = resultSet.getDouble("confidence_score");
        boolean hasConfidence = !resultSet.wasNull();
        return new Block(
                resultSet.getString("id"),
                resultSet.getString("page_id"),
                resultSet.getInt("block_index"),
                BlockContentType.valueOf(resultSet.getString("content_type")),
                resultSet.getString("original_text"),
                new Position(resultSet.getFloat("position_x"), resultSet.getFloat("position_y"),
                        resultSet.getFloat("position_w"), resultSet.getFloat("position_h")),
                fontInfo(resultSet),
                resultSet.getInt("reading_order"),
                resultSet.getString("translated_text"),
                nullableEnum(resultSet, "validation_status"),
                hasConfidence && !Double.isNaN(confidence) ? confidence : null,
                resultSet.getInt("retry_count"),
                resultSet.getString("context_json"),
                resultSet.getString("validation_issues_json"),
                Instant.parse(resultSet.getString("created_at")),
                Instant.parse(resultSet.getString("updated_at")));
    }

    private static FontInfo fontInfo(ResultSet resultSet) throws SQLException {
        String name = resultSet.getString("font_name");
        String style = resultSet.getString("font_style");
        if (name == null || style == null) {
            return null;
        }
        return new FontInfo(name, resultSet.getFloat("font_size"), FontStyle.valueOf(style));
    }

    private static ValidationStatus nullableEnum(ResultSet resultSet, String column)
            throws SQLException {
        String value = resultSet.getString(column);
        return value == null ? null : ValidationStatus.valueOf(value);
    }
}
