import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectToServer {
    public static void main(String[] args) {
        String url = "jdbc:sqlserver://DESKTOP-2V8MSJT\\SQLEXPRESS:1434;databaseName=CLIENT_FILE_MANAGEMENT_SYSTEM";
        String user = "DESKTOP-2V8MSJT/Pascal"; // Use your SQL Server username
        String password = ""; // Use your SQL Server password

        try (@SuppressWarnings("unused")
Connection connection = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connection successful!");
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}
