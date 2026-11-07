package com.clientfiles.data;

import com.clientfiles.core.AppException;
import com.clientfiles.core.DocStatus;
import com.clientfiles.core.Document;
import com.clientfiles.core.Uploads;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DocumentRepository {
    private final Database db;

    public DocumentRepository(Database db) {
        this.db = db;
    }

    public int create(int clientId, Path file, String category, LocalDate expires, String supervisor) {
        String name = file.getFileName().toString();
        String type = Uploads.extension(name).toUpperCase(Locale.ROOT);
        return db.call(c -> {
            try (CallableStatement st = c.prepareCall("{call dbo.spCreateFile(?,?,?,?,?,?,?)}");
                 InputStream content = Files.newInputStream(file)) {
                st.setInt(1, clientId);
                st.setString(2, name);
                st.setString(3, category);
                st.setString(4, type);
                st.setDate(5, expires == null ? null : Date.valueOf(expires));
                if (supervisor == null || supervisor.isBlank()) {
                    st.setNull(6, Types.NVARCHAR);
                } else {
                    st.setString(6, supervisor);
                }
                st.setBinaryStream(7, content, Files.size(file));
                try (ResultSet rs = st.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            } catch (IOException e) {
                throw new AppException("The file could not be read: " + e.getMessage());
            }
        });
    }

    /** Every document for an administrator, the person's own for a client. */
    public List<Document> visibleTo(int actorId) {
        return db.call(c -> {
            try (CallableStatement st = c.prepareCall("{call dbo.spGetFiles(?)}")) {
                st.setInt(1, actorId);
                try (ResultSet rs = st.executeQuery()) {
                    List<Document> documents = new ArrayList<>();
                    while (rs.next()) {
                        Date expires = rs.getDate("expiringDate");
                        Timestamp reviewed = rs.getTimestamp("reviewedAt");
                        documents.add(new Document(rs.getInt("docID"), rs.getString("docName"),
                                rs.getString("category"), rs.getString("docType"), rs.getLong("fileSize"),
                                rs.getTimestamp("submittedAt").toLocalDateTime(),
                                expires == null ? null : expires.toLocalDate(),
                                rs.getString("supervisorName"), DocStatus.fromDb(rs.getString("status")),
                                rs.getString("reviewNote"), reviewed == null ? null : reviewed.toLocalDateTime(),
                                rs.getInt("clientID"), rs.getString("clientName"), rs.getString("clientMail"),
                                rs.getString("reviewerName")));
                    }
                    return documents;
                }
            }
        });
    }

    public void saveContent(int docId, int actorId, Path target) {
        db.call(c -> {
            try (CallableStatement st = c.prepareCall("{call dbo.spGetFileContent(?,?)}")) {
                st.setInt(1, docId);
                st.setInt(2, actorId);
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) {
                        throw new AppException("This document no longer exists or isn't available to you.");
                    }
                    try (InputStream content = rs.getBinaryStream(1)) {
                        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new AppException("The file could not be saved: " + e.getMessage());
                    }
                    return null;
                }
            }
        });
    }

    public void review(int docId, int adminId, DocStatus decision, String note) {
        db.call(c -> {
            try (CallableStatement st = c.prepareCall("{call dbo.spReviewFile(?,?,?,?)}")) {
                st.setInt(1, docId);
                st.setInt(2, adminId);
                st.setString(3, decision.dbValue());
                if (note == null || note.isBlank()) {
                    st.setNull(4, Types.NVARCHAR);
                } else {
                    st.setString(4, note);
                }
                st.execute();
                return null;
            }
        });
    }

    public void delete(int docId, int ownerId) {
        db.call(c -> {
            try (CallableStatement st = c.prepareCall("{call dbo.spDeleteFile(?,?)}")) {
                st.setInt(1, docId);
                st.setInt(2, ownerId);
                st.execute();
                return null;
            }
        });
    }
}
