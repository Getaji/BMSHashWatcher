package com.getaji.bmshashwatcher.controller;

import com.getaji.bmshashwatcher.Main;
import com.getaji.bmshashwatcher.model.*;
import com.getaji.bmshashwatcher.view.TypedMenuItem;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class MainWindowController {
    @FXML
    private CheckMenuItem menuItemToggleWatchClipboard;

    @FXML
    private TableView<BMSHashData> hashTableView;

    @FXML
    private TableColumn<BMSHashData, String> titleColumn;

    @FXML
    private TableColumn<BMSHashData, String> md5HashColumn;

    @FXML
    private TableColumn<BMSHashData, String> sha256HashColumn;

    @FXML
    private Label bottomMessageLabel;

    private AppState appState;

    private ContextMenu contextMenu;

    private Stage primaryStage;

    @FXML
    public void initialize() {
        menuItemToggleWatchClipboard.setSelected(
                Main.getInstance().getConfig().isEnableWatchClipboard()
        );
        hashTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        md5HashColumn.setCellValueFactory(new PropertyValueFactory<>("md5Hash"));
        sha256HashColumn.setCellValueFactory(new PropertyValueFactory<>("sha256Hash"));
    }

    @FXML
    public void onActionChooseBeatorajaDir() {
        if (Main.getInstance().chooseBeatorajaDir(primaryStage)) {
            info("beatorajaのディレクトリを選択しました");
        }
    }

    @FXML
    public void onActionChooseLR2Dir() {
        if (Main.getInstance().chooseLR2Dir(primaryStage)) {
            info("LR2のディレクトリを選択しました");
        }
    }

    @FXML
    public void onActionOpenPreference() {
        Main.getInstance().openPreference();
    }

    @FXML
    public void onActionToggleWatchClipboard() {
        Main.getInstance().setEnableClipboardWatcher(menuItemToggleWatchClipboard.isSelected());
    }

    @FXML
    public void onActionClearList() {
        hashTableView.getItems().clear();
    }

    @FXML
    public void onActionQuit() {
        Platform.exit();
    }

    @FXML
    public void onContextMenuRequested(ContextMenuEvent event) {
        final ObservableList<BMSHashData> selectedItems =
                hashTableView.getSelectionModel().getSelectedItems();
        if (selectedItems.size() != 1) return;
        final BMSHashData hashData = selectedItems.get(0);
        contextMenu.getItems().forEach(item -> {
            WebService webService = null;
            if (item instanceof TypedMenuItem<?>) {
                final Object value = ((TypedMenuItem<?>) item).getValue();
                if (value instanceof WebService) {
                    webService = (WebService) value;
                }
            }
            if (webService != null) {
                switch (webService.getSupportedHashType()) {
                    case MD5 -> item.setDisable(hashData.getMD5Hash().isEmpty());
                    case SHA256 -> item.setDisable(hashData.getSHA256Hash().isEmpty());
                    case MD5_AND_SHA256 ->
                            item.setDisable(hashData.getMD5Hash().isEmpty() && hashData.getSHA256Hash().isEmpty());
                    case NONE -> item.setDisable(true);
                }
            } else {
                switch (Optional.ofNullable(item.getId()).orElse("")) {
                    case "contextMenu-itemCopyMD5Hash" ->
                            item.setDisable(hashData.getMD5Hash().equals(""));
                    case "contextMenu-itemCopySHA256Hash" ->
                            item.setDisable(hashData.getSHA256Hash().equals(""));
                    default -> item.setDisable(false);
                }
            }
        });
    }

    public void bind(MainWindowModel model) {
        hashTableView.itemsProperty().bindBidirectional(model.hashListProperty());
    }

    public void constructContextMenu(Config config) {
        contextMenu = new ContextMenu();
        final ObservableList<MenuItem> menuItems = contextMenu.getItems();

        config.getWebServiceList().forEach(webService -> {
            final TypedMenuItem<WebService> item = new TypedMenuItem<>(webService.getTitle() +
                    "で開く");
            item.setValue(webService);
            item.setOnAction(ev -> hashTableView.getSelectionModel().getSelectedItems()
                    .forEach(webService::browse));
            menuItems.add(item);
        });

        final MenuItem itemBrowseAll = new MenuItem("すべてのサービスで開く");
        itemBrowseAll.setOnAction(ev ->
                hashTableView.getSelectionModel().getSelectedItems().forEach(hashData ->
                        config.getWebServiceList().forEach(
                                webService -> webService.browse(hashData)
                        )
                )
        );
        menuItems.add(itemBrowseAll);

        menuItems.add(new SeparatorMenuItem());

        final MenuItem itemCopyTitle = new MenuItem("タイトルをコピー");
        itemCopyTitle.setId("contextMenu-itemCopyTitle");
        itemCopyTitle.setOnAction(ev ->
                Optional.ofNullable(hashTableView.getSelectionModel().getSelectedItem()).ifPresent(
                        hashData -> {
                            final ClipboardContent content = new ClipboardContent();
                            content.putString(hashData.getTitle());
                            appState.setCopyWithThisAppJustBefore(true);
                            Clipboard.getSystemClipboard().setContent(content);
                        }
                )
        );
        menuItems.add(itemCopyTitle);

        final MenuItem itemCopyMD5 = new MenuItem("MD5ハッシュをコピー");
        itemCopyMD5.setId("contextMenu-itemCopyMD5Hash");
        itemCopyMD5.setOnAction(ev ->
                Optional.ofNullable(hashTableView.getSelectionModel().getSelectedItem()).ifPresent(
                        hashData -> {
                            final ClipboardContent content = new ClipboardContent();
                            content.putString(hashData.getMD5Hash());
                            appState.setCopyWithThisAppJustBefore(true);
                            Clipboard.getSystemClipboard().setContent(content);
                        }
                )
        );
        menuItems.add(itemCopyMD5);

        final MenuItem itemCopySHA256 = new MenuItem("SHA256ハッシュをコピー");
        itemCopySHA256.setId("contextMenu-itemCopySHA256Hash");
        itemCopySHA256.setOnAction(ev ->
                Optional.ofNullable(hashTableView.getSelectionModel().getSelectedItem()).ifPresent(
                        hashData -> {
                            final ClipboardContent content = new ClipboardContent();
                            content.putString(hashData.getSHA256Hash());
                            appState.setCopyWithThisAppJustBefore(true);
                            Clipboard.getSystemClipboard().setContent(content);
                        }
                )
        );
        menuItems.add(itemCopySHA256);

        hashTableView.setContextMenu(contextMenu);
    }

    public TableView<BMSHashData> getHashTableView() {
        return hashTableView;
    }

    public void setAppState(AppState appState) {
        this.appState = appState;
    }

    public void setMessage(MessageType type, String message) {
        final String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        final String color;
        switch (type) {
            case INFO -> color = "transparent";
            case WARN -> color = "#FFFF90";
            case ERROR -> color = "#FFCCCC";
            default -> throw new IllegalArgumentException("type \"" + type + "\"");
        }
        Platform.runLater(() -> {
            bottomMessageLabel.setText("[" + time + "] " + message);
            bottomMessageLabel.setStyle("-fx-background-color: " + color);
        });
    }

    public void info(String message) {
        setMessage(MessageType.INFO, message);
    }

    public void warn(String message) {
        setMessage(MessageType.WARN, message);
    }

    public void error(String message) {
        setMessage(MessageType.ERROR, message);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setEnableWatchClipboard(boolean isEnable) {
        menuItemToggleWatchClipboard.setSelected(isEnable);
    }

    public enum MessageType {
        INFO,
        WARN,
        ERROR,
    }
}
