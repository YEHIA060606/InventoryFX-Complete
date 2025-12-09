package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyEvent;          // 👈 IMPORT AJOUTÉ
import javafx.stage.Stage;
import model.User;
import util.DatabaseConnection;
import util.SecurityUtil;
import util.UserSession;
import util.WindowUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML
    private TextField usernameField;   // username OU email

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Hyperlink forgotLink;      // lien "mot de passe oublié"

    @FXML
    private void login(ActionEvent event) {
        String login = usernameField.getText();
        String password = passwordField.getText();

        if (login == null || login.isEmpty()
                || password == null || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        String hashed = SecurityUtil.hashPassword(password);

        // 🔍 On va chercher l'utilisateur directement en BDD
        User user = findByLogin(login);

        if (user == null || !user.getPasswordHash().equals(hashed)) {
            errorLabel.setText("Identifiants incorrects.");
            return;
        }

        UserSession.setCurrentUser(user);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm()
            );

            Stage stage = (Stage) usernameField.getScene().getWindow();
            WindowUtil.switchScene(stage, scene);

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur lors du chargement du tableau de bord.");
        }
    }

    /**
     * Cherche un utilisateur par username OU email directement en JDBC.
     * ⚠️ On NE FERME PAS la connexion globale, on ferme juste ps + rs.
     */
    private User findByLogin(String login) {
        String sql = "SELECT * FROM users WHERE username = ? OR email = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection(); // connexion globale
            ps = conn.prepareStatement(sql);
            ps.setString(1, login);
            ps.setString(2, login);

            rs = ps.executeQuery();
            if (rs.next()) {
                int id          = rs.getInt("id");
                String username = rs.getString("username");
                String email    = rs.getString("email");
                String hash     = rs.getString("password_hash");
                String role     = rs.getString("role");

                return new User(id, username, email, hash, role);
            }

        } catch (Exception e) {
            System.err.println("Erreur findByLogin (LoginController) : " + e.getMessage());
        } finally {
            // ⚠️ On ferme UNIQUEMENT rs et ps, PAS conn
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
        }

        return null;
    }

    @FXML
    private void forgotPassword() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mot de passe oublié");
        alert.setHeaderText(null);
        alert.setContentText(
                "Veuillez contacter l'administrateur pour réinitialiser votre mot de passe."
        );
        alert.showAndWait();
    }

    // ✅ Appuyer sur Entrée dans un champ déclenche la connexion
    @FXML
    private void handleEnterKey(KeyEvent event) {
        switch (event.getCode()) {
            case ENTER -> login(null);
            default -> { /* rien */ }
        }
    }
}
