package otemps.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import otemps.entites.Categorie;
import otemps.services.CategorieService;
import otemps.services.ObjetService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class CategorieController {

    @FXML private TableView<Categorie> tableCategories;
    @FXML private TableColumn<Categorie, String> colNom;
    @FXML private TableColumn<Categorie, String> colDescription;
    @FXML private TableColumn<Categorie, Integer> colNbObjets;
    @FXML private TableColumn<Categorie, Void> colActions;
    @FXML private Label lblTotal;
    @FXML private Button btnAjouter;

    private final CategorieService categorieService = new CategorieService();
    private final ObjetService objetService = new ObjetService();

    @FXML
    public void initialize() {
        setupColumns();
        loadCategories();
    }

    private void setupColumns() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nomCategorie"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colNbObjets.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        objetService.getCountByCategorie(cellData.getValue().getIdCategorie())
                )
        );
        setupActionsColumn();
    }

    private void loadCategories() {
        List<Categorie> categories = categorieService.afficher();
        tableCategories.getItems().setAll(categories);
        lblTotal.setText("Total: " + categories.size() + " categories");
    }

    @FXML
    public void handleAjouter() {
        openFormWindow(null, "Ajouter une categorie");
        loadCategories();
    }

    @FXML
    public void handleRetour() {
        Stage stage = (Stage) btnAjouter.getScene().getWindow();
        stage.close();
    }

    private void openFormWindow(Categorie categorie, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/crudadmin.fxml"));
            Parent root = loader.load();

            CategorieFormController controller = loader.getController();
            if (categorie != null) {
                controller.loadCategorie(categorie);
            }

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 800, 500));
            stage.showAndWait();
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");
            private final HBox container = new HBox(8, editButton, deleteButton);

            {
                editButton.setOnAction(event -> {
                    Categorie categorie = getTableView().getItems().get(getIndex());
                    openFormWindow(categorie, "Modifier une categorie");
                    loadCategories();
                });

                deleteButton.setOnAction(event -> {
                    Categorie categorie = getTableView().getItems().get(getIndex());
                    confirmAndDelete(categorie);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void confirmAndDelete(Categorie categorie) {
        int objetsAssocies = objetService.getCountByCategorie(categorie.getIdCategorie());
        if (objetsAssocies > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Suppression impossible");
            alert.setHeaderText(null);
            alert.setContentText("Cette categorie contient encore " + objetsAssocies + " objet(s). Supprimez ou reaffectez ces objets d'abord.");
            alert.showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer la categorie \"" + categorie.getNomCategorie() + "\" ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                categorieService.delete(categorie.getIdCategorie());
                loadCategories();
            } catch (Exception e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText(null);
                errorAlert.setContentText("Impossible de supprimer la categorie: " + e.getMessage());
                errorAlert.showAndWait();
            }
        }
    }
}
