package com.otemps.controller;

import com.otemps.entity.Participation;
import com.otemps.entity.User;
import com.otemps.service.ParticipationService;
import com.otemps.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
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
    @FXML private ComboBox<User> userFilterCombo;

    private ParticipationService participationService;
    private UserService userService;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        participationService = new ParticipationService();
        userService = new UserService();

        userFilterCombo.getItems().addAll(userService.findAll());
        userFilterCombo.setOnAction(e -> loadParticipations());
        
        // Load default user (e.g. Alice) if exists
        if (!userFilterCombo.getItems().isEmpty()) {
            userFilterCombo.getSelectionModel().select(1); // Select ID 2 (Alice) index 1
            loadParticipations();
        }
    }

    private void loadParticipations() {
        participationContainer.getChildren().clear();
        User selected = userFilterCombo.getValue();
        if (selected == null) return;

        List<Participation> list = participationService.findByUser(selected.getId());
        if (list.isEmpty()) {
            participationContainer.getChildren().add(new Label("Vous n'êtes inscrit à aucun événement."));
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
        
        Label details = new Label("Inscrit le : " + formatter.format(p.getDateInscription()) + " · Lieu : " + p.getEvent().getLieu());
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
            if (alert.showAndWait().get() == ButtonType.OK) {
                participationService.delete(p.getId());
                loadParticipations();
            }
        });

        card.getChildren().addAll(info, status, cancelBtn);
        return card;
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/otemps/views/UserEventList.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) participationContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
