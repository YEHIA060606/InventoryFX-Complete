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
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Product;
import util.DatabaseConnection;
import util.WindowUtil;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class StockController {

    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String>  colNom;
    @FXML private TableColumn<Product, Double>  colPrix;
    @FXML private TableColumn<Product, Integer> colQuantite;
    @FXML private TableColumn<Product, Integer> colFournisseur;

    @FXML private TextField qttField;
    @FXML private ComboBox<String> typeBox;
    @FXML private TextField searchField;

    private ObservableList<Product> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNom()));
        colPrix.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getPrix()).asObject());
        colQuantite.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantite()).asObject());
        colFournisseur.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getFournisseurId()).asObject());

        loadProductsFromDb();

        FilteredList<Product> filtered = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal == null ? "" : newVal.toLowerCase();
            filtered.setPredicate(p -> {
                if (filter.isEmpty()) return true;
                return String.valueOf(p.getId()).contains(filter)
                        || p.getNom().toLowerCase().contains(filter)
                        || String.valueOf(p.getFournisseurId()).contains(filter);
            });
        });

        SortedList<Product> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(productTable.comparatorProperty());
        productTable.setItems(sorted);

        typeBox.setItems(FXCollections.observableArrayList("ENTREE", "SORTIE"));
    }

    private void loadProductsFromDb() {
        masterData.clear();
        String sql = "SELECT * FROM produits";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Product p = new Product(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getDouble("prix"),
                        rs.getInt("quantite"),
                        rs.getInt("fournisseur_id")
                );
                masterData.add(p);
            }

        } catch (Exception e) {
            System.err.println("Erreur loadProductsFromDb : " + e.getMessage());
        }
    }

    private void updateProductInDb(Product p) {
        String sql = "UPDATE produits SET nom=?, prix=?, quantite=?, fournisseur_id=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getNom());
            ps.setDouble(2, p.getPrix());
            ps.setInt(3, p.getQuantite());
            ps.setInt(4, p.getFournisseurId());
            ps.setInt(5, p.getId());

            ps.executeUpdate();

        } catch (Exception e) {
            System.err.println("Erreur updateProductInDb : " + e.getMessage());
        }
    }

    private void insertStockMovement(int productId, String type, int quantite, LocalDate date) {
        String sql = "INSERT INTO stock_movements (product_id, type, quantite, date_mouvement) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);
            ps.setString(2, type);
            ps.setInt(3, quantite);
            ps.setDate(4, Date.valueOf(date));

            ps.executeUpdate();

        } catch (Exception e) {
            System.err.println("Erreur insertStockMovement : " + e.getMessage());
        }
    }

    private void refreshData() {
        loadProductsFromDb();
    }

    @FXML
    private void applyMovement() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        String type = typeBox.getValue();

        if (selected == null || type == null || qttField.getText().isEmpty())
            return;

        try {
            int qtt = Integer.parseInt(qttField.getText());
            if (qtt <= 0) return;

            int newQtt = selected.getQuantite();

            if ("ENTREE".equalsIgnoreCase(type)) {
                newQtt += qtt;
            } else if ("SORTIE".equalsIgnoreCase(type)) {
                if (qtt > newQtt) {
                    // pas assez de stock
                    return;
                }
                newQtt -= qtt;
            } else {
                return;
            }

            selected.setQuantite(newQtt);
            updateProductInDb(selected);

            insertStockMovement(
                    selected.getId(),
                    type.toUpperCase(),
                    qtt,
                    LocalDate.now()
            );

            refreshData();
            qttField.clear();
            typeBox.getSelectionModel().clearSelection();

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm()
            );

            Stage stage = (Stage) productTable.getScene().getWindow();
            WindowUtil.switchScene(stage, scene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void exportCSV() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exporter le stock en CSV");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichier CSV", "*.csv")
            );

            Stage stage = (Stage) productTable.getScene().getWindow();
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;

            try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8)) {
                pw.println("ID;Nom;Prix;Quantite;FournisseurID");

                for (Product p : productTable.getItems()) {
                    pw.println(p.getId() + ";" +
                            p.getNom() + ";" +
                            p.getPrix() + ";" +
                            p.getQuantite() + ";" +
                            p.getFournisseurId());
                }
            }

            System.out.println("Export CSV stock terminé.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
