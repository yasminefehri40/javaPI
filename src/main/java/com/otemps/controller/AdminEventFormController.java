package com.otemps.controller;

import com.otemps.entity.Event;
import com.otemps.entity.User;
import com.otemps.service.EventService;
import com.otemps.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class AdminEventFormController implements Initializable {

    @FXML private Label formTitle;
    @FXML private TextField titreField;
    @FXML private TextField lieuField;
    @FXML private TextField nbPlacesField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextArea descriptionArea;

    private EventService eventService;
    private Event event;
    private boolean isEdit = false;
    private AdminEventController parentController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        eventService = new EventService();
        setupValidation();
    }

    private void setupValidation() {
        // Dynamic visual feedback (example: red border on error)
        titreField.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean validFormat = newVal.length() >= 5;
            boolean exists = eventService.existsByTitre(newVal, event != null ? event.getId() : 0);
            if (!validFormat) {
                titreField.setStyle("-fx-border-color: #ef4444;");
                titreField.setTooltip(new Tooltip("Minimum 5 caractères"));
            } else if (exists) {
                titreField.setStyle("-fx-border-color: #f59e0b; -fx-border-width: 2;");
                titreField.setTooltip(new Tooltip("⚠️ Ce nom d'événement existe déjà !"));
            } else {
                titreField.setStyle("-fx-border-color: #10b981; -fx-border-width: 2;"); // Green when beautiful and unique
                titreField.setTooltip(null);
            }
        });

        lieuField.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean valid = newVal.length() > 3;
            lieuField.setStyle(valid ? "" : "-fx-border-color: #ef4444;");
        });

        nbPlacesField.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean valid = newVal.matches("\\d+");
            nbPlacesField.setStyle(valid ? "" : "-fx-border-color: #ef4444;");
        });

        descriptionArea.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean valid = newVal.length() >= 10;
            descriptionArea.setStyle(valid ? "" : "-fx-border-color: #ef4444;");
        });
    }

    public void setParentController(AdminEventController parentController) {
        this.parentController = parentController;
    }

    public void setEvent(Event event) {
        if (event != null) {
            this.event = event;
            isEdit = true;
            formTitle.setText("Modifier l'Événement");
            titreField.setText(event.getTitre());
            lieuField.setText(event.getLieu());
            nbPlacesField.setText(String.valueOf(event.getNbPlaces()));
            dateDebutPicker.setValue(event.getDateDebut().toLocalDate());
            dateFinPicker.setValue(event.getDateFin().toLocalDate());
            descriptionArea.setText(event.getDescription());
        } else {
            this.event = new Event();
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) return;

        event.setTitre(titreField.getText());
        event.setLieu(lieuField.getText());
        event.setNbPlaces(Integer.parseInt(nbPlacesField.getText()));
        event.setDateDebut(LocalDateTime.of(dateDebutPicker.getValue(), LocalTime.of(14, 0)));
        event.setDateFin(LocalDateTime.of(dateFinPicker.getValue(), LocalTime.of(17, 0)));
        event.setDescription(descriptionArea.getText());

        try {
            if (!isEdit) {
                UserService userService = new UserService();
                User admin = userService.findById(1);
                event.setCreator(admin);
                eventService.add(event);
            } else {
                eventService.update(event);
            }
            parentController.refreshList();
            closeStage();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur base de données : " + e.getMessage());
            alert.show();
        }
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        int excludeId = event != null ? event.getId() : 0;
        if (titreField.getText().length() < 5) {
            errors.append("- Titre : minimum 5 caractères\n");
        } else if (eventService.existsByTitre(titreField.getText(), excludeId)) {
            errors.append("- Titre : Un événement avec ce nom existe déjà !\n");
        }
        if (lieuField.getText().length() <= 3) errors.append("- Lieu : veuillez saisir une adresse valide\n");
        if (!nbPlacesField.getText().matches("\\d+")) errors.append("- Nombre de places : uniquement des chiffres\n");
        if (descriptionArea.getText().length() < 10) errors.append("- Description : minimum 10 caractères\n");
        
        if (dateDebutPicker.getValue() != null && dateFinPicker.getValue() != null) {
            if (dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())) {
                errors.append("- Date fin doit être après Date début\n");
            }
        } else {
            errors.append("- Les dates sont obligatoires\n");
        }

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Erreur de saisie");
            alert.setHeaderText("Veuillez corriger les champs suivants :");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }
        return true;
    }

    private void closeStage() {
        ((Stage) titreField.getScene().getWindow()).close();
    }

    @FXML
    private void handleOpenMap() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Sélectionner une adresse sur la carte");
        
        ButtonType validerBtnType = new ButtonType("Valider cette adresse", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(validerBtnType, ButtonType.CANCEL);
        
        WebView webView = new WebView();
        webView.setPrefSize(700, 500);
        
        String mapHtml = "<!DOCTYPE html>\n" +
                         "<html>\n" +
                         "<head>\n" +
                         "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
                         "    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
                         "    <style>body{margin:0;padding:0;} #map{height:100vh;width:100vw;}</style>\n" +
                         "</head>\n" +
                         "<body>\n" +
                         "    <div id=\"map\"></div>\n" +
                         "    <div id=\"selected_address\" style=\"display:none;\"></div>\n" +
                         "    <script>\n" +
                         "        var map = L.map('map').setView([48.8566, 2.3522], 12);\n" +
                         "        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);\n" +
                         "        var marker;\n" +
                         "        map.on('click', function(e) {\n" +
                         "            if(marker) map.removeLayer(marker);\n" +
                         "            marker = L.marker(e.latlng).addTo(map);\n" +
                         "            fetch('https://nominatim.openstreetmap.org/reverse?format=json&lat=' + e.latlng.lat + '&lon=' + e.latlng.lng)\n" +
                         "              .then(res => res.json())\n" +
                         "              .then(data => {\n" +
                         "                  document.getElementById('selected_address').innerText = data.display_name || '';\n" +
                         "              });\n" +
                         "        });\n" +
                         "    </script>\n" +
                         "</body>\n" +
                         "</html>";
        
        webView.getEngine().loadContent(mapHtml);
        dialog.getDialogPane().setContent(webView);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == validerBtnType) {
                try {
                    return (String) webView.getEngine().executeScript("document.getElementById('selected_address').innerText");
                } catch (Exception e) {}
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(address -> {
            if (address != null && !address.trim().isEmpty()) {
                lieuField.setText(address);
                // Declenchement manuel pour enlever la bordure rouge si existante
                lieuField.setStyle("");
            }
        });
    }
}
