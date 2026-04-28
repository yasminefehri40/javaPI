package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Start in User view (Front-office)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/otemps/views/UserEventList.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1100, 750);
        
        primaryStage.setTitle("OTEMPS - Valorisation du Patrimoine");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
