package com.getaji.bmshashwatcher;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;

public class PreferenceDialogController {
    @FXML
    private CheckBox checkboxUseBeatorajaDB;

    @FXML
    private TextField fieldBeatorajaPath;

    @FXML
    private Label labelErrorBeatorajaPath;

    @FXML
    private TextField fieldLR2Path;

    @FXML
    private CheckBox checkboxUseLR2DB;

    @FXML
    private Label labelErrorLR2Path;

    private Dialog<ButtonType> dialog;

    @FXML
    public void onActionChooseBeatorajaPath() {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("beatorajaの場所を選択");
        final String currentPath = Main.getInstance().getConfig().getBeatorajaPath();
        if (!currentPath.equals("")) {
            directoryChooser.setInitialDirectory(new File(currentPath));
        }
        final Window dialogWindow = dialog.getDialogPane().getScene().getWindow();
        final File selectedDirectory = directoryChooser.showDialog(dialogWindow);
        fieldBeatorajaPath.setText(selectedDirectory.getPath());
    }

    @FXML
    public void onActionChooseLR2Path() {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("LR2の場所を選択");
        final String currentPath = Main.getInstance().getConfig().getLr2Path();
        if (!currentPath.equals("")) {
            directoryChooser.setInitialDirectory(new File(currentPath));
        }
        final Window dialogWindow = dialog.getDialogPane().getScene().getWindow();
        final File selectedDirectory = directoryChooser.showDialog(dialogWindow);
        fieldLR2Path.setText(selectedDirectory.getPath());
    }

    private void switchErrorLabel(Label label, String text) {
        label.setText(text);
        label.setVisible(!text.isEmpty());
        label.setManaged(!text.isEmpty());
    }

    public void bind(PreferenceDialogModel model) {
        checkboxUseBeatorajaDB.selectedProperty().bindBidirectional(model.useBeatorajaDBProperty());
        fieldBeatorajaPath.textProperty().bindBidirectional(model.beatorajaPathProperty());
        checkboxUseLR2DB.selectedProperty().bindBidirectional(model.useLR2DBProperty());
        fieldLR2Path.textProperty().bindBidirectional(model.lr2PathProperty());

        model.beatorajaPathProperty().addListener((observable, oldValue, newValue) -> {
            boolean isValid = newValue.isEmpty() || Main.getInstance().getBeatorajaSongDataAccessor().isValidPath(newValue);
            switchErrorLabel(
                    labelErrorBeatorajaPath,
                    isValid ? "" : "データベースが見つかりません。"
            );
        });

        model.lr2PathProperty().addListener((observable, oldValue, newValue) -> {
            boolean isValid = newValue.isEmpty() || Main.getInstance().getLr2SongDataAccessor().isValidPath(newValue);
            switchErrorLabel(
                    labelErrorLR2Path,
                    isValid ? "" : "データベースが見つかりません。"
            );
        });
    }

    public void unbind(PreferenceDialogModel model) {
        checkboxUseBeatorajaDB.selectedProperty().unbindBidirectional(model.useBeatorajaDBProperty());
        fieldBeatorajaPath.textProperty().unbindBidirectional(model.beatorajaPathProperty());
        checkboxUseLR2DB.selectedProperty().unbindBidirectional(model.useLR2DBProperty());
        fieldLR2Path.textProperty().unbindBidirectional(model.lr2PathProperty());
    }

    public void setDialog(Dialog<ButtonType> dialog) {
        this.dialog = dialog;
    }
}
