package com.otemps.controller;

import com.otemps.entity.Event;
import com.otemps.service.EventService;
import com.otemps.service.WeatherService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

    private EventService eventService;
    private WeatherService weatherService;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        eventService = new EventService();
        weatherService = new WeatherService();
        
        refreshList();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            loadEvents(newVal);
        });
    }

    public void refreshList() {
        loadEvents(searchField.getText());
    }

    private void loadEvents(String search) {
        eventContainer.getChildren().clear();
        List<Event> events;
        if (search.isEmpty()) {
            events = eventService.findAll();
        } else {
            events = eventService.search(search);
        }

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

        // Weather check
        Label weatherBadge = new Label();
        if (event.getStatut().equals("actif")) {
            Map<String, Object> weather = weatherService.getWeather(event.getLieu(), event.getDateDebut());
            if (weather != null && weatherService.isBadWeather(weather)) {
                weatherBadge.setText("⚠️ Météo défavorable");
                weatherBadge.setStyle("-fx-text-fill: #eab308; -fx-font-size: 11px;");
            }
        }

        Label status = new Label(event.getStatut().toUpperCase());
        status.getStyleClass().add("status-badge");
        if (event.getStatut().startsWith("annul")) {
            status.getStyleClass().add("status-cancelled");
        } else {
            status.getStyleClass().add("status-active");
        }

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

    private void handleEditEvent(Event event) {
        showEventForm(event);
    }

    private void handleDeleteEvent(Event event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'événement : " + event.getTitre());
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cet événement ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
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
            stage.setTitle(event == null ? "Nouvel Événement" : "Modifier l'Événement");
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
        dialog.setTitle("Statistiques des Événements");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Événements");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Capacité (Places)");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Capacité par Événement");

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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/otemps/views/UserEventList.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) eventContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
