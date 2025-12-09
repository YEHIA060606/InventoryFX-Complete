package controller;

import javafx.beans.property.SimpleDoubleProperty;
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
import model.Product;
import model.Supplier;
import util.DatabaseConnection;
import util.WindowUtil;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class ProductsController {

    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String>  colNom;
    @FXML private TableColumn<Product, Double>  colPrix;
    @FXML private TableColumn<Product, Integer> colQuantite;
    @FXML private TableColumn<Product, Integer> colFournisseur;

    @FXML private TextField nomField;
    @FXML private TextField prixField;
    @FXML private TextField quantiteField;
    @FXML private ComboBox<Supplier> fournisseurBox;
    @FXML private TextField searchField;

    /** Liste complète des produits affichés dans le tableau */
    private final ObservableList<Product> masterData = FXCollections.observableArrayList();

    /** Liste des fournisseurs pour la combo */
    private final ObservableList<Supplier> suppliersData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Colonnes
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNom()));
        colPrix.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getPrix()).asObject());
        colQuantite.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantite()).asObject());
        colFournisseur.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getFournisseurId()).asObject());

        // Chargement initial depuis la BDD
        loadSuppliersFromDB();
        loadProductsFromDB();

        // Combo fournisseurs
        fournisseurBox.setItems(suppliersData);

        // Recherche filtre + tri
        FilteredList<Product> filtered = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal == null ? "" : newVal.toLowerCase();
            filtered.setPredicate(p -> {
                if (filter.isEmpty()) return true;
                return String.valueOf(p.getId()).contains(filter)
                        || p.getNom().toLowerCase().contains(filter)
                        || String.valueOf(p.getPrix()).contains(filter);
            });
        });

        SortedList<Product> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(productsTable.comparatorProperty());
        productsTable.setItems(sorted);

        // Remplir le formulaire quand on clique une ligne
        productsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                nomField.setText(sel.getNom());
                prixField.setText(String.valueOf(sel.getPrix()));
                quantiteField.setText(String.valueOf(sel.getQuantite()));

                // pré-sélection du fournisseur dans la combo
                for (Supplier s : suppliersData) {
                    if (s.getId() == sel.getFournisseurId()) {
                        fournisseurBox.getSelectionModel().select(s);
                        break;
                    }
                }
            }
        });
    }

    /* =====================  ACCÈS DIRECT BDD  ===================== */

    private void loadProductsFromDB() {
        masterData.clear();
        String sql = "SELECT * FROM produits";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st   = conn.createStatement();
             ResultSet rs   = st.executeQuery(sql)) {

            while (rs.next()) {
                masterData.add(new Product(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getDouble("prix"),
                        rs.getInt("quantite"),
                        rs.getInt("fournisseur_id")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur BDD", "Erreur lors du chargement des produits.");
        }
    }

    private void loadSuppliersFromDB() {
        suppliersData.clear();
        String sql = "SELECT * FROM fournisseurs";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st   = conn.createStatement();
             ResultSet rs   = st.executeQuery(sql)) {

            while (rs.next()) {
                suppliersData.add(new Supplier(
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

    private boolean insertProductInDB(Product p) {
        String sql = "INSERT INTO produits(nom, prix, quantite, fournisseur_id) VALUES (?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getNom());
            ps.setDouble(2, p.getPrix());
            ps.setInt(3, p.getQuantite());
            ps.setInt(4, p.getFournisseurId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur BDD", "Impossible d'ajouter le produit.");
            return false;
        }
    }

    private boolean updateProductInDB(Product p) {
        String sql = "UPDATE produits SET nom=?, prix=?, quantite=?, fournisseur_id=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getNom());
            ps.setDouble(2, p.getPrix());
            ps.setInt(3, p.getQuantite());
            ps.setInt(4, p.getFournisseurId());
            ps.setInt(5, p.getId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur BDD", "Impossible de modifier le produit.");
            return false;
        }
    }

    private boolean deleteProductFromDB(int id) {
        String sql = "DELETE FROM produits WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            // Contrainte de clé étrangère (historique, etc.)
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key")) {
                showAlert(
                        "Suppression impossible",
                        "Ce produit est encore référencé dans les mouvements de stock.\n" +
                                "Supprimez d'abord l'historique ou désactivez la contrainte."
                );
            } else {
                showAlert("Erreur BDD", "Impossible de supprimer le produit.");
            }
            e.printStackTrace();
            return false;
        }
    }

    /* =====================  FORMULAIRE  ===================== */

    private boolean validateProductForm() {
        String nom    = nomField.getText().trim();
        String prixTxt = prixField.getText().trim();
        String qttTxt  = quantiteField.getText().trim();
        Supplier s    = fournisseurBox.getSelectionModel().getSelectedItem();

        if (nom.isEmpty() || prixTxt.isEmpty() || qttTxt.isEmpty() || s == null) {
            showAlert("Champs manquants", "Veuillez remplir tous les champs et choisir un fournisseur.");
            return false;
        }

        try {
            Double.parseDouble(prixTxt);
            Integer.parseInt(qttTxt);
        } catch (NumberFormatException e) {
            showAlert("Valeurs invalides", "Prix et quantité doivent être des nombres valides.");
            return false;
        }

        return true;
    }

    /* =====================  ACTIONS BOUTONS  ===================== */

    @FXML
    void addProduct() {
        if (!validateProductForm()) return;

        String nom = nomField.getText().trim();
        double prix = Double.parseDouble(prixField.getText().trim());
        int qtt = Integer.parseInt(quantiteField.getText().trim());
        Supplier s = fournisseurBox.getSelectionModel().getSelectedItem();

        Product p = new Product(nom, prix, qtt, s.getId());

        if (insertProductInDB(p)) {
            loadProductsFromDB();
        }
    }

    @FXML
    void deleteProduct() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Aucun produit sélectionné", "Veuillez sélectionner un produit à supprimer.");
            return;
        }

        if (deleteProductFromDB(selected.getId())) {
            loadProductsFromDB();
        }
    }

    @FXML
    void updateProduct() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Aucun produit sélectionné", "Veuillez sélectionner un produit dans le tableau.");
            return;
        }

        if (!validateProductForm()) return;

        String nom = nomField.getText().trim();
        double prix = Double.parseDouble(prixField.getText().trim());
        int qtt = Integer.parseInt(quantiteField.getText().trim());
        Supplier s = fournisseurBox.getSelectionModel().getSelectedItem();

        selected.setNom(nom);
        selected.setPrix(prix);
        selected.setQuantite(qtt);
        selected.setFournisseurId(s.getId());

        if (updateProductInDB(selected)) {
            loadProductsFromDB();
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
            Stage stage = (Stage) productsTable.getScene().getWindow();
            WindowUtil.switchScene(stage, scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void exportCSV() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exporter produits en CSV");
            fileChooser.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));

            Stage stage = (Stage) productsTable.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            if (file == null) return;

            try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8)) {
                pw.println("ID;Nom;Prix;Quantite;Fournisseur");
                for (Product p : productsTable.getItems()) {
                    pw.println(p.getId() + ";" +
                            p.getNom() + ";" +
                            p.getPrix() + ";" +
                            p.getQuantite() + ";" +
                            p.getFournisseurId());
                }
            }
            System.out.println("Export CSV terminé.");
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
