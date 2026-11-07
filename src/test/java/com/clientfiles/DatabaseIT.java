package com.clientfiles;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clientfiles.core.AppConfig;
import com.clientfiles.core.AppException;
import com.clientfiles.core.AuthService;
import com.clientfiles.core.ClientSummary;
import com.clientfiles.core.DocStatus;
import com.clientfiles.core.Document;
import com.clientfiles.core.User;
import com.clientfiles.data.Database;
import com.clientfiles.data.DocumentRepository;
import com.clientfiles.data.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the whole data layer against an empty database given by -Dtest.db.url.
 * Skipped otherwise, because it creates accounts and documents.
 */
@EnabledIfSystemProperty(named = "test.db.url", matches = ".+")
@TestMethodOrder(OrderAnnotation.class)
class DatabaseIT {
    private static final char[] PW = "Password1".toCharArray();
    private final Database db = new Database(new AppConfig(System.getProperty("test.db.url"), "", ""));
    private final UserRepository users = new UserRepository(db);
    private final DocumentRepository documents = new DocumentRepository(db);
    private final AuthService auth = new AuthService(users);

    @Test
    @Order(1)
    void firstAdminCanOnlyBeCreatedOnce() {
        assertFalse(users.hasAdmin());
        auth.createFirstAdmin("Ada Admin", "ADA@test.local", PW);
        assertTrue(users.hasAdmin());
        AppException e = assertThrows(AppException.class, () -> auth.createFirstAdmin("Eve", "eve@test.local", PW));
        assertEquals("An administrator already exists.", e.getMessage());
    }

    @Test
    @Order(2)
    void signUpAndSignIn() {
        auth.signUp("Cleo Client", "cleo@test.local", "+243 811 000 000", "", PW);
        auth.signUp("Otis Other", "otis@test.local", "", "", PW);
        assertEquals("An account with this email already exists.",
                assertThrows(AppException.class, () -> auth.signUp("Copy", "CLEO@test.local", "", "", PW)).getMessage());

        User cleo = auth.signIn("Cleo@Test.Local ", PW);
        assertEquals("Cleo Client", cleo.fullName());
        assertFalse(cleo.isAdmin());
        assertTrue(auth.signIn("ada@test.local", PW).isAdmin());

        String wrong = "Incorrect email or password.";
        assertEquals(wrong, assertThrows(AppException.class,
                () -> auth.signIn("cleo@test.local", "nope".toCharArray())).getMessage());
        assertEquals(wrong, assertThrows(AppException.class, () -> auth.signIn("ghost@test.local", PW)).getMessage());
    }

    @Test
    @Order(3)
    void documentLifecycleAndPermissions(@TempDir Path dir) throws IOException {
        User admin = auth.signIn("ada@test.local", PW);
        User cleo = auth.signIn("cleo@test.local", PW);
        User otis = auth.signIn("otis@test.local", PW);

        byte[] bytes = new byte[300_000];
        new Random(7).nextBytes(bytes);
        Path file = Files.write(dir.resolve("Résumé annuel.pdf"), bytes);
        int id = documents.create(cleo.id(), file, "Report", LocalDate.now().plusDays(10), "Sup Ervisor");
        int rejected = documents.create(cleo.id(), file, "Report", null, "");

        assertEquals(2, documents.visibleTo(cleo.id()).size());
        assertEquals(2, documents.visibleTo(admin.id()).size());
        assertEquals(0, documents.visibleTo(otis.id()).size());
        Document doc = documents.visibleTo(cleo.id()).stream().filter(d -> d.id() == id).findFirst().orElseThrow();
        assertEquals("Résumé annuel.pdf", doc.name());
        assertEquals("PDF", doc.type());
        assertEquals(300_000, doc.size());
        assertEquals(DocStatus.PENDING, doc.status());
        assertEquals("Cleo Client", doc.clientName());

        Path copy = dir.resolve("copy.pdf");
        documents.saveContent(id, admin.id(), copy);
        assertArrayEquals(bytes, Files.readAllBytes(copy));
        assertThrows(AppException.class, () -> documents.saveContent(id, otis.id(), dir.resolve("stolen.pdf")));

        assertThrows(AppException.class, () -> documents.review(id, cleo.id(), DocStatus.APPROVED, null));
        documents.review(id, admin.id(), DocStatus.APPROVED, null);
        documents.review(rejected, admin.id(), DocStatus.REJECTED, "Unreadable");
        Document approved = documents.visibleTo(cleo.id()).stream().filter(d -> d.id() == id).findFirst().orElseThrow();
        assertEquals(DocStatus.APPROVED, approved.status());
        assertEquals("Ada Admin", approved.reviewerName());
        assertNotNull(approved.reviewedAt());

        assertThrows(AppException.class, () -> documents.delete(id, cleo.id()), "approved stays on record");
        assertThrows(AppException.class, () -> documents.delete(rejected, otis.id()), "not the owner");
        documents.delete(rejected, cleo.id());
        assertEquals(1, documents.visibleTo(cleo.id()).size());
    }

    @Test
    @Order(4)
    void profileAndPassword() {
        User cleo = auth.signIn("cleo@test.local", PW);
        users.updateProfile(cleo.id(), "Cleo Renamed", cleo.email(), "", "Kinshasa");
        assertEquals("Cleo Renamed", auth.signIn("cleo@test.local", PW).fullName());
        assertThrows(AppException.class,
                () -> users.updateProfile(cleo.id(), "x", "otis@test.local", "", ""), "email taken");

        assertThrows(AppException.class,
                () -> auth.changePassword(cleo.id(), "bad".toCharArray(), "NewPassword1".toCharArray()));
        auth.changePassword(cleo.id(), PW, "NewPassword1".toCharArray());
        auth.signIn("cleo@test.local", "NewPassword1".toCharArray());
        assertThrows(AppException.class, () -> auth.signIn("cleo@test.local", PW));
    }

    @Test
    @Order(5)
    void deactivatedClientsAreLockedOut() {
        User admin = auth.signIn("ada@test.local", PW);
        User otis = auth.signIn("otis@test.local", PW);
        assertThrows(AppException.class, () -> users.clients(otis.id()), "clients can't list clients");

        List<ClientSummary> clients = users.clients(admin.id());
        assertEquals(2, clients.size());
        assertEquals(1, clients.get(0).documents());
        assertEquals(0, clients.get(1).pending());

        users.setClientActive(admin.id(), otis.id(), false);
        assertTrue(assertThrows(AppException.class,
                () -> auth.signIn("otis@test.local", PW)).getMessage().contains("deactivated"));
        assertThrows(AppException.class, () -> documents.visibleTo(otis.id()));
        users.setClientActive(admin.id(), otis.id(), true);
        auth.signIn("otis@test.local", PW);
    }

    @Test
    @Order(6)
    void unreachableServerIsReportedAsSuch() {
        Database down = new Database(new AppConfig(
                "jdbc:sqlserver://localhost:1;databaseName=x;encrypt=false;loginTimeout=1", "u", "p"));
        AppException e = assertThrows(AppException.class, () -> new UserRepository(down).hasAdmin());
        assertTrue(e.isConnectionProblem());
    }
}
