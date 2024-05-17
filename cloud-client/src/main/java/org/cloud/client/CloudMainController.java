package org.cloud.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;

import javafx.fxml.Initializable;
import org.cloud.common.*;
import org.cloud.model.*;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class CloudMainController implements Initializable, NetworkHandler {
    @FXML
    private Button downloadButton;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private ListView<String> clientView;
    @FXML
    private ListView<String> serverView;

    DaemonThreadFactory factory = new DaemonThreadFactory();

    private String currentDirectory;

    private FileFlow fileFlow;

    private ClientNetwork network;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(factory);
    public void handleUploadButton() {
        uploadFile();
    }

    public void handleDownloadButton() {
        downloadFile();
    }

    public void handleClientViewUploadMI() {
        uploadFile();
    }
    public void handleClientViewRenameMI() {
        renameFile(SideType.CLIENT);
    }

    public void handleClientViewDeleteMI() {
        deleteFile(SideType.CLIENT);
    }

    public void handleClientViewKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.DELETE)) {
            deleteFile(SideType.CLIENT);
        }
    }

    public void handleServerViewDownloadMI() {
        downloadFile();
    }

    public void handleServerViewRenameMI() {
        renameFile(SideType.SERVER);
    }

    public void handleServerViewDeleteMI() {
        deleteFile(SideType.SERVER);
    }

    public void handleServerViewKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.DELETE)) {
            deleteFile(SideType.SERVER);
        }
    }

    @Override
    public <T extends CloudMessage> void writeToNetwork(T cloudMessage) throws RuntimeException {
        try {
            network.getOutputStream().writeObject(cloudMessage);
        } catch (IOException e) {
            log.error("failed to send {} to server", cloudMessage.getType(), e);
            throw new RuntimeException("failed to send object to server");
        }
    }

    @Override
    public void addUpwardNavigation(List<String> list) {
        list.add("..");
    }

    @Override
    public void updateCurrentDirContent() {
        Platform.runLater(() -> fillView(clientView,
                FileUtils.getFilesFromDir(currentDirectory, this)));
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setCurrentDirectory(System.getProperty("user.home"));
        networkInit();
        clientView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = currentDirectory + "/" + clientView.getSelectionModel().getSelectedItem();
                File selectedFile = new File(selected);
                if (selectedFile.isDirectory()) {
                    setCurrentDirectory(selected);
                }
            }
        });
        serverView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = serverView.getSelectionModel().getSelectedItem();
                writeToNetwork(new NavigationRequest(selected));
            }
        });
    }

    @Override
    public FileFlow getFileFlow() {
        return fileFlow;
    }

    @Override
    public void setProgress(double percentage) {
        Platform.runLater(() -> progressBar.setProgress(percentage));
    }

    @Override
    public void onGettingLastFileCut() {
        Platform.runLater(() -> downloadButton.setDisable(false));
    }

    public void shutdownExecutor() {
        executor.shutdown();
    }

    private void setCurrentDirectory(String directory) {
        currentDirectory = directory;
        fillView(clientView, FileUtils.getFilesFromDir(currentDirectory, this));
    }

    private void fillView(ListView<String> view, List<String> data) {
        view.getItems().clear();
        view.getItems().setAll(data);
    }

    private void networkInit() {
        try {
            network = ClientNetwork.getNetwork();
            log.info("Connection to the server is received");
            factory.getNamedThread(this::readMessages,
                    "client-listener-thread-"
            ).start();
        } catch (IOException e) {
            log.error("Failed to connect to the server", e);
            throw new RuntimeException(e);
        }
    }

    private void readMessages() {
        try {
            log.info("Socket start listening...");
            while (true) {
                CloudMessage message = (CloudMessage) network.getInputStream().readObject();
                log.debug("Message with type {} received: ", message.getType());
                handleMessage(message);
            }
        } catch (IOException e) {
            log.debug("Client disconnected");
            handleNetworkLost();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleNetworkLost() {
        if (fileFlow != null) {
            fileFlow.close();
        }
        ClientNetwork.disconnect();
        Platform.runLater(() -> {
            PopupDialog.showAlertDialog(Alert.AlertType.ERROR, "Connection to the server is lost");
            SceneSwitcher.switchScene(SceneSwitcher.getStage(clientView), "cloud-login-view.fxml");
        });
    }

    private void handleMessage(CloudMessage message) throws IOException{
        switch (message) {
            case ListDirMessage listDirMessage:
                Platform.runLater(() -> fillView(serverView, listDirMessage.getFiles()));
                break;
            case FileCutMessage fileCut:
                if (fileFlow == null || !fileFlow.isReceiving()) {
                    fileFlow = new FileFlow(currentDirectory, fileCut.getFileName());
                }
                FileUtils.handleFileCut(fileCut, this);
                break;
            case ProgressMessage progressMessage:
                setProgress(progressMessage.getPercentage());
                break;
            default:
                log.error("Unknown Message type received");
                break;
        }
    }

    private void uploadFile() {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        executor.execute(() -> FileUtils.sendFileToNetwork(currentDirectory, fileName, this));
    }

    private void downloadFile() {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        writeToNetwork(new FileRequest(fileName));
        Platform.runLater(() -> downloadButton.setDisable(true));
    }

    private void renameFile(SideType sideType) {
        String fileName = getSelectedFileName(sideType);
        TextInputDialog renameDialog = new TextInputDialog(fileName);
        renameDialog.setHeaderText("Rename?");
        renameDialog.showAndWait()
                .ifPresent(response -> {
                    if (sideType.equals(SideType.CLIENT)) {
                        File file = new File(currentDirectory + "/" + fileName);
                        file.renameTo(new File(currentDirectory, response));
                        fillView(clientView, FileUtils.getFilesFromDir(currentDirectory, this));
                    } else if (sideType.equals(SideType.SERVER)) {
                        writeToNetwork(new RenameRequest(fileName, response));
                    } else {
                        log.error("Unknown side type received");
                        throw new RuntimeException("Unknown side type received");
                    }
                });
    }

    private void deleteFile(SideType sideType) {
        String fileName = getSelectedFileName(sideType);
        Alert deleteAlert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + fileName +"?");
        deleteAlert.setHeaderText("Delete?");
        deleteAlert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .ifPresent(response -> {
                    if (sideType.equals(SideType.CLIENT)) {
                        new File(currentDirectory + "/" + fileName).delete();
                        fillView(clientView, FileUtils.getFilesFromDir(currentDirectory, this));
                    } else if (sideType.equals(SideType.SERVER)) {
                        writeToNetwork(new DeleteRequest(fileName));
                    } else {
                        log.error("Unknown side type received");
                        throw new RuntimeException("Unknown side type received");
                    }
                });
    }

    private String getSelectedFileName(SideType side) {
        if (side.equals(SideType.CLIENT)) {
            return clientView.getSelectionModel().getSelectedItem();
        } else if (side.equals(SideType.SERVER)) {
            return serverView.getSelectionModel().getSelectedItem();
        } else {
            log.error("Unknown side type received");
            throw new RuntimeException("Unknown side type received");
        }
    }
}
