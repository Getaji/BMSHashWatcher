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
import javafx.scene.control.Dialog;
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
import java.util.function.Consumer;

public class Main extends Application {
    public static final String APP_VERSION = "0.1.1";

    private static Main INSTANCE;

    private final Config config;

    private final ClipboardWatcher clipboardWatcher;
    private final SongDataAccessor beatorajaSongDataAccessor;
    private final SongDataAccessor lr2SongDataAccessor;
    private final SongDataPollingController songDataPollingController;
    private final AppState appState;

    private MainWindowController controller;

    public static void main(String[] args) {
        Application.launch();
    }

    public Main() {
        appState = new AppState();

        if (!Files.exists(Path.of("./config.json"))) {
            appState.setFirstBoot(true);
        }

        config = tryLoadConfig();

        if (config == null) {
            Platform.exit();
            throw new IllegalStateException();
        }

        clipboardWatcher = new ClipboardWatcher(1000);

        beatorajaSongDataAccessor = new BeatorajaSongDataAccessor();
        lr2SongDataAccessor = new LR2SongDataAccessor();

        songDataPollingController = new SongDataPollingController();
        songDataPollingController
                .addAccessor(beatorajaSongDataAccessor)
                .setEnable(!config.getBeatorajaPath().equals(""));
        songDataPollingController
                .addAccessor(lr2SongDataAccessor)
                .setEnable(!config.getLr2Path().equals(""));
    }

    public static Main getInstance() {
        return INSTANCE;
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
            final Alert alertInfo = new Alert(Alert.AlertType.INFORMATION);
            alertInfo.setTitle("案内");
            alertInfo.setHeaderText("beatorajaまたはLR2のBMSデータを参照する場合、ファイルメニューからそれぞれのルートフォルダを選択してください");
            alertInfo.showAndWait();

            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("確認");
            alert.setHeaderText("クリップボードの監視を開始しますか？");
            Optional<ButtonType> confirmResult = alert.showAndWait();
            config.setEnableWatchClipboard(confirmResult.isPresent());

            trySaveConfig();
        }

        controller.setEnableWatchClipboard(config.isEnableWatchClipboard());

        if (config.isEnableWatchClipboard()) {
            clipboardWatcher.start();
        }

