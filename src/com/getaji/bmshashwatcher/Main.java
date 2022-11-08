package com.getaji.bmshashwatcher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class Main extends Application {
    public static final String APP_VERSION = "0.1.0";

    private static Main INSTANCE;

    private final Config config;

    private final ClipboardWatcher clipboardWatcher;
    private final SongDataAccessor beatorajaSongDataAccessor;
    private final SongDataAccessor lr2SongDataAccessor;
    private final SongDataPollingController songDataPollingController;
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
        beatorajaSongDataAccessor = new BeatorajaSongDataAccessor();
        lr2SongDataAccessor = new LR2SongDataAccessor();
        songDataPollingController = new SongDataPollingController();

        if (!config.getBeatorajaPath().equals("")) {
            songDataPollingController.addAccessor(beatorajaSongDataAccessor);
        }
        if (!config.getLr2Path().equals("")) {
            songDataPollingController.addAccessor(lr2SongDataAccessor);
        }
    }

    public static void main(String[] args) {
        Application.launch();
    }

    @Override
    public void start(Stage primaryStage) {
        INSTANCE = this;

        primaryStage.setTitle("BMS Hash Watcher v" + APP_VERSION);

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
        controller.setPrimaryStage(primaryStage);

        final TableView<HashData> tableView = controller.getHashTableView();
        final ObservableList<HashData> hashDataList = FXCollections.observableArrayList();
        tableView.setItems(hashDataList);

        primaryStage.show();

        songDataPollingController.setConsumer(this::onCompleteSongDataPolling);

        clipboardWatcher.addCallback(this::onUpdateClipboard);

        if (appState.isFirstBoot()) {
        }

        if (appState.isFirstBoot()) {
            final Alert alertInfo = new Alert(Alert.AlertType.INFORMATION);
            alertInfo.setTitle("案内");
            alertInfo.setHeaderText("beatorajaまたはLR2のBMSデータを参照する場合、ファイルメニューからそれぞれのルートフォルダを選択してください");
            alertInfo.showAndWait();

            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("確認");
            alert.setHeaderText("クリップボードの監視を開始しますか？");
            Optional<ButtonType> confirmResult = alert.showAndWait();
            config.setEnableWatchClipboard(confirmResult.isPresent());

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
                Platform.exit();
                return;
            }
        }

        controller.setEnableWatchClipboard(config.isEnableWatchClipboard());

        if (config.isEnableWatchClipboard()) {
            clipboardWatcher.start();
        }

        controller.info("起動完了");
    }

    /**
     * beatorajaのディレクトリを選択するダイアログを表示する
     * 正しいディレクトリが選択されるまでループする
     * 正しいディレクトリが選択された場合は設定を保存する
     * キャンセルされた場合は打ち切る
     * @param owner ダイアログのオーナーウィンドウ
     */
    public void chooseDBDir(SongDataAccessor accessor, Window owner) {
        final String name;
        final String currentPath;
        final String dbPathRelative;
        final Consumer<String> setPath;

        if (accessor == beatorajaSongDataAccessor) {
            name = "beatoraja";
            currentPath = config.getBeatorajaPath();
            dbPathRelative = "songdata.db";
            setPath = config::setBeatorajaPath;
        } else if (accessor == lr2SongDataAccessor) {
            name = "LR2";
            currentPath = config.getLr2Path();
            dbPathRelative = "LR2files/Database/song.db";
            setPath = config::setLr2Path;
        } else {
            throw new IllegalArgumentException();
        }

        while (true) {
            // ダイアログ表示
            final DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle(name + "のディレクトリを選択");
            if (!currentPath.equals("")) {
                directoryChooser.setInitialDirectory(new File(currentPath));
            }
            final File selectedDirectory = directoryChooser.showDialog(owner);

            // キャンセルしたら打ち切り
            if (selectedDirectory == null) {
                return;
            }

            // ディレクトリの確認
            final Path dbPath = selectedDirectory.toPath().resolve(dbPathRelative);
            if (!Files.exists(dbPath)) {
                final Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("エラー");
                errorAlert.setHeaderText(
                        "楽曲データベースがありません。ディレクトリを確認してください"
                );
                errorAlert.showAndWait();
                continue;
            }

            // accessorが開かれていてDBのパスが変更された場合は閉じる
            final boolean isRestartDBConnectionRequired = accessor.isOpen()
                    && !currentPath.equals(selectedDirectory.getAbsolutePath());
            setPath.accept(selectedDirectory.getAbsolutePath());
            if (isRestartDBConnectionRequired && accessor.isOpen()) {
                try {
                    accessor.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

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
                Platform.exit();
            }

            break;
        }
    }

    public void chooseBeatorajaDir(Window owner) {
        chooseDBDir(beatorajaSongDataAccessor, owner);
        if (!songDataPollingController.getAccessors().contains(beatorajaSongDataAccessor)) {
            songDataPollingController.addAccessor(0, beatorajaSongDataAccessor);
        }
    }

    public void chooseLR2Dir(Window owner) {
        chooseDBDir(lr2SongDataAccessor, owner);
        if (!songDataPollingController.getAccessors().contains(lr2SongDataAccessor)) {
            songDataPollingController.addAccessor(lr2SongDataAccessor);
        }
    }

    private void onCompleteSongDataPolling(SongDataPollingController.Result result) {
        final ObservableList<HashData> hashDataList = controller.getHashTableView().getItems();
        final SongData songData = result.data().songData();
        final List<Integer> restDataIndices = new ArrayList<>();
        boolean isFound = false;

        for (int i = 0; i < hashDataList.size(); i++) {
            final HashData hashData = hashDataList.get(i);

            // すでに更新済みの場合は重複を削除する準備
            if (isFound) {
                switch (result.data().hashType()) {
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
                    (result.data().hashType() == HashData.HashType.MD5 && hashData.getMD5Hash().equals(result.data().hash()))
                            || result.data().hashType() == HashData.HashType.SHA256 && hashData.getSHA256Hash().equals(result.data().hash())
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

        // 既存のリストを検索し、存在すれば一番上に移動
        for (HashData hashData : hashDataList) {
            if ((sha256.isPresent() && hashData.getSHA256Hash().equals(sha256.get()))
                    || (md5.isPresent() && hashData.getMD5Hash().equals(md5.get()))
            ) {
                hashDataList.remove(hashData);
                hashDataList.add(0, hashData);
            }
        }

        // 楽曲データを取得

        if (songDataPollingController.getPollers().size() == 0) {
            hashDataList.add(0, new HashData("不明のBMS"));
            return;
        }

        final HashData hashData = new HashData("取得中...");

        if (sha256.isPresent()) {
            hashData.setSHA56Hash(sha256.get());
            songDataPollingController.poll(HashData.HashType.SHA256, sha256.get());
        } else {
            hashData.setMD5Hash(md5.get());
            songDataPollingController.poll(HashData.HashType.MD5, md5.get());
        }

        hashDataList.add(0, hashData);
    }

    public Config getConfig() {
        return config;
    }

    public MainWindowController getController() {
        return controller;
    }

    public ClipboardWatcher getClipboardWatcher() {
        return clipboardWatcher;
    }

    public static Main getInstance() {
        return INSTANCE;
    }
}
