package com.clientfiles.data;

import com.clientfiles.core.AppConfig;
import com.clientfiles.core.AppException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Opens connections and turns driver errors into messages fit for the interface. */
public final class Database {
    private static final Logger LOG = Logger.getLogger(Database.class.getName());
    /** Numbers from this one up are raised by our own procedures, with readable messages. */
    private static final int FIRST_BUSINESS_ERROR = 50000;

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private final AppConfig config;

    public Database(AppConfig config) {
        this.config = config;
    }

    public <T> T call(Work<T> work) {
        try (Connection connection = open()) {
            return work.run(connection);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    private Connection open() throws SQLException {
        return config.user().isEmpty()
                ? DriverManager.getConnection(config.url())
                : DriverManager.getConnection(config.url(), config.user(), config.password());
    }

    private static AppException translate(SQLException e) {
        if (e.getErrorCode() >= FIRST_BUSINESS_ERROR) {
            return new AppException(e.getMessage());
        }
        LOG.log(Level.WARNING, "Database call failed", e);
        String state = e.getSQLState();
        boolean connection = e.getErrorCode() == 0 || (state != null && state.startsWith("08"))
                || e.getErrorCode() == 4060 || e.getErrorCode() == 18456;
        return new AppException(connection
                ? "The database can't be reached. Check that SQL Server is running, then try again."
                : "The database reported a problem. Please try again.", e, connection);
    }
}
