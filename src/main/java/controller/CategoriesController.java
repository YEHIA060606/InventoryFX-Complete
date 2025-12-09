package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Category;
import util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CategoriesController {

    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Integer> colId;
    @FXML private TableColumn<Category, String> colNom;

    @FXML private TextField nomField;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colNom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNom()));

        loadCategories();
    }

    private void loadCategories() {
        categoryTable.setItems(FXCollections.observableArrayList());

        String sql = "SELECT * FROM categories";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Category c = new Category(
                        rs.getInt("id"),
                        rs.getString("nom")
                );
                categoryTable.getItems().add(c);
            }

        } catch (Exception e) {
            System.err.println("Erreur loadCategories : " + e.getMessage());
        }
    }

    private void insertCategory(Category c) {
        String sql = "INSERT INTO categories (nom) VALUES (?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getNom());
            ps.executeUpdate();

        } catch (Exception e) {
            System.err.println("Erreur insertCategory : " + e.getMessage());
        }
    }

    private void updateCategoryInDb(Category c) {
        String sql = "UPDATE categories SET nom=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getNom());
            ps.setInt(2, c.getId());
            ps.executeUpdate();

        } catch (Exception e) {
            System.err.println("Erreur updateCategoryInDb : " + e.getMessage());
        }
    }

    private void deleteCategoryFromDb(int id) {
        String sql = "DELETE FROM categories WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            System.err.println("Erreur deleteCategoryFromDb : " + e.getMessage());
        }
    }

    @FXML
    void addCategory() {
        String nom = nomField.getText().trim();
        if (nom.isEmpty()) return;

        Category c = new Category(nom);
        insertCategory(c);
        loadCategories();
    }

    @FXML
    void updateCategory() {
        Category c = categoryTable.getSelectionModel().getSelectedItem();
        if (c != null) {
            String nom = nomField.getText().trim();
            if (nom.isEmpty()) return;

            c.setNom(nom);
            updateCategoryInDb(c);
            loadCategories();
        }
    }

    @FXML
    void deleteCategory() {
        Category c = categoryTable.getSelectionModel().getSelectedItem();
        if (c != null) {
            deleteCategoryFromDb(c.getId());
            loadCategories();
        }
    }
}
