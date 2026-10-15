package com.recursive.infrastructure.database;

import com.recursive.domain.LicenseRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcLicenseRepository implements LicenseRepository {

    private final ConnectionProvider connectionProvider;

    public JdbcLicenseRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public boolean isAccepted() {
        try (Connection conn = connectionProvider.connection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT accepted FROM license WHERE id = 1")) {
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt("accepted") == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read license state", e);
        }
    }

    @Override
    public void accept() {
        try (Connection conn = connectionProvider.connection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT OR REPLACE INTO license (id, accepted) VALUES (1, 1)")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not persist license acceptance", e);
        }
    }
}
