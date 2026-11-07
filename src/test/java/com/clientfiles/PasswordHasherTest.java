package com.clientfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clientfiles.core.PasswordHasher;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {
    @Test
    void acceptsTheRightPasswordOnly() {
        String stored = PasswordHasher.hash("correct horse".toCharArray());
        assertTrue(PasswordHasher.matches("correct horse".toCharArray(), stored));
        assertFalse(PasswordHasher.matches("wrong horse".toCharArray(), stored));
    }

    @Test
    void samePasswordGivesDifferentHashes() {
        assertNotEquals(PasswordHasher.hash("same".toCharArray()), PasswordHasher.hash("same".toCharArray()));
    }

    @Test
    void malformedStoredValuesNeverMatch() {
        assertFalse(PasswordHasher.matches("x".toCharArray(), null));
        assertFalse(PasswordHasher.matches("x".toCharArray(), ""));
        assertFalse(PasswordHasher.matches("x".toCharArray(), "pbkdf2$abc$def$ghi"));
    }
}
