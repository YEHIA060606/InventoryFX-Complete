package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Supplier;
import util.DatabaseConnection;
import util.WindowUtil;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class SupplierController {

    @FXML private TableView<Supplier> suppliersTable;
    @FXML private TableColumn<Supplier, Integer> colId;
    @FXML private TableColumn<Supplier, String>  colNom;
    @FXML private TableColumn<Supplier, String>  colEmail;
    @FXML private TableColumn<Supplier, String>  colTel;

    @FXML private TextField nomField;
    @FXML private TextField mailField;
    @FXML private TextField telField;
    @FXML private TextField searchField;

    private final ObservableList<Supplier> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNom()));
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        colTel.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTelephone()));

        loadSuppliersFromDB();

        FilteredList<Supplier> filtered = new FilteredList<>(masterData, s -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String f = newVal == null ? "" : newVal.toLowerCase();
            filtered.setPredicate(s -> {
                if (f.isEmpty()) return true;
                return String.valueOf(s.getId()).contains(f)
                        || s.getNom().toLowerCase().contains(f)
                        || s.getEmail().toLowerCase().contains(f)
                        || s.getTelephone().toLowerCase().contains(f);
            });
        });

        SortedList<Supplier> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(suppliersTable.comparatorProperty());
        suppliersTable.setItems(sorted);

        // Remplir les champs sur sélection
        suppliersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                nomField.setText(sel.getNom());
                mailField.setText(sel.getEmail());
                telField.setText(sel.getTelephone());
            }
        });
    }

    /* =====================  ACCÈS DIRECT BDD  ===================== */

    private void loadSuppliersFromDB() {
        masterData.clear();
        String sql = "SELECT * FROM fournisseurs";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st   = conn.createStatement();
             ResultSet rs   = st.executeQuery(sql)) {

            while (rs.next()) {
                masterData.add(new Supplier(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("telephone")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur BDD", "Erreur lors du chargement des fournisseurs.");
        }
    }

    private boolean insertSupplierInDB(Supplier s) {
        String sql = "INSERT INTO fournisseurs(nom, email, telephone) VALUES (?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, s.getNom());
            ps.setString(2, s.getEmail());
            ps.setString(3, s.getTelephone());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur BDD", "Impossible d'ajouter le fournisseur.");
            return false;
        }
    }

    private boolean updateSupplierInDB(Supplier s) {
        String sql = "UPDATE fournisseurs SET nom=?, email=?, telephone=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, s.getNom());
            ps.setString(2, s.getEmail());
            ps.setString(3, s.getTelephone());
            ps.setInt(4, s.getId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur BDD", "Impossible de modifier le fournisseur.");
            return false;
        }
    }

    private boolean deleteSupplierFromDB(int id) {
        String sql = "DELETE FROM fournisseurs WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            // cas fournisseur utilisé par un produit
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key")) {
                showAlert(
                        "Suppression impossible",
                        "Ce fournisseur est encore lié à un ou plusieurs produits.\n" +
                                "Modifiez ou supprimez d'abord les produits avant de supprimer le fournisseur."
                );
            } else {
                showAlert("Erreur BDD", "Impossible de supprimer le fournisseur.");
            }
            e.printStackTrace();
            return false;
        }
    }

    /* =====================  ACTIONS BOUTONS  ===================== */

    @FXML
    void addSupplier() {
        String nom   = nomField.getText().trim();
        String email = mailField.getText().trim();
        String tel   = telField.getText().trim();

        if (nom.isEmpty() || email.isEmpty() || tel.isEmpty()) {
            showAlert("Champs manquants", "Veuillez remplir tous les champs pour ajouter un fournisseur.");
            return;
        }

        Supplier s = new Supplier(nom, email, tel);
        if (insertSupplierInDB(s)) {
            loadSuppliersFromDB();
        }
    }

    @FXML
    void deleteSupplier() {
        Supplier s = suppliersTable.getSelectionModel().getSelectedItem();
        if (s == null) {
            showAlert("Aucun fournisseur sélectionné", "Veuillez sélectionner un fournisseur à supprimer.");
            return;
        }

        if (deleteSupplierFromDB(s.getId())) {
            loadSuppliersFromDB();
        }
    }

    @FXML
    void updateSupplier() {
        Supplier s = suppliersTable.getSelectionModel().getSelectedItem();
        if (s == null) {
            showAlert("Aucun fournisseur sélectionné", "Veuillez sélectionner un fournisseur à modifier.");
            return;
        }

        String nom   = nomField.getText().trim();
        String email = mailField.getText().trim();
        String tel   = telField.getText().trim();

        if (nom.isEmpty() || email.isEmpty() || tel.isEmpty()) {
            showAlert("Champs manquants", "Veuillez remplir tous les champs pour modifier le fournisseur.");
            return;
        }

        s.setNom(nom);
        s.setEmail(email);
        s.setTelephone(tel);

        if (updateSupplierInDB(s)) {
            loadSuppliersFromDB();
        }
    }

    @FXML
    void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm()
            );
            Stage stage = (Stage) suppliersTable.getScene().getWindow();
            WindowUtil.switchScene(stage, scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void exportCSV() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exporter les fournisseurs en CSV");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichier CSV", "*.csv")
            );

            Stage stage = (Stage) suppliersTable.getScene().getWindow();
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;

            try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8)) {
                pw.println("ID;Nom;Email;Téléphone");

                for (Supplier s : suppliersTable.getItems()) {
                    pw.println(s.getId() + ";" +
                            s.getNom() + ";" +
                            s.getEmail() + ";" +
                            s.getTelephone());
                }
            }

            System.out.println("Export CSV fournisseurs terminé.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'export CSV.");
        }
    }

    /* =====================  UTILS  ===================== */

    private void showAlert(String titre, String contenu) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(contenu);
        alert.showAndWait();
    }
}
