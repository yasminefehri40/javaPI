package com.otemps.controller;

import com.otemps.entity.Event;
import com.otemps.service.EventService;
import com.otemps.service.WeatherService;
import com.otemps.session.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminEventController implements Initializable {

    @FXML private VBox eventContainer;
    @FXML private TextField searchField;
    @FXML private Label adminNameLabel;
    @FXML private Label adminRoleLabel;

    private EventService eventService;
    private WeatherService weatherService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!ensureAdminSession()) {
            return;
        }

        eventService = new EventService();
        weatherService = new WeatherService();
        adminNameLabel.setText(UserSession.getCurrentUser().getName());
        adminRoleLabel.setText("Administrateur");

        refreshList();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadEvents(newVal));
    }

    private boolean ensureAdminSession() {
        if (UserSession.isAdmin()) {
            return true;
        }

        Platform.runLater(() -> {
            if (eventContainer != null && eventContainer.getScene() != null) {
                navigateTo("/com/otemps/views/LoginView.fxml");
            }
        });
        return false;
    }

    public void refreshList() {
        loadEvents(searchField.getText());
    }

    private void loadEvents(String search) {
        eventContainer.getChildren().clear();
        List<Event> events = search == null || search.isEmpty()
                ? eventService.findAll()
                : eventService.search(search);

        for (Event event : events) {
            eventContainer.getChildren().add(createEventCard(event));
        }
    }

    private HBox createEventCard(Event event) {
        HBox card = new HBox(20);
        card.getStyleClass().add("event-card");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(5);
        Label title = new Label(event.getTitre());
        title.getStyleClass().add("event-card-title");

        Label details = new Label(formatter.format(event.getDateDebut()) + " - " + event.getLieu());
        details.getStyleClass().add("event-card-subtitle");

        info.getChildren().addAll(title, details);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label weatherBadge = new Label();
        if ("actif".equals(event.getStatut())) {
            Map<String, Object> weather = weatherService.getWeather(event.getLieu(), event.getDateDebut());
            if (weather != null && weatherService.isBadWeather(weather)) {
                weatherBadge.setText("Meteo defavorable");
                weatherBadge.setStyle("-fx-text-fill: #eab308; -fx-font-size: 11px;");
            }
        }

        Label status = new Label(event.getStatut().toUpperCase());
        status.getStyleClass().add("status-badge");
        status.getStyleClass().add(event.getStatut().startsWith("annul") ? "status-cancelled" : "status-active");

        HBox actions = new HBox(10);
        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().add("btn-small");
        editBtn.setOnAction(e -> handleEditEvent(event));

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().add("btn-small");
        deleteBtn.setStyle("-fx-text-fill: #ef4444;");
        deleteBtn.setOnAction(e -> handleDeleteEvent(event));

        actions.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(info, weatherBadge, status, actions);
        return card;
    }

    @FXML
    private void handleNewEvent() {
        showEventForm(null);
    }

    @FXML
    private void handleOpenHeritageAdmin() {
        navigateTo("/fxml/Dashboard.fxml");
    }

    private void handleEditEvent(Event event) {
        showEventForm(event);
    }

    private void handleDeleteEvent(Event event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'evenement : " + event.getTitre());
        alert.setContentText("Etes-vous sur de vouloir supprimer cet evenement ?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            eventService.delete(event.getId());
            refreshList();
        }
    }

    private void showEventForm(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/otemps/views/AdminEventForm.fxml"));
            Parent root = loader.load();

            AdminEventFormController controller = loader.getController();
            controller.setParentController(this);
            controller.setEvent(event);

            Stage stage = new Stage();
            stage.setTitle(event == null ? "Nouvel evenement" : "Modifier l'evenement");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(eventContainer.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowStats() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Statistiques des evenements");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Evenements");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Capacite (Places)");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Capacite par evenement");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Places");

        List<Event> events = eventService.findAll();
        for (Event e : events) {
            series.getData().add(new XYChart.Data<>(e.getTitre(), e.getNbPlaces()));
        }

        barChart.getData().add(series);
        barChart.setPrefSize(800, 500);

        dialog.getDialogPane().setContent(barChart);
        dialog.showAndWait();
    }

    @FXML
    private void handleSwitchToFront() {
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
            Stage stage = (Stage) eventContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
