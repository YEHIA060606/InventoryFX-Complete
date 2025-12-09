package app;

import util.DatabaseConnection;

import java.sql.Connection;

public class TestDb {

    public static void main(String[] args) {
        Connection cnx = DatabaseConnection.getConnection();
        if (cnx != null) {
            System.out.println("Connexion OK à la base inventoryfx ✅");
        } else {
            System.out.println("Échec de connexion ❌");
        }
    }
}
