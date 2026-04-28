package otemps.main;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

public final class OtempsNavigator {

    private OtempsNavigator() {
    }

    public static void showOnStage(Stage stage, String fxmlPath, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(OtempsNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
            if (width > 0) {
                stage.setWidth(width);
            }
            if (height > 0) {
                stage.setHeight(height);
            }
        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger " + fxmlPath, e);
        }
    }
}
