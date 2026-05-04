package otemps.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import otemps.entites.Categorie;
import otemps.services.CategorieService;

import java.sql.SQLException;

public class CategorieFormController {

    @FXML private Label lblTitre;
    @FXML private TextField tfNom;
    @FXML private TextArea taDescription;
    @FXML private Button btnEnregistrer;
    @FXML private Label lblMessage;

    private final CategorieService categorieService = new CategorieService();
    private Categorie currentCategorie;

    public void loadCategorie(Categorie categorie) {
        this.currentCategorie = categorie;
        lblTitre.setText("Modifier une categorie");
        tfNom.setText(categorie.getNomCategorie());
        taDescription.setText(categorie.getDescription());
    }

    @FXML
    public void handleEnregistrer() {
        String validationMessage = validateFields();
        if (validationMessage != null) {
            showAlert("Champs obligatoires", validationMessage, Alert.AlertType.WARNING);
            return;
        }

        try {
            if (currentCategorie == null) {
                Categorie newCat = new Categorie();
                newCat.setNomCategorie(tfNom.getText().trim());
                newCat.setDescription(taDescription.getText().trim());

                int id = categorieService.ajouter(newCat);
                if (id > 0) {
                    showAlert("Succes", "Categorie ajoutee avec succes.", Alert.AlertType.INFORMATION);
                    lblMessage.setText("Categorie ajoutee");
                    lblMessage.setStyle("-fx-text-fill: #27ae60;");
                    clearForm();
                }
            } else {
                currentCategorie.setNomCategorie(tfNom.getText().trim());
                currentCategorie.setDescription(taDescription.getText().trim());

                categorieService.update(currentCategorie);
                showAlert("Succes", "Categorie modifiee avec succes.", Alert.AlertType.INFORMATION);
                lblMessage.setText("Categorie modifiee");
                lblMessage.setStyle("-fx-text-fill: #27ae60;");
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleAnnuler() {
        Stage stage = (Stage) btnEnregistrer.getScene().getWindow();
        stage.close();
    }

    private String validateFields() {
        StringBuilder errors = new StringBuilder();

        if (tfNom.getText() == null || tfNom.getText().trim().isEmpty()) {
            errors.append("- Le nom de la categorie est obligatoire.\n");
        }
        if (taDescription.getText() == null || taDescription.getText().trim().isEmpty()) {
            errors.append("- La description est obligatoire.\n");
        }

        return errors.isEmpty() ? null : errors.toString().trim();
    }

    private void clearForm() {
        tfNom.clear();
        taDescription.clear();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
