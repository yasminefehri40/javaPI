package com.otemps.controller;

import com.otemps.entity.User;
import com.otemps.service.UserService;
import com.otemps.session.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private ComboBox<User> userComboBox;
    @FXML private Label helperLabel;
    @FXML private Button loginButton;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        List<User> users = userService.findAll();
        userComboBox.getItems().setAll(users);

        if (users.isEmpty()) {
            loginButton.setDisable(true);
            helperLabel.setText("Aucun utilisateur n'est disponible dans la base.");
            return;
        }

        userComboBox.getSelectionModel().selectFirst();
        updateHelper(userComboBox.getValue());
        userComboBox.setOnAction(event -> updateHelper(userComboBox.getValue()));
    }

    @FXML
    private void handleLogin() {
        User selectedUser = userComboBox.getValue();
        if (selectedUser == null) {
            new Alert(Alert.AlertType.WARNING, "Selectionnez un utilisateur pour continuer.").show();
            return;
        }

        UserSession.login(selectedUser);
        navigateTo(selectedUser.isAdmin()
                ? "/com/otemps/views/AdminDashboard.fxml"
                : "/com/otemps/views/UserEventList.fxml");
    }

    private void updateHelper(User user) {
        if (user == null) {
            helperLabel.setText("Choisissez un profil.");
            return;
        }

        String role = user.isAdmin() ? "Administrateur" : "Participant";
        helperLabel.setText(user.getEmail() + " | " + role);
    }

    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Impossible de charger l'interface : " + e.getMessage()).show();
        }
    }
}