        controller.info("起動完了");
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean trySaveConfig() {
        while (true) {
            try {
                Config.save("./config.json", config);
                return true;
            } catch (IOException e) {
                final Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("エラー");
                alert.setHeaderText(
                        "設定ファイル(config.json)を保存できません。再試行しますか？\n（キャンセルするとアプリケーションを終了します）"
                );
                final Optional<ButtonType> result = alert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    e.printStackTrace();
                    Platform.exit();
                    return false;
                }
            }
        }
    }

    public Config tryLoadConfig() {
        while (true) {
            try {
                return Config.load("./config.json");
            } catch (IOException e) {
                final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("エラー");
                alert.setHeaderText(
                        "設定ファイル(config.json)を作成または読み込みできません。再試行しますか？\n（キャンセルするとアプリケーションを終了します）"
                );
                final Optional<ButtonType> result = alert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    e.printStackTrace();
                    Platform.exit();
                    return null;
                }
            }
        }
    }

    /**
     * beatorajaのディレクトリを選択するダイアログを表示する
     * 正しいディレクトリが選択されるまでループする
     * 正しいディレクトリが選択された場合は設定を保存する
     * キャンセルされた場合は打ち切る
     *
     * @param owner ダイアログのオーナーウィンドウ
     * @return 選択したか
     */
    public boolean chooseDBDir(SongDataAccessor accessor, Window owner) {
        final String name;
        final String currentPath;
        final Consumer<String> setPath;
        final boolean isEnable;

        if (accessor == beatorajaSongDataAccessor) {
            name = "beatoraja";
            currentPath = config.getBeatorajaPath();
            setPath = config::setBeatorajaPath;
            isEnable = config.isUseBeatorajaDB();
        } else if (accessor == lr2SongDataAccessor) {
            name = "LR2";
            currentPath = config.getLr2Path();
            setPath = config::setLr2Path;
            isEnable = config.isUseLR2DB();
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
                return false;
            }

            // ディレクトリの確認
            if (accessor.isValidPath(selectedDirectory.getPath())) {
                final Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("エラー");
                errorAlert.setHeaderText(
                        "楽曲データベースがありません。ディレクトリを確認してください"
                );
                errorAlert.showAndWait();
                continue;
            }

            final boolean isPathChanged = !currentPath.equals(selectedDirectory.getAbsolutePath());
            setPath.accept(selectedDirectory.getAbsolutePath());
            songDataPollingController.getPoller(accessor).ifPresent(poller -> {
                if (accessor.isOpen() && isPathChanged) {
                    poller.setReconnectRequired(true);
                }
                poller.setEnable(isEnable);
            });

            trySaveConfig();

            return true;
        }
    }

    public boolean chooseBeatorajaDir(Window owner) {
        final boolean isChose = chooseDBDir(beatorajaSongDataAccessor, owner);
        if (!songDataPollingController.getAccessors().contains(beatorajaSongDataAccessor)) {
            songDataPollingController.addAccessor(0, beatorajaSongDataAccessor);
        }
        return isChose;
    }

    public boolean chooseLR2Dir(Window owner) {
        final boolean isChose = chooseDBDir(lr2SongDataAccessor, owner);
        if (!songDataPollingController.getAccessors().contains(lr2SongDataAccessor)) {
            songDataPollingController.addAccessor(lr2SongDataAccessor);
        }
        return isChose;
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

    public void setEnableClipboardWatcher(boolean isEnable) {
        if (isEnable) {
            getClipboardWatcher().start();
            controller.info("クリップボードの監視を開始しました");
        } else {
            getClipboardWatcher().stop();
            controller.info("クリップボードの監視を停止しました");
        }
        config.setEnableWatchClipboard(isEnable);
        trySaveConfig();
    }

    public void applyPreference(PreferenceDialogModel model) {
        // beatoraja処理
        songDataPollingController.getPoller(beatorajaSongDataAccessor).ifPresent(poller -> {
            poller.setEnable(model.isUseBeatorajaDB() && !model.getBeatorajaPath().equals(""));
            // パスが変更
            if (!config.getBeatorajaPath().equals(model.getBeatorajaPath())) {
                poller.setEnable(model.isUseBeatorajaDB() && !model.getBeatorajaPath().equals(""));
                if (beatorajaSongDataAccessor.isOpen()) {
                    poller.setReconnectRequired(true);
                }
            }
        });
        // LR2処理
        songDataPollingController.getPoller(lr2SongDataAccessor).ifPresent(poller -> {
            poller.setEnable(model.isUseLR2DB() && !model.getLr2Path().equals(""));
            // パスが変更
            if (!config.getLr2Path().equals(model.getLr2Path())) {
                poller.setEnable(model.isUseLR2DB() && !model.getLr2Path().equals(""));
                if (lr2SongDataAccessor.isOpen()) {
                    poller.setReconnectRequired(true);
                }
            }
        });

        config.setUseBeatorajaDB(model.isUseBeatorajaDB());
        config.setUseLR2DB(model.isUseLR2DB());
        config.setBeatorajaPath(model.getBeatorajaPath());
        config.setLr2Path(model.getLr2Path());
    }

    public void openPreference() {
        final Dialog<ButtonType> dialog = new Dialog<>();
        final FXMLLoader rootLoader = new FXMLLoader(Main.class.getResource("PreferenceDialog.fxml"));
        final Parent root;
        try {
            root = rootLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        dialog.setTitle("設定");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.APPLY, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(root);

        // controller <-> model
        final PreferenceDialogModel model = new PreferenceDialogModel();
        final PreferenceDialogController dialogController = rootLoader.getController();
        dialogController.bind(model);
        dialogController.setDialog(dialog);

        // config -> model
        model.useBeatorajaDBProperty().set(config.isUseBeatorajaDB());
        model.useLR2DBProperty().set(config.isUseLR2DB());
        model.beatorajaPathProperty().set(config.getBeatorajaPath());
        model.lr2PathProperty().set(config.getLr2Path());

        dialog.setOnCloseRequest(event -> {
            // APPLYボタンならイベントを使用することで閉じないようにする
            if (dialog.getResult() == ButtonType.APPLY) {
                event.consume();
                applyPreference(model);
            }
        });

        final Optional<ButtonType> result = dialog.showAndWait();
        result.ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                applyPreference(model);
            }
        });
        dialogController.unbind(model);
    }

    public SongDataAccessor getBeatorajaSongDataAccessor() {
        return beatorajaSongDataAccessor;
    }

    public SongDataAccessor getLr2SongDataAccessor() {
        return lr2SongDataAccessor;
    }
}
