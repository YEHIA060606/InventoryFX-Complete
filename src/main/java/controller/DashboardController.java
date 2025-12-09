package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import model.User;
import util.UserSession;
import util.WindowUtil;

public class DashboardController {

    @FXML
    private Button productsBtn;

    @FXML
    private Button suppliersBtn;

    @FXML
    private Button stockBtn;

    @FXML
    private Button usersBtn;

    @FXML
    private Button logoutBtn;

    // 🔹 MenuItem du hamburger pour "Utilisateurs"
    @FXML
    private MenuItem usersMenuItem;

    @FXML
    public void initialize() {
        User current = UserSession.getCurrentUser();
        boolean isAdmin = current != null && "ADMIN".equalsIgnoreCase(current.getRole());

        // Bouton caché (pour le controller)
        if (usersBtn != null) {
            usersBtn.setDisable(!isAdmin);
        }

        // MenuItem du hamburger
        if (usersMenuItem != null) {
            // tu peux choisir : désactiver ou cacher
            usersMenuItem.setDisable(!isAdmin);
            // ou, si tu préfères complètement le cacher :
            // usersMenuItem.setVisible(isAdmin);
        }
    }

    @FXML
    private void goProducts() {
        loadPage("products.fxml");
    }

    @FXML
    private void goSuppliers() {
        loadPage("suppliers.fxml");
    }

    @FXML
    private void goStock() {
        loadPage("stock.fxml");
    }

    @FXML
    private void goUsers() {
        User current = UserSession.getCurrentUser();
        if (current == null || !"ADMIN".equalsIgnoreCase(current.getRole())) {
            // 🔒 sécurité côté code, au cas où
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Vous n'avez pas les droits pour accéder à la gestion des utilisateurs.");
            alert.showAndWait();
            return;
        }

        loadPage("UsersView.fxml");
    }

    @FXML
    private void goHistory() {
        loadPage("history.fxml");
    }

    @FXML
    private void logout() {
        try {
            UserSession.clear();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm()
            );

            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            WindowUtil.switchScene(stage, scene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPage(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/" + fxmlFile));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm()
            );

            Stage stage = (Stage) productsBtn.getScene().getWindow();
            WindowUtil.switchScene(stage, scene);

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la page : " + fxmlFile);
            e.printStackTrace();
        }
    }
}
