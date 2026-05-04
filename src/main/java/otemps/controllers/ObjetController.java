package otemps.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import otemps.entites.Categorie;
import otemps.entites.Objet;
import otemps.services.CategorieService;
import otemps.services.ObjetService;

import java.io.IOException;
import java.util.List;

public class ObjetController {

    @FXML private TableView<Objet> tableObjets;
    @FXML private TableColumn<Objet, String> colNom;
    @FXML private TableColumn<Objet, String> colCategorie;
    @FXML private TableColumn<Objet, String> colEpoque;
    @FXML private TableColumn<Objet, String> colOrigine;
    @FXML private TableColumn<Objet, Void> colActions;
    @FXML private TextField tfRecherche;
    @FXML private ComboBox<Categorie> cbCategorie;
    @FXML private Label lblTotal;
    @FXML private Button btnAjouter;

    private final ObjetService objetService = new ObjetService();
    private final CategorieService categorieService = new CategorieService();

    @FXML
    public void initialize() {
        setupColumns();
        loadCategories();
        loadObjets();
    }

    private void setupColumns() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colCategorie.setCellValueFactory(cellData -> {
            Categorie categorie = categorieService.getById(cellData.getValue().getIdCategorie());
            String nomCategorie = categorie != null ? categorie.getNomCategorie() : "Non definie";
            return new javafx.beans.property.SimpleStringProperty(nomCategorie);
        });
        colEpoque.setCellValueFactory(new PropertyValueFactory<>("epoque"));
        colOrigine.setCellValueFactory(new PropertyValueFactory<>("origine"));
        setupActionsColumn();
    }

    private void loadCategories() {
        List<Categorie> categories = categorieService.afficher();
        cbCategorie.getItems().setAll(categories);
        cbCategorie.setConverter(new StringConverter<>() {
            @Override
            public String toString(Categorie categorie) {
                return categorie == null ? "" : categorie.getNomCategorie();
            }

            @Override
            public Categorie fromString(String string) {
                return null;
            }
        });
    }

    private void loadObjets() {
        List<Objet> objets = objetService.afficher();
        tableObjets.getItems().setAll(objets);
        lblTotal.setText("Total: " + objets.size() + " objets");
    }

    @FXML
    public void handleAjouter() {
        openFormWindow(null, "Ajouter un objet");
        loadObjets();
    }

    @FXML
    public void handleFiltrer() {
        String recherche = tfRecherche.getText() == null ? "" : tfRecherche.getText().trim().toLowerCase();
        List<Objet> results;

        if (!recherche.isEmpty()) {
            results = objetService.search(recherche);
        } else if (cbCategorie.getValue() != null) {
            results = objetService.getByCategorie(cbCategorie.getValue().getIdCategorie());
        } else {
            results = objetService.afficher();
        }

        tableObjets.getItems().setAll(results);
        lblTotal.setText("Total: " + results.size() + " objets");
    }

    @FXML
    public void handleRetour() {
        Stage stage = (Stage) btnAjouter.getScene().getWindow();
        stage.close();
    }

    private void openFormWindow(Objet objet, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/crudadminobjet.fxml"));
            Parent root = loader.load();

            ObjetFormController controller = loader.getController();
            if (objet != null) {
                controller.loadObjet(objet);
            }

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 900, 700));
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
                    Objet objet = getTableView().getItems().get(getIndex());
                    openFormWindow(objet, "Modifier un objet");
                    loadObjets();
                });

                deleteButton.setOnAction(event -> {
                    Objet objet = getTableView().getItems().get(getIndex());
                    confirmAndDelete(objet);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void confirmAndDelete(Objet objet) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer l'objet \"" + objet.getNom() + "\" ?");

        alert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .ifPresent(response -> {
                    try {
                        objetService.delete(objet.getIdObjet());
                        loadObjets();
                    } catch (Exception e) {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Erreur");
                        errorAlert.setHeaderText(null);
                        errorAlert.setContentText("Impossible de supprimer l'objet: " + e.getMessage());
                        errorAlert.showAndWait();
                    }
                });
    }
}
