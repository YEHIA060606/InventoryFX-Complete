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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.StockMovement;
import util.DatabaseConnection;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class HistoryController {

    @FXML private TableView<StockMovement> historyTable;

    @FXML private TableColumn<StockMovement, Integer> colId;
    @FXML private TableColumn<StockMovement, Integer> colProductId;
    @FXML private TableColumn<StockMovement, String> colType;
    @FXML private TableColumn<StockMovement, Integer> colQuantite;
    @FXML private TableColumn<StockMovement, String> colDate;

    @FXML private TextField searchField;

    private ObservableList<StockMovement> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colProductId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getProductId()).asObject());
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
        colQuantite.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantite()).asObject());
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDateMouvement().toString()));

        loadHistoryFromDb();

        FilteredList<StockMovement> filtered = new FilteredList<>(masterData, m -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = (newVal == null) ? "" : newVal.toLowerCase();

            filtered.setPredicate(m -> {
                if (filter.isEmpty()) return true;

                return String.valueOf(m.getId()).contains(filter)
                        || String.valueOf(m.getProductId()).contains(filter)
                        || m.getType().toLowerCase().contains(filter)
                        || String.valueOf(m.getQuantite()).contains(filter)
                        || m.getDateMouvement().toString().toLowerCase().contains(filter);
            });
        });

        SortedList<StockMovement> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(historyTable.comparatorProperty());
        historyTable.setItems(sorted);
    }

    private void loadHistoryFromDb() {
        masterData.clear();
        String sql = "SELECT * FROM stock_movements ORDER BY date_mouvement DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                StockMovement m = new StockMovement(
                        rs.getInt("id"),
                        rs.getInt("product_id"),
                        rs.getString("type"),
                        rs.getInt("quantite"),
                        rs.getDate("date_mouvement").toLocalDate()
                );
                masterData.add(m);
            }

        } catch (Exception e) {
            System.err.println("Erreur loadHistoryFromDb : " + e.getMessage());
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

            Stage stage = (Stage) historyTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void exportCSV() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exporter l'historique en CSV");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichier CSV", "*.csv")
            );

            Stage stage = (Stage) historyTable.getScene().getWindow();
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;

            try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8)) {
                pw.println("ID;ProduitID;Type;Quantite;Date");

                for (StockMovement m : historyTable.getItems()) {
                    pw.println(m.getId() + ";" +
                            m.getProductId() + ";" +
                            m.getType() + ";" +
                            m.getQuantite() + ";" +
                            m.getDateMouvement());
                }
            }

            System.out.println("Export CSV historique terminé.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
