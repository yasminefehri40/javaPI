package otemps.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import otemps.main.OtempsNavigator;
import otemps.main.SessionBridge;
import otemps.services.CategorieService;
import otemps.services.MediaService;
import otemps.services.ObjetService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class AdminDashboardController {

    @FXML private Label lblCategoriesCount;
    @FXML private Label lblObjetsCount;
    @FXML private Label lblMediasCount;
    @FXML private Label lblDbStatus;
    @FXML private Label lblLastUpdate;
    @FXML private Label lblStatus;
    @FXML private Label sessionUserLabel;
    @FXML private Label sessionRoleLabel;
    @FXML private PieChart categoriesPieChart;

    private CategorieService categorieService;
    private ObjetService objetService;
    private MediaService mediaService;

    @FXML
    public void initialize() {
        categorieService = new CategorieService();
        objetService = new ObjetService();
        mediaService = new MediaService();
        sessionUserLabel.setText(SessionBridge.getDisplayName());
        sessionRoleLabel.setText(SessionBridge.getRoleLabel());
        loadStatistics();
        updateTimestamp();
    }

    private void loadStatistics() {
        try {
            lblCategoriesCount.setText(String.valueOf(categorieService.getTotalCount()));
            lblObjetsCount.setText(String.valueOf(objetService.getTotalCount()));
            lblMediasCount.setText(String.valueOf(mediaService.getTotalCount()));
            lblDbStatus.setText("Connectee");
            lblStatus.setText("Systeme operationnel");
            loadCategoryUsageStats();
        } catch (Exception e) {
            System.err.println("Erreur chargement statistiques: " + e.getMessage());
            lblDbStatus.setText("Erreur de connexion");
            lblStatus.setText("Probleme systeme");
        }
    }

    private void updateTimestamp() {
        lblLastUpdate.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private void loadCategoryUsageStats() {
        if (categoriesPieChart == null) {
            return;
        }

        Map<String, Integer> stats = objetService.getStatsByCategorie();
        categoriesPieChart.getData().clear();

        if (stats.isEmpty()) {
            categoriesPieChart.setTitle("Aucune categorie utilisee");
            return;
        }

        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            categoriesPieChart.getData().add(
                    new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue())
            );
        }
        categoriesPieChart.setTitle("Repartition des categories");
        categoriesPieChart.setLabelsVisible(true);
        categoriesPieChart.setLegendVisible(true);
    }

    @FXML
    public void handleCategories() {
        openWindow("/fxml/GestiondesCat\u00E9gories.fxml", "Gestion des Categories", 1000, 700);
    }

    @FXML
    public void handleAddCategorie() {
        openWindow("/fxml/crudadmin.fxml", "Nouvelle Categorie", 800, 500);
    }

    @FXML
    public void handleObjets() {
        openWindow("/fxml/GestiondesObjets.fxml", "Gestion des Objets", 1200, 700);
    }

    @FXML
    public void handleAddObjet() {
        openWindow("/fxml/crudadminobjet.fxml", "Nouvel Objet", 950, 700);
    }

    @FXML
    public void handleMedias() {
        openWindow("/fxml/GestiondesMedias.fxml", "Gestion des Medias", 1100, 700);
    }

    @FXML
    public void handleAddMedia() {
        openWindow("/fxml/crudadminmedia.fxml", "Nouveau Media", 700, 450);
    }

    @FXML
    public void handleReports() {
        handleRefresh();
    }

    @FXML
    public void handleRefresh() {
        loadStatistics();
        updateTimestamp();
    }

    @FXML
    public void handleHome() {
        Stage stage = (Stage) lblStatus.getScene().getWindow();
        OtempsNavigator.showOnStage(stage, "/fxml/HomeView.fxml", 1200, 800);
    }

    @FXML
    public void handleBackToMainApp() {
        Stage stage = (Stage) lblStatus.getScene().getWindow();
        OtempsNavigator.showOnStage(stage, "/com/otemps/views/AdminDashboard.fxml", 1080, 720);
    }

    @FXML
    public void handleLogout() {
        SessionBridge.logout();
        Stage stage = (Stage) lblStatus.getScene().getWindow();
        OtempsNavigator.showOnStage(stage, "/com/otemps/views/LoginView.fxml", 1100, 750);
    }

    private void openWindow(String fxmlPath, String title, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            if (loader.getLocation() == null) {
                System.err.println("FXML non trouve: " + fxmlPath);
                return;
            }

            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("OTEMPS - " + title);
            stage.setScene(new Scene(root, width, height));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur ouverture fenetre: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
