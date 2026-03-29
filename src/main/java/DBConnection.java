import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    public static Connection getDBConnection() {
        Connection connection = null;

        String jdbcUrl = "jdbc:postgresql://localhost:5432/mini_dish_db";
        String username = "mini_dish_db_manager";
        String password = "123456";

        try {
            Class.forName("org.postgresql.Driver");

            connection = DriverManager.getConnection(jdbcUrl, username, password);

            System.out.println("Connexion reussie a la base de donnees !");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver PostgreSQL non trouve !");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la connexion a la base !");
            e.printStackTrace();
        }

        return connection;
    }

    public static void main(String[] args) {
        Connection conn = getDBConnection();
        if (conn != null) {
            System.out.println("Connexion OK !");
        }
    }
}