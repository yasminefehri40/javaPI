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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import otemps.entites.Media;
import otemps.entites.Objet;
import otemps.services.MediaService;
import otemps.services.ObjetService;

import java.io.IOException;
import java.util.List;

public class MediaController {

    @FXML private TableView<Media> tableMedias;
    @FXML private TableColumn<Media, String> colObjet;
    @FXML private TableColumn<Media, String> colType;
    @FXML private TableColumn<Media, String> colFichier;
    @FXML private TableColumn<Media, Void> colActions;
    @FXML private ComboBox<Objet> cbObjet;
    @FXML private Label lblTotal;
    @FXML private Button btnUpload;

    private final MediaService mediaService = new MediaService();
    private final ObjetService objetService = new ObjetService();

    @FXML
    public void initialize() {
        setupColumns();
        loadObjets();
        loadMedias();
    }

    private void setupColumns() {
        colObjet.setCellValueFactory(cellData -> {
            Objet objet = objetService.getById(cellData.getValue().getIdObjet());
            String nomObjet = objet != null ? objet.getNom() : "Objet inconnu";
            return new javafx.beans.property.SimpleStringProperty(nomObjet);
        });
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colFichier.setCellValueFactory(new PropertyValueFactory<>("lienFichier"));
        setupActionsColumn();
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

    private void loadMedias() {
        List<Media> medias = mediaService.afficher();
        tableMedias.getItems().setAll(medias);
        lblTotal.setText("Total: " + medias.size() + " medias");
    }

    @FXML
    public void handleUpload() {
        openFormWindow(null, "Ajouter un media");
        loadMedias();
    }

    @FXML
    public void handleFiltrer() {
        if (cbObjet.getValue() != null) {
            List<Media> medias = mediaService.getByObjet(cbObjet.getValue().getIdObjet());
            tableMedias.getItems().setAll(medias);
            lblTotal.setText("Total: " + medias.size() + " medias");
        } else {
            loadMedias();
        }
    }

    @FXML
    public void handleRetour() {
        Stage stage = (Stage) btnUpload.getScene().getWindow();
        stage.close();
    }

    private void openFormWindow(Media media, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/crudadminmedia.fxml"));
            Parent root = loader.load();

            MediaFormController controller = loader.getController();
            if (media != null) {
                controller.loadMedia(media);
            }

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 650, 400));
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
                    Media media = getTableView().getItems().get(getIndex());
                    openFormWindow(media, "Modifier un media");
                    loadMedias();
                });

                deleteButton.setOnAction(event -> {
                    Media media = getTableView().getItems().get(getIndex());
                    confirmAndDelete(media);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void confirmAndDelete(Media media) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText(null);
        alert.setContentText("Supprimer ce media ?");

        alert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .ifPresent(response -> {
                    try {
                        mediaService.delete(media.getIdMedia());
                        loadMedias();
                    } catch (Exception e) {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Erreur");
                        errorAlert.setHeaderText(null);
                        errorAlert.setContentText("Impossible de supprimer le media: " + e.getMessage());
                        errorAlert.showAndWait();
                    }
                });
    }
}
