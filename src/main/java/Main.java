import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        Connection conn = DBConnection.getDBConnection();
        if (conn != null) {
            System.out.println("Connexion OK !");
        }
    }
}