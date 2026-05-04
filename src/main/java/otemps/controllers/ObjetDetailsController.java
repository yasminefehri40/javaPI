package otemps.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import otemps.entites.Categorie;
import otemps.entites.Media;
import otemps.entites.Objet;
import otemps.services.CategorieService;
import otemps.services.ObjetService;
import otemps.services.TranslationService;
import otemps.utils.PDFGenerator;

import java.awt.Desktop;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjetDetailsController {

    @FXML private Button btnBack, btnPrint, btnPDF, btnToggle3D;
    @FXML private Label titleLabel, mainTitleLabel, categorieLabel, epoqueLabel, origineLabel, materiauxLabel, descriptionLabel;
    @FXML private Label traductionStatus;
    @FXML private ImageView mainImageView;
    @FXML private HBox mediasContainer;
    @FXML private StackPane displayStack;
    @FXML private ComboBox<String> languageSelector;

    private final ObjetService objetService = new ObjetService();
    private final CategorieService categorieService = new CategorieService();
    private final TranslationService translationService = new TranslationService();

    private int currentObjetId;
    private boolean is3DMode = false;
    private Objet currentObjet;
    private String descriptionSource;

    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

    private final String PLACEHOLDER_URL = "https://via.placeholder.com/700x400?text=Image+Indisponible";
    private final Map<String, String> languageMap = new HashMap<>();

    @FXML
    public void initialize() {
        languageMap.put("Français (Original)", "fr");
        languageMap.put("English", "en");
        languageMap.put("Español", "es");
        languageMap.put("Deutsch", "de");
        languageMap.put("Italiano", "it");

        if (languageSelector != null) {
            languageSelector.getItems().addAll(languageMap.keySet());
            languageSelector.getSelectionModel().selectFirst();
        }
    }

    public void loadObjet(int idObjet) {
        this.currentObjetId = idObjet;
        new Thread(() -> {
            try {
                Objet objet = objetService.getById(idObjet);
                if (objet != null) {
                    this.currentObjet = objet;
                    this.descriptionSource = objet.getDescription();
                    Categorie cat = categorieService.getById(objet.getIdCategorie());
                    List<Media> medias = objet.getMedias();

                    Platform.runLater(() -> {
                        titleLabel.setText(objet.getNom());
                        mainTitleLabel.setText(objet.getNom());
                        descriptionLabel.setText(objet.getDescription() != null ? objet.getDescription() : "Aucune description");
                        epoqueLabel.setText(objet.getEpoque() != null ? objet.getEpoque() : "Non specifiee");
                        origineLabel.setText(objet.getOrigine() != null ? objet.getOrigine() : "Non specifiee");
                        materiauxLabel.setText(objet.getMateriaux() != null ? objet.getMateriaux() : "Non specifies");

                        if (cat != null) {
                            categorieLabel.setText(cat.getNomCategorie());
                        } else {
                            categorieLabel.setText("Non specifiee");
                        }

                        if (medias != null && !medias.isEmpty()) {
                            setMainImageAsync(medias.get(0).getLienFichier());
                            loadGallery(medias);
                        } else {
                            mainImageView.setImage(new Image(PLACEHOLDER_URL));
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Erreur loadObjet: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleToggle3D() {
        if (!is3DMode) {
            mainImageView.setVisible(false);
            btnToggle3D.setText("Retour vue photo");
            setup3DScene(displayStack);
            is3DMode = true;
        } else {
            displayStack.getChildren().removeIf(node -> node instanceof SubScene);
            mainImageView.setVisible(true);
            btnToggle3D.setText("Voir cet objet en 3D");
            is3DMode = false;
        }
    }

    private void setup3DScene(StackPane container) {
        Image img = mainImageView.getImage();
        if (img == null) {
            return;
        }

        try {
            double imgWidth = 400;
            double imgHeight = (img.getHeight() / img.getWidth()) * imgWidth;

            Box canvas3D = new Box(imgWidth, imgHeight, 10);
            PhongMaterial material = new PhongMaterial();
            material.setDiffuseMap(img);
            material.setSpecularColor(Color.WHITE);
            canvas3D.setMaterial(material);

            Group root3D = new Group(canvas3D);
            root3D.getTransforms().addAll(rotateX, rotateY);

            container.setOnMousePressed(event -> {
                anchorX = event.getSceneX();
                anchorY = event.getSceneY();
                anchorAngleX = rotateX.getAngle();
                anchorAngleY = rotateY.getAngle();
            });

            container.setOnMouseDragged(event -> {
                rotateX.setAngle(anchorAngleX - (anchorY - event.getSceneY()));
                rotateY.setAngle(anchorAngleY + (anchorX - event.getSceneX()));
            });

            PointLight light = new PointLight(Color.WHITE);
            light.setTranslateZ(-500);
            light.setTranslateX(200);
            light.setTranslateY(-200);

            AmbientLight ambientLight = new AmbientLight(Color.color(0.9, 0.9, 0.9));
            root3D.getChildren().addAll(light, ambientLight);

            SubScene subScene = new SubScene(root3D, 800, 500, true, SceneAntialiasing.BALANCED);
            subScene.setFill(Color.web("#0d0d0d"));

            PerspectiveCamera camera = new PerspectiveCamera(true);
            camera.setNearClip(0.1);
            camera.setFarClip(10000.0);
            camera.setTranslateZ(-900);
            subScene.setCamera(camera);

            container.getChildren().add(subScene);
        } catch (Exception e) {
            System.err.println("Erreur setup3D: " + e.getMessage());
        }
    }

    @FXML
    public void handleExportPDF() {
        if (currentObjet == null) {
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer la fiche de l'oeuvre");
        fc.setInitialFileName(mainTitleLabel.getText() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        File file = fc.showSaveDialog(btnPDF.getScene().getWindow());

        if (file != null) {
            PDFGenerator.generatePDF(currentObjet, mainImageView.getImage(), file.getAbsolutePath());
            openPdfFile(file);
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Exportation reussie");
            alert.setHeaderText(null);
            alert.setContentText("Le PDF a ete cree avec succes!");
            alert.showAndWait();
        }
    }

    private void openPdfFile(File file) {
        try {
            if (file != null && file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception e) {
            System.err.println("Impossible d'ouvrir le PDF automatiquement: " + e.getMessage());
        }
    }

    private void setMainImageAsync(String url) {
        new Thread(() -> {
            Image img = fetchImage(url);
            Platform.runLater(() -> mainImageView.setImage(img));
        }).start();
    }

    private void loadGallery(List<Media> medias) {
        mediasContainer.getChildren().clear();
        for (Media m : medias) {
            ImageView thumb = new ImageView();
            thumb.setFitHeight(80);
            thumb.setFitWidth(80);
            thumb.setPreserveRatio(true);
            thumb.setStyle("-fx-cursor: hand; -fx-border-color: #d4af37; -fx-border-width: 2; -fx-border-radius: 5;");
            new Thread(() -> {
                Image img = fetchImage(m.getLienFichier());
                Platform.runLater(() -> thumb.setImage(img));
            }).start();
            thumb.setOnMouseClicked(e -> {
                mainImageView.setImage(thumb.getImage());
                if (is3DMode) {
                    handleToggle3D();
                }
            });
            mediasContainer.getChildren().add(thumb);
        }
    }

    private Image fetchImage(String urlStr) {
        try {
            if (urlStr == null || urlStr.isEmpty()) {
                return new Image(PLACEHOLDER_URL);
            }

            if (urlStr.startsWith("http")) {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                try (InputStream is = conn.getInputStream()) {
                    return new Image(is);
                }
            }
            return new Image("file:" + urlStr);
        } catch (Exception e) {
            return new Image(PLACEHOLDER_URL);
        }
    }

    @FXML
    public void handleBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/HomeView.fxml"));
            Stage stage = (Stage) btnBack.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur handleBack: " + e.getMessage());
        }
    }

    @FXML
    public void handlePrint() {
        System.out.println("Impression de " + mainTitleLabel.getText());
    }

    @FXML
    public void onLanguageChange() {
        String selected = languageSelector.getValue();
        if (selected == null || descriptionSource == null) {
            return;
        }

        if (selected.equals("Français (Original)")) {
            descriptionLabel.setText(descriptionSource);
            traductionStatus.setText("");
            return;
        }

        traductionStatus.setText("Traduction en cours...");
        descriptionLabel.setStyle("-fx-opacity: 0.6;");

        new Thread(() -> {
            String langCode = languageMap.get(selected);
            String result = translationService.translate(descriptionSource, "fr", langCode);

            Platform.runLater(() -> {
                if (result.contains("Erreur") || result.contains("❌")) {
                    traductionStatus.setText("Traduction echouee, texte original affiche");
                    descriptionLabel.setText(descriptionSource);
                    descriptionLabel.setStyle("-fx-opacity: 1.0;");
                } else {
                    descriptionLabel.setText(result);
                    traductionStatus.setText("Traduction reussie (" + selected + ")");
                    descriptionLabel.setStyle("-fx-opacity: 1.0;");
                }
            });
        }).start();
    }
}
