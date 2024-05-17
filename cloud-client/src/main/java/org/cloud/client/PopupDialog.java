package org.cloud.client;

import javafx.scene.control.Alert;

public class PopupDialog {
    public static void showAlertDialog(Alert.AlertType dialogType, String dialogMsg) {
        Alert alert = new Alert(dialogType, dialogMsg);
        alert.showAndWait();
    }
}
