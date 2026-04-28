package com.otemps.controller;

import com.otemps.entity.Participation;
import com.otemps.entity.User;
import com.otemps.service.ParticipationService;
import com.otemps.session.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class UserParticipationsController implements Initializable {

    @FXML private VBox participationContainer;
    @FXML private Label currentUserLabel;
    @FXML private Label currentRoleLabel;

    private ParticipationService participationService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!ensureSession()) {
            return;
        }

        participationService = new ParticipationService();
        updateSessionHeader();
        loadParticipations();
    }

    private boolean ensureSession() {
        if (UserSession.isLoggedIn()) {
            return true;
        }

        Platform.runLater(() -> navigateTo("/com/otemps/views/LoginView.fxml"));
        return false;
    }

    private void updateSessionHeader() {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        currentUserLabel.setText(currentUser.getName());
        currentRoleLabel.setText(currentUser.isAdmin() ? "Administrateur" : "Participant");
    }

    private void loadParticipations() {
        participationContainer.getChildren().clear();
        User selected = UserSession.getCurrentUser();
        if (selected == null) return;

        List<Participation> list = participationService.findByUser(selected.getId());
        if (list.isEmpty()) {
            participationContainer.getChildren().add(new Label("Vous n'etes inscrit a aucun evenement."));
            return;
        }

        for (Participation p : list) {
            participationContainer.getChildren().add(createParticipationCard(p));
        }
    }

    private HBox createParticipationCard(Participation p) {
        HBox card = new HBox(20);
        card.getStyleClass().add("event-card");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(5);
        Label title = new Label(p.getEvent().getTitre());
        title.getStyleClass().add("event-card-title");

        Label details = new Label("Inscrit le : " + formatter.format(p.getDateInscription()) + " | Lieu : " + p.getEvent().getLieu());
        details.getStyleClass().add("event-card-subtitle");

        info.getChildren().addAll(title, details);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label status = new Label(p.getStatut().toUpperCase());
        status.getStyleClass().add("status-badge");
        status.getStyleClass().add("status-active");

        Button cancelBtn = new Button("Annuler mon inscription");
        cancelBtn.setStyle("-fx-text-fill: #ef4444; -fx-background-color: #fee2e2; -fx-background-radius: 8; -fx-padding: 8 15; -fx-font-weight: bold;");
        cancelBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Annuler l'inscription");
            alert.setHeaderText("Souhaitez-vous annuler votre participation ?");
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                participationService.delete(p.getId());
                loadParticipations();
            }
        });

        card.getChildren().addAll(info, status, cancelBtn);
        return card;
    }

    @FXML
    private void handleBack() {
        navigateTo("/com/otemps/views/UserEventList.fxml");
    }

    @FXML
    private void handleLogout() {
        UserSession.logout();
        navigateTo("/com/otemps/views/LoginView.fxml");
    }

    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) participationContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
