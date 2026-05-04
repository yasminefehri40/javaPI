package otemps.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import otemps.entites.Categorie;
import otemps.entites.Objet;
import otemps.services.CategorieService;
import otemps.services.ObjetService;

import java.sql.SQLException;

public class ObjetFormController {

    @FXML private Label lblTitre;
    @FXML private Label lblMessage;
    @FXML private TextField tfNom;
    @FXML private TextField tfEpoque;
    @FXML private TextField tfOrigine;
    @FXML private TextField tfMateriaux;
    @FXML private ComboBox<Categorie> cbCategorie;
    @FXML private TextArea taDescription;
    @FXML private Button btnEnregistrer;

    private final ObjetService objetService = new ObjetService();
    private final CategorieService categorieService = new CategorieService();
    private Objet currentObjet;

    @FXML
    public void initialize() {
        setupProComboBox();
        loadCategories();
    }

    private void setupProComboBox() {
        cbCategorie.setConverter(new StringConverter<>() {
            @Override
            public String toString(Categorie cat) {
                return cat == null ? "" : cat.getNomCategorie();
            }

            @Override
            public Categorie fromString(String s) {
                return null;
            }
        });

        cbCategorie.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Categorie item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox container = new VBox(2);
                    Label nameLabel = new Label(item.getNomCategorie());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                    container.getChildren().add(nameLabel);
                    setGraphic(container);
                }
            }
        });

        cbCategorie.setStyle("-fx-background-radius: 5; -fx-border-radius: 5;");
    }

    private void loadCategories() {
        cbCategorie.getItems().setAll(categorieService.afficher());
    }

    public void loadObjet(Objet objet) {
        this.currentObjet = objet;
        lblTitre.setText("Modifier l'objet");

        tfNom.setText(objet.getNom());
        tfEpoque.setText(objet.getEpoque());
        tfOrigine.setText(objet.getOrigine());
        tfMateriaux.setText(objet.getMateriaux());
        taDescription.setText(objet.getDescription());

        for (Categorie cat : cbCategorie.getItems()) {
            if (cat.getIdCategorie() == objet.getIdCategorie()) {
                cbCategorie.setValue(cat);
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
            if (currentObjet == null) {
                Objet newObj = new Objet();
                fillData(newObj);
                objetService.ajouter(newObj);
                showNotification("Objet ajoute avec succes");
                clearForm();
            } else {
                fillData(currentObjet);
                objetService.update(currentObjet);
                showNotification("Objet mis a jour");
            }
        } catch (SQLException e) {
            showAlert("Erreur SQL", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void fillData(Objet obj) {
        obj.setNom(tfNom.getText().trim());
        obj.setEpoque(tfEpoque.getText().trim());
        obj.setOrigine(tfOrigine.getText().trim());
        obj.setMateriaux(tfMateriaux.getText().trim());
        obj.setDescription(taDescription.getText().trim());
        obj.setIdCategorie(cbCategorie.getValue().getIdCategorie());
    }

    private String validateFields() {
        StringBuilder errors = new StringBuilder();

        if (tfNom.getText() == null || tfNom.getText().trim().isEmpty()) {
            errors.append("- Le nom de l'objet est obligatoire.\n");
        }
        if (cbCategorie.getValue() == null) {
            errors.append("- La categorie est obligatoire.\n");
        }
        if (tfEpoque.getText() == null || tfEpoque.getText().trim().isEmpty()) {
            errors.append("- L'epoque est obligatoire.\n");
        }
        if (tfOrigine.getText() == null || tfOrigine.getText().trim().isEmpty()) {
            errors.append("- L'origine est obligatoire.\n");
        }
        if (tfMateriaux.getText() == null || tfMateriaux.getText().trim().isEmpty()) {
            errors.append("- Les materiaux sont obligatoires.\n");
        }
        if (taDescription.getText() == null || taDescription.getText().trim().isEmpty()) {
            errors.append("- La description est obligatoire.\n");
        }

        return errors.isEmpty() ? null : errors.toString().trim();
    }

    private void showNotification(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }

    @FXML
    public void handleAnnuler() {
        Stage stage = (Stage) btnEnregistrer.getScene().getWindow();
        stage.close();
    }

    private void clearForm() {
        tfNom.clear();
        tfEpoque.clear();
        tfOrigine.clear();
        tfMateriaux.clear();
        taDescription.clear();
        cbCategorie.setValue(null);
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
