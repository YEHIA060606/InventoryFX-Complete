package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ReportsController {

    @FXML private Label totalProductsLabel;
    @FXML private Label totalSuppliersLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalStockLabel;

    @FXML
    public void initialize() {
        totalProductsLabel.setText(String.valueOf(totalProducts()));
        totalSuppliersLabel.setText(String.valueOf(totalSuppliers()));
        totalUsersLabel.setText(String.valueOf(totalUsers()));
        totalStockLabel.setText(String.valueOf(totalStock()));
    }

    private int simpleCount(String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            System.err.println("Erreur simpleCount : " + e.getMessage());
        }
        return 0;
    }

    private int totalProducts() {
        return simpleCount("SELECT COUNT(*) FROM produits");
    }

    private int totalSuppliers() {
        return simpleCount("SELECT COUNT(*) FROM fournisseurs");
    }

    private int totalUsers() {
        return simpleCount("SELECT COUNT(*) FROM users");
    }

    private int totalStock() {
        return simpleCount("SELECT COALESCE(SUM(quantite),0) FROM produits");
    }
}
