package org.cloud.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class CloudClient extends Application {

    private static CloudClient cloudClient;
    private FXMLLoader currentfxmlLoader;
    @Override
    public void start(Stage stage) throws IOException {
        cloudClient = this;
        currentfxmlLoader = new FXMLLoader(CloudClient.class.getResource("cloud-login-view.fxml"));
        Scene scene = new Scene(currentfxmlLoader.load());
        stage.setTitle("Cloud client");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

    }
    @Override
    public void stop() {
        if (currentfxmlLoader.getController() instanceof CloudMainController controller) {
            log.debug("Closing main controller");
            controller.shutdownExecutor();
        }
    }

    public static void main(String[] args) {
        launch();
    }

    public static CloudClient getCloudClientInst() {
        return cloudClient;
    }

    public void updateCurrentLoader(FXMLLoader loader) {
        currentfxmlLoader = loader;
    }
}