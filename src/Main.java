import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Main extends Application {
    private static Main INSTANCE;

    private final Config config;
    private final ClipboardWatcher clipboardWatcher;
    private final SongDataPoller songDataPoller;
    private final AppState appState;

    private MainWindowController controller;

    public Main() throws IOException {
        appState = new AppState();

        if (!Files.exists(Path.of("./config.json"))) {
            appState.setFirstBoot(true);
        }

        try {
            config = Config.load("./config.json");
        } catch (IOException e) {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("エラー");
            alert.setHeaderText(
                    "設定ファイル(config.json)を作成または読み込みできません。アクセス権限を確認してください"
            );
            alert.showAndWait();
            Platform.exit();
            throw e;
        }

        clipboardWatcher = new ClipboardWatcher(1000);
        songDataPoller = new SongDataPoller(config);
    }

    public static void main(String[] args) {
        Application.launch();
    }

    @Override
    public void start(Stage primaryStage) {
        INSTANCE = this;

        primaryStage.setTitle("BMS Hash Watcher v0.1.0-alpha");

        final FXMLLoader rootLoader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
        final Parent root;
        try {
            root = rootLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        controller = rootLoader.getController();
        controller.setAppState(appState);
        controller.constructContextMenu(config);

        final TableView<HashData> tableView = controller.getHashTableView();
        final ObservableList<HashData> hashDataList = FXCollections.observableArrayList();
        tableView.setItems(hashDataList);

        primaryStage.show();

        songDataPoller.setConsumer(this::onCompleteSongDataPolling);

        clipboardWatcher.addCallback(this::onUpdateClipboard);

        if (config.getBeatorajaPath().equals("")) {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("初期設定");
            alert.setHeaderText("beatorajaのディレクトリを指定してください\nキャンセルすると楽曲データを取得せずに動作します");
            alert.showAndWait();
            chooseBeatorajaDir(primaryStage.getOwner());
        }

        clipboardWatcher.start();

        controller.info("起動完了");
    }

    /**
     * beatorajaのディレクトリを選択するダイアログを表示する
     * 正しいディレクトリが選択されるまでループする
     * 正しいディレクトリが選択された場合は設定を保存する
     * キャンセルされた場合は打ち切る
     * @param owner ダイアログのオーナーウィンドウ
     */
    private void chooseBeatorajaDir(Window owner) {
        while (true) {
            // ダイアログ表示
            final DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("beatorajaのディレクトリを選択");
            final File selectedDirectory = directoryChooser.showDialog(owner);

            // キャンセルしたら打ち切り
            if (selectedDirectory == null) {
                break;
            }

            // ディレクトリの確認
            final Path dbPath = selectedDirectory.toPath().resolve("songdata.db");
            if (!Files.exists(dbPath)) {
                final Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("エラー");
                errorAlert.setHeaderText(
                        "楽曲データベースがありません。ディレクトリを確認してください"
                );
                errorAlert.showAndWait();
                continue;
            }

            config.setBeatorajaPath(selectedDirectory.getAbsolutePath());

            try {
                Config.save("./config.json", config);
            } catch (IOException e) {
                e.printStackTrace();
                final Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("エラー");
                errorAlert.setHeaderText(
                        "設定ファイル(config.json)を保存できません。アクセス権限を確認してください"
                );
                errorAlert.showAndWait();
            }

            break;
        }
    }

    private void onCompleteSongDataPolling(SongDataAccessor.Result result) {
        final ObservableList<HashData> hashDataList = controller.getHashTableView().getItems();
        final SongData songData = result.songData();
        final List<Integer> restDataIndices = new ArrayList<>();
        boolean isFound = false;

        for (int i = 0; i < hashDataList.size(); i++) {
            final HashData hashData = hashDataList.get(i);

            // すでに更新済みの場合は重複を削除する準備
            if (isFound) {
                switch (result.hashType()) {
                    case MD5 -> {
                        if (!hashData.getSHA256Hash().equals("")
                                && hashData.getSHA256Hash().equals(songData.sha256())) {
                            restDataIndices.add(i);
                        }
                    }
                    case SHA256 -> {
                        if (!hashData.getMD5Hash().equals("")
                                && hashData.getMD5Hash().equals(songData.md5())) {
                            restDataIndices.add(i);
                        }
                    }
                }
                continue;
            }

            // 更新済みでない
            if (
                    (result.hashType() == HashData.HashType.MD5 && hashData.getMD5Hash().equals(result.hash()))
                            || result.hashType() == HashData.HashType.SHA256 && hashData.getSHA256Hash().equals(result.hash())
            ) {
                if (songData == null) {
                    hashDataList.set(i, new HashData("未登録のBMS", hashData.getMD5Hash(), hashData.getSHA256Hash()));
                    break;
                } else {
                    hashDataList.set(i, new HashData(songData.getTitleFull(), songData.md5(), songData.sha256()));
                    isFound = true;
                }
            }
        }
        for (int i = restDataIndices.size() - 1; i >= 0; i--) {
            hashDataList.remove(i, i + 1);
        }
    }

    private void onUpdateClipboard(String value) {
        // このアプリがコピーしたデータなら無視
        if (appState.isCopyWithThisAppJustBefore()) {
            appState.setCopyWithThisAppJustBefore(false);
            return;
        }

        final Optional<String> sha256 = HashChecker.getSHA256HashPart(value);
        final Optional<String> md5 = HashChecker.getMD5HashPart(value);

        if (sha256.isEmpty() && md5.isEmpty()) return;

        final ObservableList<HashData> hashDataList = controller.getHashTableView().getItems();

        // 既存のリストを検索し、存在すれば一番上に移動して処理打ち切り
        for (HashData hashData : hashDataList) {
            if ((sha256.isPresent() && hashData.getSHA256Hash().equals(sha256.get()))
                    || (md5.isPresent() && hashData.getMD5Hash().equals(md5.get()))
            ) {
                hashDataList.remove(hashData);
                hashDataList.add(0, hashData);
                return;
            }
        }

        // 楽曲データを取得

        if (config.getBeatorajaPath().equals("")) {
            hashDataList.add(0, new HashData("不明のBMS"));
            return;
        }

        final HashData hashData = new HashData("取得中...");

        if (sha256.isPresent()) {
            hashData.setSHA56Hash(sha256.get());
            songDataPoller.poll(HashData.HashType.SHA256, sha256.get());
        } else {
            hashData.setMD5Hash(md5.get());
            songDataPoller.poll(HashData.HashType.MD5, md5.get());
        }

        hashDataList.add(0, hashData);
    }

    public MainWindowController getController() {
        return controller;
    }

    public static Main getInstance() {
        return INSTANCE;
    }
}
