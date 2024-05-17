package org.cloud.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;
import org.cloud.common.FieldVerificationException;
import org.cloud.common.FieldVerifier;
import org.cloud.model.*;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class CloudLoginController implements Initializable {
    @FXML
    private Label feedbackLabel;
    @FXML
    private PasswordField passField;
    @FXML
    private TextField nameField;
    private String username;
    private String password;

    private ClientNetwork network;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
    }

    public void handleSignupButton(ActionEvent actionEvent) {
        SceneSwitcher.switchScene(SceneSwitcher.getStage(actionEvent), "cloud-signup-view.fxml");
    }

    public void handleLoginButton(ActionEvent actionEvent) {
        try {
            username = nameField.getText();
            password = passField.getText();
            FieldVerifier.verifyCredentials(username, password);
            network = ClientNetwork.getNetwork();
            log.debug("Connection to the server is received");
            network.getOutputStream().writeObject(new LoginMessage(username, password));
            CloudMessage message = (CloudMessage) network.getInputStream().readObject();
            log.debug("Message with type {} received: ", message.getType());
            handleMessage(actionEvent, message);
        } catch (FieldVerificationException e) {
            Platform.runLater(() -> feedbackLabel.setText(e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to connect to the server", e);
            Platform.runLater(() -> feedbackLabel.setText("Failed to connect to the server"));
            ClientNetwork.disconnect();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleMessage(ActionEvent actionEvent, CloudMessage message) throws IOException {
        switch (message) {
            case AcceptMessage acceptMessage:
                SceneSwitcher.switchScene(SceneSwitcher.getStage(actionEvent), "cloud-client-view.fxml");
                break;
            case DenyMessage denyMessage:
                String reason = denyMessage.getReason();
                log.debug("Authorization failure: {}", reason);
                Platform.runLater(() -> feedbackLabel.setText("Authorization failure: " + reason));
                break;
            case ErrorMessage errorMessage:
                String issue = errorMessage.getIssue();
                log.debug("Authorization failure: {}", issue);
                Platform.runLater(() -> feedbackLabel.setText("Authorization failure: " + issue));
                break;
            default:
                log.error("irrelevant Message type received");
                Platform.runLater(() -> feedbackLabel.setText("Authorization failure"));
                break;
        }
    }
}
