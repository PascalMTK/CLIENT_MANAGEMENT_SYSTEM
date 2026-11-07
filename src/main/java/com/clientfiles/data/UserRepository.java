package com.clientfiles.data;

import com.clientfiles.core.ClientSummary;
import com.clientfiles.core.Role;
import com.clientfiles.core.User;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class UserRepository {

    /** A user together with the stored hash, which goes no further than the sign-in check. */
    public record Credentials(User user, String passwordHash) {
    }

    private final Database db;

    public UserRepository(Database db) {
        this.db = db;
    }

    public boolean hasAdmin() {
        return db.call(c -> {
            try (CallableStatement st = c.prepareCall("{call dbo.spHasAdmin}"); ResultSet rs = st.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 1;
            }
        });
    }

    public void createClient(String fullName, String email, String phone, String address, String passwordHash) {
        db.call(c -> {
            try (CallableStatement st = c.prepareCall("{call dbo.spCreateClient(?,?,?,?,?)}")) {
                st.setString(1, fullName);
                st.setString(2, email);
                st.setString(3, emptyToNull(phone));
                st.setString(4, emptyToNull(address));
                st.setString(5, passwordHash);
                st.execute();
                return null;
            }
        });
    }

    public void createFirstAdmin(String fullName, String email, String passwordHash) {
        db.call(c -> {
            try (CallableStatement st = c.prepareCall("{call dbo.spCreateFirstAdmin(?,?,?)}")) {
                st.setString(1, fullName);
                st.setString(2, email);
                st.setString(3, passwordHash);
                st.execute();
                return null;
            }
        });
    }

    public Optional<Credentials> findByEmail(String email) {
        return db.call(c -> {
            try (CallableStatement st = c.prepareCall("{call dbo.spGetUserByEmail(?)}")) {
                st.setString(1, email);
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    User user = new User(rs.getInt("userID"), rs.getString("fullName"), rs.getString("email"),
                            rs.getString("phoneNumber"), rs.getString("address"),
                            Role.fromDb(rs.getString("privilege")), "active".equals(rs.getString("status")),
                            dateTime(rs, "createdAt"));
                    return Optional.of(new Credentials(user, rs.getString("passwordHash")));
                }
            }
        });
    }

    public String passwordHash(int userId) {
        return db.call(c -> {
            try (CallableStatement st = c.prepareCall("{call dbo.spGetPasswordHash(?)}")) {
                st.setInt(1, userId);
                try (ResultSet rs = st.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        });
    }

    public void updateProfile(int userId, String fullName, String email, String phone, String address) {
        db.call(c -> {
            try (CallableStatement st = c.prepareCall("{call dbo.spUpdateProfile(?,?,?,?,?)}")) {
                st.setInt(1, userId);
                st.setString(2, fullName);
                st.setString(3, email);
                st.setString(4, emptyToNull(phone));
                st.setString(5, emptyToNull(address));
                st.execute();
                return null;
            }
        });
    }

    public void changePassword(int userId, String passwordHash) {
        db.call(c -> {
            try (CallableStatement st = c.prepareCall("{call dbo.spChangePassword(?,?)}")) {
                st.setInt(1, userId);
                st.setString(2, passwordHash);
                st.execute();
                return null;
            }
        });
    }

    public List<ClientSummary> clients(int adminId) {
        return db.call(c -> {
            try (CallableStatement st = c.prepareCall("{call dbo.spGetClients(?)}")) {
                st.setInt(1, adminId);
                try (ResultSet rs = st.executeQuery()) {
                    List<ClientSummary> clients = new ArrayList<>();
                    while (rs.next()) {
                        clients.add(new ClientSummary(rs.getInt("userID"), rs.getString("fullName"),
                                rs.getString("email"), rs.getString("phoneNumber"), rs.getString("address"),
                                "active".equals(rs.getString("status")), dateTime(rs, "createdAt"),
                                rs.getInt("documents"), rs.getInt("pending"), dateTime(rs, "lastSubmission")));
                    }
                    return clients;
                }
            }
        });
    }

    public void setClientActive(int adminId, int clientId, boolean active) {
        db.call(c -> {
            try (CallableStatement st = c.prepareCall("{call dbo.spSetClientStatus(?,?,?)}")) {
                st.setInt(1, adminId);
                st.setInt(2, clientId);
                st.setString(3, active ? "active" : "inactive");
                st.execute();
                return null;
            }
        });
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
