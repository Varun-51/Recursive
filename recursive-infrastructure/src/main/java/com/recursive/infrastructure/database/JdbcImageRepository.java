package com.recursive.infrastructure.database;

import com.recursive.domain.ImageReference;
import com.recursive.domain.ImageRepository;
import com.recursive.domain.Position;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link ImageRepository}. Image bytes travel as
 * BLOBs so the reconstructor can re-place rasters without re-reading the
 * source PDF.
 */
public class JdbcImageRepository implements ImageRepository {

    private static final String COLUMNS = """
            id, page_id, image_index, position_x, position_y, position_w,
            position_h, image_data, original_format, created_at
            """;

    private final ConnectionProvider connectionProvider;

    public JdbcImageRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public ImageReference save(ImageReference image) {
        String sql = """
                INSERT OR REPLACE INTO images (id, page_id, image_index, position_x,
                position_y, position_w, position_h, image_data, original_format,
                created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """;
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, image.id());
            statement.setString(2, image.pageId());
            statement.setInt(3, image.imageIndex());
            statement.setFloat(4, image.position().x());
            statement.setFloat(5, image.position().y());
            statement.setFloat(6, image.position().width());
            statement.setFloat(7, image.position().height());
            statement.setBytes(8, image.imageData());
            statement.setString(9, image.originalFormat());
            statement.setString(10, image.createdAt().toString());
            statement.executeUpdate();
            return image;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save image " + image.id(), e);
        }
    }

    @Override
    public Optional<ImageReference> findById(String id) {
        String sql = "SELECT " + COLUMNS + " FROM images WHERE id = ?";
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load image " + id, e);
        }
    }

    @Override
    public List<ImageReference> findByPageId(String pageId) {
        String sql = "SELECT " + COLUMNS + " FROM images WHERE page_id = ? ORDER BY image_index";
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pageId);
            return readAll(statement);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not query images of page " + pageId, e);
        }
    }

    @Override
    public void deleteByPageId(String pageId) {
        try (Connection connection = connectionProvider.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM images WHERE page_id = ?")) {
            statement.setString(1, pageId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete images of page " + pageId, e);
        }
    }

    private static List<ImageReference> readAll(PreparedStatement statement) throws SQLException {
        List<ImageReference> images = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                images.add(map(resultSet));
            }
        }
        return images;
    }

    private static ImageReference map(ResultSet resultSet) throws SQLException {
        return new ImageReference(
                resultSet.getString("id"),
                resultSet.getString("page_id"),
                resultSet.getInt("image_index"),
                new Position(resultSet.getFloat("position_x"), resultSet.getFloat("position_y"),
                        resultSet.getFloat("position_w"), resultSet.getFloat("position_h")),
                resultSet.getBytes("image_data"),
                resultSet.getString("original_format"),
                Instant.parse(resultSet.getString("created_at")));
    }
}
