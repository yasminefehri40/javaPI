package otemps.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import otemps.entites.Media;
import otemps.entites.Objet;
import otemps.services.MediaService;
import otemps.services.ObjetService;

import java.sql.SQLException;
import java.util.List;

public class MediaFormController {

    @FXML private Label lblTitre;
    @FXML private ComboBox<Objet> cbObjet;
    @FXML private TextField tfType;
    @FXML private TextField tfLienFichier;
    @FXML private Label lblMessage;
    @FXML private Button btnEnregistrer;

    private final MediaService mediaService = new MediaService();
    private final ObjetService objetService = new ObjetService();
    private Media currentMedia;

    @FXML
    public void initialize() {
        loadObjets();
    }

    private void loadObjets() {
        List<Objet> objets = objetService.afficher();
        cbObjet.getItems().setAll(objets);
        cbObjet.setConverter(new StringConverter<>() {
            @Override
            public String toString(Objet objet) {
                return objet == null ? "" : objet.getNom();
            }

            @Override
            public Objet fromString(String string) {
                return null;
            }
        });
    }

    public void loadMedia(Media media) {
        currentMedia = media;
        lblTitre.setText("Modifier un media");
        tfType.setText(media.getType());
        tfLienFichier.setText(media.getLienFichier());

        for (Objet objet : cbObjet.getItems()) {
            if (objet.getIdObjet() == media.getIdObjet()) {
                cbObjet.setValue(objet);
                break;
            }
        }
    }

    @FXML
    public void handleEnregistrer() {
        String validationMessage = validateFields();
        if (validationMessage != null) {
            showAlert("Champs obligatoires", validationMessage, Alert.AlertType.WARNING);
            return;
        }

        try {
            if (currentMedia == null) {
                Media media = new Media();
                fillMedia(media);
                mediaService.ajouter(media);
                lblMessage.setText("Media ajoute avec succes");
            } else {
                fillMedia(currentMedia);
                mediaService.update(currentMedia);
                lblMessage.setText("Media modifie avec succes");
            }

            lblMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            closeWindow();
        } catch (SQLException e) {
            showAlert("Erreur SQL", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleAnnuler() {
        closeWindow();
    }

    private void fillMedia(Media media) {
        media.setIdObjet(cbObjet.getValue().getIdObjet());
        media.setType(tfType.getText().trim());
        media.setLienFichier(tfLienFichier.getText().trim());
    }

    private String validateFields() {
        StringBuilder errors = new StringBuilder();

        if (cbObjet.getValue() == null) {
            errors.append("- L'objet est obligatoire.\n");
        }
        if (tfType.getText() == null || tfType.getText().trim().isEmpty()) {
            errors.append("- Le type du media est obligatoire.\n");
        }
        if (tfLienFichier.getText() == null || tfLienFichier.getText().trim().isEmpty()) {
            errors.append("- Le lien du fichier est obligatoire.\n");
        }

        return errors.isEmpty() ? null : errors.toString().trim();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnEnregistrer.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
