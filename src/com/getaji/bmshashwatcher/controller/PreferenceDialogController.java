package com.getaji.bmshashwatcher.controller;

import com.getaji.bmshashwatcher.ClipboardWatcher;
import com.getaji.bmshashwatcher.Main;
import com.getaji.bmshashwatcher.model.Config;
import com.getaji.bmshashwatcher.model.PreferenceDialogModel;
import com.getaji.bmshashwatcher.model.WebService;
import com.getaji.bmshashwatcher.model.WebServiceEditorModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

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

    @FXML
    private Spinner<Integer> spinnerClipboardDelay;

    @FXML
    private Label labelErrorClipboardDelay;

    @FXML
    private TableView<WebService> tableWebService;

    @FXML
    private TableColumn<WebService, String> columnWebServiceName;

    @FXML
    private TableColumn<WebService, String> columnWebServiceMD5Pattern;

    @FXML
    private TableColumn<WebService, String> columnWebServiceSHA256Pattern;

    private Dialog<ButtonType> dialog;

    private final Dialog<ButtonType> cacheDialogWebServiceEdit = null;

    @FXML
    public void initialize() {
        // 楽曲DBタブ
        spinnerClipboardDelay.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(100, Integer.MAX_VALUE));

        // Webサービスタブ
        tableWebService.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        columnWebServiceName.setCellValueFactory(new PropertyValueFactory<>("title"));
        columnWebServiceMD5Pattern.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getMD5UrlPattern())
        );
        columnWebServiceSHA256Pattern.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getSHA256UrlPattern())
        );
    }

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

    @FXML
    public void onClickWebService(MouseEvent event) {
        if (event.getClickCount() == 2) {
            final WebService selectedService =
                    tableWebService.getSelectionModel().getSelectedItem();
            if (selectedService != null) {
                openWebServiceEditor(selectedService);
            }
        }
    }

    @FXML
    private void onActionMoveUpWebService() {
        final int selectedIndex = tableWebService.getSelectionModel().getSelectedIndex();
        if (selectedIndex == 0) return;
        final WebService selectedItem = tableWebService.getSelectionModel().getSelectedItem();
        tableWebService.getItems().remove(selectedIndex);
        tableWebService.getItems().add(selectedIndex - 1, selectedItem);
        tableWebService.getSelectionModel().clearAndSelect(selectedIndex - 1);
    }

    @FXML
    private void onActionMoveDownWebService() {
        final int selectedIndex = tableWebService.getSelectionModel().getSelectedIndex();
        if (selectedIndex == tableWebService.getItems().size() - 1) return;
        final WebService selectedItem = tableWebService.getSelectionModel().getSelectedItem();
        tableWebService.getItems().remove(selectedIndex);
        tableWebService.getItems().add(selectedIndex + 1, selectedItem);
        tableWebService.getSelectionModel().clearAndSelect(selectedIndex + 1);
    }

    @FXML
    public void onActionAddWebService() {
        openWebServiceEditor();
    }

    @FXML
    public void onActionEditWebService() {
        final WebService selectedService = tableWebService.getSelectionModel().getSelectedItem();
        if (selectedService != null) {
            openWebServiceEditor(selectedService);
        }
    }

    @FXML
    public void onActionRemoveWebService() {
        final WebService webService = tableWebService.getSelectionModel().getSelectedItem();
        if (webService == null) return;

        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("確認");
        alert.setHeaderText("「" + webService.getTitle() + "」を削除しますか？");
        Optional<ButtonType> result = alert.showAndWait();
        result.ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                tableWebService.getItems().remove(webService);
            }
        });
    }

    @FXML
    public void onActionRestoreDefaultWebService() {
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("確認");
        alert.setHeaderText("登録されているWebサービスをデフォルトに戻しますか？\n（設定を保存するまで反映されません）");
        Optional<ButtonType> result = alert.showAndWait();
        result.ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                tableWebService.getItems().clear();
                tableWebService.getItems().setAll(Config.DEFAULT_WEB_SERVICE_LIST);
            }
        });
    }

    private void openWebServiceEditor() {
        openWebServiceEditor(new WebService("", "", ""));
    }

    private void onDoneEditWebService(WebService oldValue, WebService newValue) {
        final int index = tableWebService.getItems().indexOf(oldValue);
        if (index == -1) {
            tableWebService.getItems().add(newValue);
        } else {
            tableWebService.getItems().set(index, newValue);
        }
    }

    private void openWebServiceEditor(WebService webService) {
        final Dialog<ButtonType> dialog = new Dialog<>();
        final FXMLLoader rootLoader = new FXMLLoader(Main.class.getResource("fxml" +
                "/WebServiceEditor.fxml"));
        final Parent root;
        try {
            root = rootLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        dialog.setTitle("Webサービスの編集");
        dialog.setResizable(true);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(root);

        // controller <-> model
        final WebServiceEditorModel model = new WebServiceEditorModel();
        final WebServiceEditorController editorController = rootLoader.getController();
        editorController.bind(model);

        // webService instance => model
        model.nameProperty().set(webService.getTitle());
        model.md5UrlPatternProperty().set(webService.getMD5UrlPattern());
        model.sha256UrlPatternProperty().set(webService.getSHA256UrlPattern());

        model.nameProperty().addListener((observable, oldValue, newValue) ->
                dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(newValue.isBlank())
        );
        dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(webService.getTitle().isBlank());

        final Optional<ButtonType> result = dialog.showAndWait();
        result.ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                final WebService newWebService = new WebService(
                        model.getName(),
                        model.getMd5UrlPattern(),
                        model.getSha256UrlPattern()
                );
                onDoneEditWebService(webService, newWebService);
            }
        });
        editorController.unbind(model);
    }

    private void switchErrorLabel(Label label, String text) {
        label.setText(text);
        label.setVisible(!text.isEmpty());
        label.setManaged(!text.isEmpty());
        setEnableDialogOK(text.isEmpty());
    }

    public void bind(PreferenceDialogModel model) {
        checkboxUseBeatorajaDB.selectedProperty().bindBidirectional(model.useBeatorajaDBProperty());
        fieldBeatorajaPath.textProperty().bindBidirectional(model.beatorajaPathProperty());
        checkboxUseLR2DB.selectedProperty().bindBidirectional(model.useLR2DBProperty());
        fieldLR2Path.textProperty().bindBidirectional(model.lr2PathProperty());

        model.beatorajaPathProperty().addListener((observable, oldValue, newValue) -> {
            boolean isValid =
                    newValue.isEmpty() || Main.getInstance().getBeatorajaSongDataAccessor().isValidPath(newValue);
            switchErrorLabel(
                    labelErrorBeatorajaPath,
                    isValid ? "" : "データベースが見つかりません。"
            );
        });

        model.lr2PathProperty().addListener((observable, oldValue, newValue) -> {
            boolean isValid =
                    newValue.isEmpty() || Main.getInstance().getLr2SongDataAccessor().isValidPath(newValue);
            switchErrorLabel(
                    labelErrorLR2Path,
                    isValid ? "" : "データベースが見つかりません。"
            );
        });

        model.clipboardDelayProperty().asObject().bindBidirectional(spinnerClipboardDelay.getValueFactory().valueProperty());

        spinnerClipboardDelay.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                switchErrorLabel(labelErrorClipboardDelay, "正しい値を指定してください");
            } else if (newValue < ClipboardWatcher.DELAY_LOWER_LIMIT) {
                switchErrorLabel(labelErrorClipboardDelay, "100ミリ秒（0.1秒）以上に設定してください");
            } else {
                switchErrorLabel(labelErrorClipboardDelay, "");
            }
        });

        tableWebService.itemsProperty().bindBidirectional(model.webServicesProperty());
    }

    public void unbind(PreferenceDialogModel model) {
        checkboxUseBeatorajaDB.selectedProperty().unbindBidirectional(model.useBeatorajaDBProperty());
        fieldBeatorajaPath.textProperty().unbindBidirectional(model.beatorajaPathProperty());
        checkboxUseLR2DB.selectedProperty().unbindBidirectional(model.useLR2DBProperty());
        fieldLR2Path.textProperty().unbindBidirectional(model.lr2PathProperty());
        tableWebService.itemsProperty().unbindBidirectional(model.webServicesProperty());
    }

    public void setDialog(Dialog<ButtonType> dialog) {
        this.dialog = dialog;
    }

    public void setEnableDialogOK(boolean isEnable) {
        dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(!isEnable);
    }

    public void onPreApply(PreferenceDialogModel model) {
        model.clipboardDelayProperty().set(spinnerClipboardDelay.getValue());
    }
}
