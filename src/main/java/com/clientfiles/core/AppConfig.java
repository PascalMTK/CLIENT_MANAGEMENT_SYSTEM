package com.clientfiles.core;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Database settings, read from config.properties in the working directory.
 * System properties with the same names take precedence.
 */
public record AppConfig(String url, String user, String password) {

    static final String DEFAULT_URL = "jdbc:sqlserver://localhost:1433"
            + ";databaseName=CLIENT_FILE_MANAGEMENT_SYSTEM"
            + ";integratedSecurity=true;encrypt=true;trustServerCertificate=true;loginTimeout=5";

    public static AppConfig load() {
        return load(Path.of("config.properties"));
    }

    static AppConfig load(Path path) {
        Properties settings = new Properties();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                settings.load(reader);
            } catch (IOException e) {
                throw new AppException("config.properties could not be read: " + e.getMessage());
            }
        }
        return new AppConfig(
                value(settings, "db.url", DEFAULT_URL),
                value(settings, "db.user", ""),
                value(settings, "db.password", ""));
    }

    private static String value(Properties settings, String key, String fallback) {
        return System.getProperty(key, settings.getProperty(key, fallback)).trim();
    }
}
