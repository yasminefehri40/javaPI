package com.otemps.controller;

import com.otemps.entity.Event;
import com.otemps.entity.User;
import com.otemps.service.EventService;
import com.otemps.service.ParticipationService;
import com.otemps.service.ReviewService;
import com.otemps.service.UserService;
import com.otemps.session.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class UserEventController implements Initializable {

    @FXML private FlowPane eventFlowPane;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Button adminPanelButton;
    @FXML private Label pageInfoLabel;
    @FXML private Label currentUserLabel;
    @FXML private Label currentRoleLabel;

    private EventService eventService;
    private UserService userService;
    private ParticipationService participationService;
    private ReviewService reviewService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private int currentPage = 1;
    private final int pageSize = 3;
    private int totalPages = 1;
    private String currentSort = "nb_places";
    private String currentDir = "ASC";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!ensureSession()) {
            return;
        }

        eventService = new EventService();
        userService = new UserService();
        participationService = new ParticipationService();
        reviewService = new ReviewService();

        updateSessionHeader();

        sortCombo.getItems().addAll("Places (Croissant)", "Places (Decroissant)", "Date (Plus recent)");
        sortCombo.getSelectionModel().select(0);
        sortCombo.setOnAction(e -> handleSortChange());

        loadEvents("");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 1;
            loadEvents(newVal);
        });
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
        adminPanelButton.setVisible(currentUser.isAdmin());
        adminPanelButton.setManaged(currentUser.isAdmin());
    }

    private void handleSortChange() {
        String selected = sortCombo.getValue();
        if (selected == null) return;
        if (selected.contains("Croissant")) {
            currentSort = "nb_places";
            currentDir = "ASC";
        } else if (selected.contains("Decroissant")) {
            currentSort = "nb_places";
            currentDir = "DESC";
        } else {
            currentSort = "date_debut";
            currentDir = "DESC";
        }
        currentPage = 1;
        loadEvents(searchField.getText());
    }

    private void loadEvents(String search) {
        eventFlowPane.getChildren().clear();
        List<Event> events;

        if (search.isEmpty()) {
            int total = eventService.countAll();
            totalPages = (int) Math.ceil((double) total / pageSize);
            if (totalPages == 0) totalPages = 1;
            events = eventService.findPaged(currentPage, pageSize, currentSort, currentDir);

            pageInfoLabel.setText("Page " + currentPage + " / " + totalPages);
            prevBtn.setDisable(currentPage == 1);
            nextBtn.setDisable(currentPage == totalPages);
        } else {
            events = eventService.search(search);
            pageInfoLabel.setText("Resultats");
            prevBtn.setDisable(true);
            nextBtn.setDisable(true);
        }

        for (Event event : events) {
            eventFlowPane.getChildren().add(createEventUserCard(event));
        }
    }

    @FXML
    private void handleNextPage() {
        currentPage++;
        loadEvents("");
    }

    @FXML
    private void handlePrevPage() {
        currentPage--;
        loadEvents("");
    }

    private VBox createEventUserCard(Event event) {
        VBox card = new VBox();
        card.getStyleClass().add("event-user-card");

        VBox imageBox = new VBox();
        imageBox.getStyleClass().add("event-image-placeholder");
        Label icon = new Label("Evenement");
        icon.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        imageBox.getChildren().add(icon);

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        Label title = new Label(event.getTitre());
        title.getStyleClass().add("event-user-title");
        Label date = new Label("Date : " + formatter.format(event.getDateDebut()));

        int count = participationService.countByEvent(event.getId());
        int rem = event.getNbPlaces() - count;
        Label placesLabel = new Label((rem > 0 ? rem : 0) + " places restantes");
        placesLabel.setStyle(rem > 0 ? "-fx-text-fill: #16a34a;" : "-fx-text-fill: #ef4444;");

        HBox footer = new HBox(placesLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btn = new Button("Details");
        btn.getStyleClass().add("btn-participate");
        btn.setOnAction(e -> showEventDetails(event));
        footer.getChildren().addAll(spacer, btn);

        Label weatherLabel = new Label("Meteo : verification en cours...");
        weatherLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #64748b; -fx-font-size: 11px;");

        content.getChildren().addAll(title, date, weatherLabel, footer);
        card.getChildren().addAll(imageBox, content);

        new Thread(() -> {
            try {
                String place = event.getLieu().split(",")[0].trim().replace(" ", "+");
                String meteoUrl = "https://wttr.in/" + place + "?format=1";
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(new java.net.URI(meteoUrl))
                        .header("User-Agent", "curl")
                        .build();
                java.net.http.HttpResponse<String> res = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    String weather = res.body().trim();

                    int temperature = 999;
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("([-+]?\\d+)").matcher(weather);
                    if (m.find()) {
                        temperature = Integer.parseInt(m.group(1));
                    }

                    boolean isCold = temperature != 999 && temperature < 14;
                    boolean isRaining = weather.toLowerCase().contains("rain")
                            || weather.toLowerCase().contains("shower")
                            || weather.toLowerCase().contains("pluie")
                            || weather.toLowerCase().contains("storm")
                            || weather.toLowerCase().contains("drizzle");

                    Platform.runLater(() -> {
                        if (isRaining) {
                            weatherLabel.setText("Meteo : " + weather + " - evenement annule");
                            weatherLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 11px;");
                            btn.setDisable(true);
                            btn.setText("Annule");
                            card.setOpacity(0.6);
                            title.setStyle("-fx-strikethrough: true; -fx-text-fill: #ef4444;");
                        } else if (isCold) {
                            weatherLabel.setText("Meteo : " + weather + " - trop froid");
                            weatherLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold; -fx-font-size: 11px;");
                            btn.setDisable(true);
                            btn.setText("Ferme");
                        } else {
                            weatherLabel.setText("Meteo : " + weather);
                            weatherLabel.setStyle("-fx-text-fill: #0ea5e9; -fx-font-weight: bold; -fx-font-size: 11px;");
                        }
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> weatherLabel.setText("Meteo : indisponible."));
            }
        }).start();

        return card;
    }

    private void showEventDetails(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/otemps/views/EventDetailsDialog.fxml"));
            VBox root = loader.load();

            EventDetailsDialogController controller = loader.getController();
            controller.setEvent(event);

            Stage stage = new Stage();
            stage.setTitle("Details de l'evenement - " + event.getTitre());
            stage.initOwner(eventFlowPane.getScene().getWindow());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

            loadEvents(searchField.getText());
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'ouverture des details : " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleGoToParticipations() {
        navigateTo("/com/otemps/views/UserParticipations.fxml");
    }

    @FXML
    private void handleSwitchToAdmin() {
        if (!UserSession.isAdmin()) {
            new Alert(Alert.AlertType.WARNING, "Cette section est reservee aux administrateurs.").show();
            return;
        }
        navigateTo("/com/otemps/views/AdminDashboard.fxml");
    }

    @FXML
    private void handleOpenChatbot() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/otemps/views/ChatbotView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Assistant OTEMPS");
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleOpenGlobalMap() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ou se trouvent nos evenements ?");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        WebView webView = new WebView();
        webView.setPrefSize(800, 600);

        List<Event> events = eventService.findAll();
        StringBuilder jsonEvents = new StringBuilder("[");
        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            jsonEvents.append("{")
                    .append("\"titre\":\"").append(e.getTitre().replace("\"", "\\\"")).append("\",")
                    .append("\"lieu\":\"").append(e.getLieu().replace("\"", "\\\"")).append("\"")
                    .append("}");
            if (i < events.size() - 1) {
                jsonEvents.append(",");
            }
        }
        jsonEvents.append("]");

        String mapHtml = "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n"
                + "    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n"
                + "    <style>body{margin:0;padding:0;} #map{height:100vh;width:100vw; font-family:sans-serif;}</style>\n"
                + "</head>\n"
                + "<body>\n"
                + "    <div id=\"map\"></div>\n"
                + "    <script>\n"
                + "        var events = " + jsonEvents + ";\n"
                + "        var map = L.map('map').setView([48.8566, 2.3522], 6);\n"
                + "        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);\n"
                + "        function processEvent(index) {\n"
                + "            if (index >= events.length) return;\n"
                + "            let e = events[index];\n"
                + "            fetch('https://nominatim.openstreetmap.org/search?format=json&q=' + encodeURIComponent(e.lieu))\n"
                + "              .then(res => res.json())\n"
                + "              .then(data => {\n"
                + "                  if (data && data.length > 0) {\n"
                + "                      let marker = L.marker([data[0].lat, data[0].lon]).addTo(map);\n"
                + "                      marker.bindPopup('<b>' + e.titre + '</b><br>' + e.lieu);\n"
                + "                      if (index === 0) map.setView([data[0].lat, data[0].lon], 5);\n"
                + "                  }\n"
                + "                  setTimeout(() => processEvent(index + 1), 1100);\n"
                + "              }).catch(err => setTimeout(() => processEvent(index + 1), 1100));\n"
                + "        }\n"
                + "        if (events.length > 0) processEvent(0);\n"
                + "    </script>\n"
                + "</body>\n"
                + "</html>";

        webView.getEngine().loadContent(mapHtml);
        dialog.getDialogPane().setContent(webView);
        dialog.show();
    }

    @FXML
    private void handleOpenCalendar() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Calendrier des evenements");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(600);
        root.setPrefHeight(550);

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER);
        Button prevMonth = new Button("<");
        Button nextMonth = new Button(">");
        Label monthLabel = new Label();
        monthLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        header.getChildren().addAll(prevMonth, monthLabel, nextMonth);

        GridPane calendarGrid = new GridPane();
        calendarGrid.setAlignment(Pos.CENTER);
        calendarGrid.setHgap(5);
        calendarGrid.setVgap(5);

        String[] days = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(days[i]);
            dayLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5px;");
            calendarGrid.add(dayLabel, i, 0);
        }

        List<Event> allEvents = eventService.findAll();
        final java.time.YearMonth[] currentMonth = {java.time.YearMonth.now()};

        Runnable updateCalendar = () -> {
            calendarGrid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0);

            monthLabel.setText(currentMonth[0].getMonth().name() + " " + currentMonth[0].getYear());

            java.time.LocalDate firstDay = currentMonth[0].atDay(1);
            int dayOfWeek = firstDay.getDayOfWeek().getValue();
            int daysInMonth = currentMonth[0].lengthOfMonth();

            int row = 1;
            int col = dayOfWeek - 1;

            for (int day = 1; day <= daysInMonth; day++) {
                java.time.LocalDate currentDate = currentMonth[0].atDay(day);

                VBox dayCell = new VBox(5);
                dayCell.setAlignment(Pos.TOP_CENTER);
                dayCell.setPrefSize(75, 75);
                dayCell.setStyle("-fx-border-color: #cbd5e1; -fx-border-radius: 5; -fx-background-radius: 5;");

                Label dayNum = new Label(String.valueOf(day));
                dayNum.setStyle("-fx-font-weight: bold;");
                dayCell.getChildren().add(dayNum);

                List<Event> eventsToday = allEvents.stream()
                        .filter(e -> e.getDateDebut().toLocalDate().isEqual(currentDate))
                        .toList();

                if (!eventsToday.isEmpty()) {
                    dayCell.setStyle("-fx-border-color: #8b5cf6; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-color: #ede9fe; -fx-background-radius: 5;");
                    Label evLabel = new Label(eventsToday.size() + " even.");
                    evLabel.setStyle("-fx-text-fill: #6d28d9; -fx-font-size: 11px; -fx-font-weight: bold;");
                    dayCell.getChildren().add(evLabel);
                    dayCell.setOnMouseClicked(e -> showEventDetails(eventsToday.get(0)));
                    dayCell.setStyle(dayCell.getStyle() + " -fx-cursor: hand;");
                }

                calendarGrid.add(dayCell, col, row);

                col++;
                if (col > 6) {
                    col = 0;
                    row++;
                }
            }
        };

        prevMonth.setOnAction(e -> {
            currentMonth[0] = currentMonth[0].minusMonths(1);
            updateCalendar.run();
        });

        nextMonth.setOnAction(e -> {
            currentMonth[0] = currentMonth[0].plusMonths(1);
            updateCalendar.run();
        });

        updateCalendar.run();

        root.getChildren().addAll(header, calendarGrid);
        dialog.getDialogPane().setContent(root);
        dialog.showAndWait();
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
            Stage stage = (Stage) eventFlowPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
