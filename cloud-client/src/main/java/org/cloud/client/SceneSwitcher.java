package org.cloud.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneSwitcher {

    public static void switchScene(Stage stage, String fxmlName) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(CloudClient.class.getResource(fxmlName));
            Scene scene = new Scene(fxmlLoader.load());
            stage.setScene(scene);
            CloudClient.getCloudClientInst().updateCurrentLoader(fxmlLoader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Stage getStage(ActionEvent event) {
        return (Stage) ((Node) event.getSource()).getScene().getWindow();
    }

    public static Stage getStage(Node node) {
        return (Stage) node.getScene().getWindow();
    }
}
