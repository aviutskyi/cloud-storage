package org.cloud.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
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
public class CloudSignupController implements Initializable {

    @FXML
    private Label feedbackLabel;
    @FXML
    private TextField nameField;
    @FXML
    private PasswordField passField;
    @FXML
    private PasswordField confirmPassField;

    private String username;
    private String password;
    private String confirmPassword;

    private ClientNetwork network;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
    }

    public void handleLoginButton(ActionEvent actionEvent) {
        SceneSwitcher.switchScene(SceneSwitcher.getStage(actionEvent), "cloud-login-view.fxml");
    }

    public void handleSignUpButton(ActionEvent actionEvent) {
        try {
            username = nameField.getText();
            password = passField.getText();
            confirmPassword = confirmPassField.getText();
            FieldVerifier.verifyCredentials(username, password);
            FieldVerifier.verifyPasswordMatching(password, confirmPassword);
            network = ClientNetwork.getNetwork();
            log.debug("Connection to the server is received");
            network.getOutputStream().writeObject(new SignupMessage(username, password));
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
                Platform.runLater(() -> PopupDialog.showAlertDialog(Alert.AlertType.INFORMATION,
                        "User " + username + " has been successfully created"));
                SceneSwitcher.switchScene(SceneSwitcher.getStage(actionEvent), "cloud-client-view.fxml");
                break;
            case ErrorMessage errorMessage:
                String issue = errorMessage.getIssue();
                log.debug("Registration failure: {}", issue);
                Platform.runLater(() -> feedbackLabel.setText("Registration failure: " + issue));
                break;
            default:
                log.error("irrelevant Message type received");
                Platform.runLater(() -> feedbackLabel.setText("Registration failure"));
                break;
        }
    }
}
