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
import javafx.stage.Stage;
import model.User;
import util.DatabaseConnection;
import util.SecurityUtil;
import util.WindowUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UsersController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colUsername;
    @FXML private TableColumn<User, String>  colEmail;
    @FXML private TableColumn<User, String>  colRole;

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleBox;
    @FXML private TextField searchField;

    private ObservableList<User> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole()));

        loadUsersFromDb();

        FilteredList<User> filtered = new FilteredList<>(masterData, u -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal == null ? "" : newVal.toLowerCase();
            filtered.setPredicate(u -> {
                if (filter.isEmpty()) return true;
                return String.valueOf(u.getId()).contains(filter)
                        || u.getUsername().toLowerCase().contains(filter)
                        || u.getEmail().toLowerCase().contains(filter)
                        || u.getRole().toLowerCase().contains(filter);
            });
        });

        SortedList<User> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sorted);

        roleBox.setItems(FXCollections.observableArrayList("ADMIN", "USER"));
    }

    private void loadUsersFromDb() {
        masterData.clear();
        String sql = "SELECT * FROM users";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User u = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("role")
                );
                masterData.add(u);
            }

        } catch (Exception e) {
            System.err.println("Erreur loadUsersFromDb : " + e.getMessage());
        }
    }

    private void insertUser(User u) {
        String sql = "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, u.getUsername());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getRole());
            ps.executeUpdate();

        } catch (Exception e) {
            System.err.println("Erreur insertUser : " + e.getMessage());
        }
    }

    private void deleteUserFromDb(int id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            System.err.println("Erreur deleteUserFromDb : " + e.getMessage());
        }
    }

    private void refresh() {
        loadUsersFromDb();
    }

    @FXML
    private void addUser() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String role = roleBox.getValue();

        if (username == null || username.isEmpty()
                || email == null || email.isEmpty()
                || password == null || password.isEmpty()
                || role == null) {
            return;
        }

        String hashed = SecurityUtil.hashPassword(password);
        User user = new User(username, email, hashed, role);
        insertUser(user);

        refresh();

        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        roleBox.getSelectionModel().clearSelection();
    }

    @FXML
    private void deleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        deleteUserFromDb(selected.getId());
        refresh();
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm()
            );
            Stage stage = (Stage) userTable.getScene().getWindow();
            WindowUtil.switchScene(stage, scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
