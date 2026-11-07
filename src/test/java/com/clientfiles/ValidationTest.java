package com.clientfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clientfiles.core.DocStatus;
import com.clientfiles.core.Document;
import com.clientfiles.core.Format;
import com.clientfiles.core.Uploads;
import com.clientfiles.core.Validation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValidationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    @Test
    void email() {
        assertNull(Validation.email(" name@example.com "));
        assertNotNull(Validation.email(""));
        assertNotNull(Validation.email("name@example"));
        assertNotNull(Validation.email("a b@example.com"));
        assertEquals("name@example.com", Validation.normalizeEmail("  Name@Example.COM "));
    }

    @Test
    void phoneIsOptionalButMustLookLikeOne() {
        assertNull(Validation.phone(""));
        assertNull(Validation.phone("+243 812 345 678"));
        assertNotNull(Validation.phone("call me"));
    }

    @Test
    void expiryDateMustBeValidAndInTheFuture() {
        assertNull(Validation.expiryDate("", TODAY));
        assertNull(Validation.expiryDate("2026-06-16", TODAY));
        assertNotNull(Validation.expiryDate("2026-06-15", TODAY));
        assertNotNull(Validation.expiryDate("2026-02-31", TODAY));
        assertNotNull(Validation.expiryDate("soon", TODAY));
    }

    @Test
    void uploadsRejectDangerousAndEmptyFiles(@TempDir Path dir) throws IOException {
        Path good = Files.writeString(dir.resolve("Report.PDF"), "content");
        Path bad = Files.writeString(dir.resolve("setup.exe"), "content");
        Path empty = Files.createFile(dir.resolve("empty.pdf"));
        assertNull(Uploads.problemWith(good));
        assertNotNull(Uploads.problemWith(bad));
        assertNotNull(Uploads.problemWith(empty));
        assertNotNull(Uploads.problemWith(dir.resolve("missing.pdf")));
    }

    @Test
    void expiryStates() {
        Document overdue = document(TODAY.minusDays(1));
        Document soon = document(TODAY.plusDays(Document.EXPIRY_WARNING_DAYS));
        Document later = document(TODAY.plusDays(Document.EXPIRY_WARNING_DAYS + 1));
        assertTrue(overdue.isExpired(TODAY));
        assertFalse(overdue.isExpiringSoon(TODAY));
        assertTrue(soon.isExpiringSoon(TODAY));
        assertFalse(later.isExpiringSoon(TODAY));
    }

    @Test
    void formatting() {
        assertEquals("GM", Format.initials("Grace  Mbuyi"));
        assertEquals("M", Format.initials("Madonna"));
        assertEquals("?", Format.initials("  "));
        assertEquals("1.5 MB", Format.size(1_572_864));
        assertEquals("in 3 days", Format.daysUntil(TODAY.plusDays(3), TODAY));
        assertEquals("1 day ago", Format.daysUntil(TODAY.minusDays(1), TODAY));
    }

    private static Document document(LocalDate expires) {
        return new Document(1, "a.pdf", "Report", "PDF", 1, LocalDateTime.now(), expires, null,
                DocStatus.PENDING, null, null, 1, "c", "c@x.y", null);
    }
}
