package com.clientfiles.dev;

import com.clientfiles.core.AppConfig;
import com.clientfiles.core.AuthService;
import com.clientfiles.core.DocStatus;
import com.clientfiles.data.Database;
import com.clientfiles.data.DocumentRepository;
import com.clientfiles.data.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;

/**
 * Fills an empty database with sample accounts and documents for demonstrations.
 * Refuses to run once any administrator exists, so it can never touch real data.
 */
public final class DemoData {
    static final String PASSWORD = "Demo1234!";

    private DemoData() {
    }

    private record Sample(String name, String category, int daysToExpiry, String supervisor,
                          DocStatus status, String note, int daysAgo) {
    }

    public static void main(String[] args) throws IOException {
        Database db = new Database(AppConfig.load());
        UserRepository users = new UserRepository(db);
        DocumentRepository documents = new DocumentRepository(db);
        AuthService auth = new AuthService(users);

        if (users.hasAdmin()) {
            System.out.println("This database already has an administrator: nothing was changed.");
            return;
        }
        char[] password = PASSWORD.toCharArray();
        auth.createFirstAdmin("Admin Demo", "admin@demo.test", password);
        auth.signUp("Grace Mbuyi", "grace@demo.test", "+243 812 345 678", "12 Avenue de la Paix, Kinshasa", password);
        auth.signUp("Daniel Ilunga", "daniel@demo.test", "+243 998 112 233", "4 Boulevard du 30 Juin, Lubumbashi", password);
        auth.signUp("Sarah Kabongo", "sarah@demo.test", "", "", password);

        int adminId = users.findByEmail("admin@demo.test").orElseThrow().user().id();
        Path dir = Files.createTempDirectory("client-files-demo");

        seed(db, users, documents, adminId, dir, "grace@demo.test", List.of(
                new Sample("Annual report 2025.pdf", "Report", 240, "Marie Tshibanda", DocStatus.APPROVED, null, 118),
                new Sample("Service agreement.docx", "Contract", 21, "Marie Tshibanda", DocStatus.APPROVED, null, 91),
                new Sample("Invoice 2026-0412.pdf", "Invoice", 0, "Jean Lukusa", DocStatus.PENDING, null, 3),
                new Sample("Quarterly budget.xlsx", "Report", 0, "Jean Lukusa", DocStatus.REJECTED,
                        "Totals in the Q3 sheet don't match the invoices. Please correct them and submit again.", 12)));
        seed(db, users, documents, adminId, dir, "daniel@demo.test", List.of(
                new Sample("Passport scan.png", "Identity", 12, null, DocStatus.APPROVED, null, 150),
                new Sample("Tax certificate.pdf", "Certificate", 180, "Marie Tshibanda", DocStatus.PENDING, null, 1),
                new Sample("Lease contract.pdf", "Contract", 400, "Marie Tshibanda", DocStatus.APPROVED, null, 60),
                new Sample("Invoice 2026-0398.pdf", "Invoice", 0, "Jean Lukusa", DocStatus.APPROVED, null, 33),
                new Sample("Site photos.jpg", "Other", 0, null, DocStatus.PENDING, null, 0)));
        seed(db, users, documents, adminId, dir, "sarah@demo.test", List.of(
                new Sample("Business licence.pdf", "Certificate", 25, "Jean Lukusa", DocStatus.PENDING, null, 2),
                new Sample("Staff list.csv", "Report", 0, null, DocStatus.APPROVED, null, 44),
                new Sample("Insurance policy.pdf", "Contract", 330, "Marie Tshibanda", DocStatus.APPROVED, null, 75)));

        Files.delete(dir);
        System.out.println("Demo data created. Sign in as admin@demo.test or grace@demo.test, password " + PASSWORD);
    }

    private static void seed(Database db, UserRepository users, DocumentRepository documents, int adminId,
                             Path dir, String email, List<Sample> samples) throws IOException {
        int clientId = users.findByEmail(email).orElseThrow().user().id();
        for (Sample s : samples) {
            Path file = dir.resolve(s.name());
            Files.writeString(file, "Sample content of " + s.name());
            LocalDate expires = s.daysToExpiry() == 0 ? null : LocalDate.now().plusDays(s.daysToExpiry());
            int id = documents.create(clientId, file, s.category(), expires, s.supervisor());
            if (s.status() != DocStatus.PENDING) {
                documents.review(id, adminId, s.status(), s.note());
            }
            backdate(db, id, s.daysAgo());
            Files.delete(file);
        }
    }

    private static void backdate(Database db, int docId, int days) {
        db.call(c -> {
            try (PreparedStatement st = c.prepareStatement(
                    "UPDATE dbo.FILES SET submittedAt = DATEADD(day, -?, submittedAt),"
                            + " reviewedAt = DATEADD(day, 1 - ?, reviewedAt) WHERE docID = ?")) {
                st.setInt(1, days);
                st.setInt(2, days);
                st.setInt(3, docId);
                st.execute();
                return null;
            }
        });
    }
}
