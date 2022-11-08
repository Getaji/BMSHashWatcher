import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;

import java.util.Optional;

public class MainWindowController {
    @FXML
    private TableView<HashData> hashTableView;

    @FXML
    private TableColumn<HashData, String> titleColumn;

    @FXML
    private TableColumn<HashData, String> md5HashColumn;

    @FXML
    private TableColumn<HashData, String> sha256HashColumn;

    private AppState appState;

    private ContextMenu contextMenu;

    @FXML
    public void initialize() {
        hashTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        md5HashColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getMD5Hash())
        );
        sha256HashColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getSHA256Hash())
        );
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
        final ObservableList<HashData> selectedItems = hashTableView.getSelectionModel().getSelectedItems();
        if (selectedItems.size() != 1) return;
        final HashData hashData = selectedItems.get(0);
        contextMenu.getItems().forEach(item -> {
            // なんとかして
            switch (Optional.ofNullable(item.getId()).orElse("")) {
                case "contextMenu-itemCopyMD5Hash":
                case "contextMenu-itemBrowseLR2IR":
                    item.setDisable(hashData.getMD5Hash().equals(""));
                    break;
                case "contextMenu-itemCopySHA256Hash":
                case "contextMenu-itemBrowseMocha-Repository":
                case "contextMenu-itemBrowseMinIR":
                case "contextMenu-itemBrowseCinnamon":
                    item.setDisable(hashData.getSHA256Hash().equals(""));
                    break;
                default:
                    item.setDisable(false);
            }
        });
    }

    public void constructContextMenu(Config config) {
        contextMenu = new ContextMenu();
        ObservableList<MenuItem> menuItems = contextMenu.getItems();

        config.getWebServiceList().forEach(webService -> {
            final MenuItem item = new MenuItem(webService.getTitle() + "で開く");
            item.setId("contextMenu-itemBrowse" + webService.getTitle());
            item.setOnAction(ev -> {
                hashTableView.getSelectionModel().getSelectedItems()
                        .forEach(webService::browse);
            });
            menuItems.add(item);
        });

        final MenuItem itemBrowseAll = new MenuItem("すべてのサービスで開く");
        itemBrowseAll.setOnAction(ev -> {
            hashTableView.getSelectionModel().getSelectedItems().forEach(hashData -> {
                config.getWebServiceList().forEach(
                        webService -> webService.browse(hashData)
                );
            });
        });
        menuItems.add(itemBrowseAll);

        menuItems.add(new SeparatorMenuItem());

        final MenuItem itemCopyTitle = new MenuItem("タイトルをコピー");
        itemCopyTitle.setId("contextMenu-itemCopyTitle");
        itemCopyTitle.setOnAction(ev -> {
            Optional.ofNullable(hashTableView.getSelectionModel().getSelectedItem()).ifPresent(
                    hashData -> {
                        final ClipboardContent content = new ClipboardContent();
                        content.putString(hashData.getTitle());
                        appState.setCopyWithThisAppJustBefore(true);
                        Clipboard.getSystemClipboard().setContent(content);
                    }
            );
        });
        menuItems.add(itemCopyTitle);

        final MenuItem itemCopyMD5 = new MenuItem("MD5ハッシュをコピー");
        itemCopyMD5.setId("contextMenu-itemCopyMD5Hash");
        itemCopyMD5.setOnAction(ev -> {
            Optional.ofNullable(hashTableView.getSelectionModel().getSelectedItem()).ifPresent(
                    hashData -> {
                        final ClipboardContent content = new ClipboardContent();
                        content.putString(hashData.getMD5Hash());
                        appState.setCopyWithThisAppJustBefore(true);
                        Clipboard.getSystemClipboard().setContent(content);
                    }
            );
        });
        menuItems.add(itemCopyMD5);

        final MenuItem itemCopySHA256 = new MenuItem("SHA256ハッシュをコピー");
        itemCopySHA256.setId("contextMenu-itemCopySHA256Hash");
        itemCopySHA256.setOnAction(ev -> {
            Optional.ofNullable(hashTableView.getSelectionModel().getSelectedItem()).ifPresent(
                    hashData -> {
                        final ClipboardContent content = new ClipboardContent();
                        content.putString(hashData.getSHA256Hash());
                        appState.setCopyWithThisAppJustBefore(true);
                        Clipboard.getSystemClipboard().setContent(content);
                    }
            );
        });
        menuItems.add(itemCopySHA256);

        hashTableView.setContextMenu(contextMenu);
    }

    public TableView<HashData> getHashTableView() {
        return hashTableView;
    }

    public void setAppState(AppState appState) {
        this.appState = appState;
    }
}
