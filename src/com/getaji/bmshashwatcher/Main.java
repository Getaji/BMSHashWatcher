package com.getaji.bmshashwatcher;

import com.getaji.bmshashwatcher.controller.MainWindowController;
import com.getaji.bmshashwatcher.controller.PreferenceDialogController;
import com.getaji.bmshashwatcher.db.BeatorajaSongDataAccessor;
import com.getaji.bmshashwatcher.db.LR2SongDataAccessor;
import com.getaji.bmshashwatcher.db.SongDataAccessor;
import com.getaji.bmshashwatcher.db.SongDataPollingController;
import com.getaji.bmshashwatcher.lib.HashChecker;
import com.getaji.bmshashwatcher.model.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class Main extends Application {
    public static final String APP_VERSION = "0.3.0";

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

        if (config.getClipboardDelay() < 100) {
            config.setClipboardDelay(1000);
            trySaveConfig();
        }

        clipboardWatcher = new ClipboardWatcher(config.getClipboardDelay());
        clipboardWatcher.setCallback(this::onUpdateClipboard);

        beatorajaSongDataAccessor = new BeatorajaSongDataAccessor();
        lr2SongDataAccessor = new LR2SongDataAccessor();

        songDataPollingController = new SongDataPollingController();
        songDataPollingController
                .addAccessor(beatorajaSongDataAccessor)
                .setEnable(!config.getBeatorajaPath().isEmpty() && config.isUseBeatorajaDB());
        songDataPollingController
                .addAccessor(lr2SongDataAccessor)
                .setEnable(!config.getLr2Path().isEmpty() && config.isUseLR2DB());

        songDataPollingController.setMultipleConsumer(multipleResult -> {
            synchronized (songDataPollingController) {
                onCompleteSongDataPolling(multipleResult);
            }
        });
    }

    public static Main getInstance() {
        return INSTANCE;
    }

    @Override
    public void start(Stage primaryStage) {
        INSTANCE = this;

        primaryStage.setTitle("BMS Hash Watcher v" + APP_VERSION);

        final FXMLLoader rootLoader = new FXMLLoader(getClass().getResource("fxml/MainWindow" +
                ".fxml"));
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

        final MainWindowModel model = new MainWindowModel();
        controller.bind(model);

        primaryStage.show();

        if (appState.isFirstBoot()) {
            final Alert alertInfo = new Alert(Alert.AlertType.INFORMATION);
            alertInfo.setTitle("案内");
            alertInfo.setHeaderText("beatorajaまたはLR2のBMS" +
                    "データを参照する場合、ファイルメニューからそれぞれのルートフォルダを選択してください");
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

    /**
     * 設定の保存を試みる。
     * 失敗した場合は確認ダイアログを表示し、OKが選択された場合は再試行する。
     *
     * @return 保存に成功したか
     */
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

    /**
     * 設定の読み込みを試みる。
     * 失敗した場合は確認ダイアログを表示し、OKが選択された場合は再試行する。
     *
     * @return 読み込みに成功したか
     */
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
            if (!accessor.isValidPath(selectedDirectory.getPath())) {
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

    /**
     * beatorajaフォルダを選択するダイアログを表示する。
     *
     * @param owner ダイアログのオーナーウィンドウ
     * @return 選択されたか
     */
    public boolean chooseBeatorajaDir(Window owner) {
        return chooseDBDir(beatorajaSongDataAccessor, owner);
    }

    /**
     * LR2フォルダを選択するダイアログを表示する。
     *
     * @param owner ダイアログのオーナーウィンドウ
     * @return 選択されたか
     */
    public boolean chooseLR2Dir(Window owner) {
        return chooseDBDir(lr2SongDataAccessor, owner);
    }

    /**
     * どちらかの空文字ではないハッシュがBMSHashDataのハッシュと一致すればtrueを返す
     */
    private boolean isResponseMatchesEitherHash(BMSHashData hashData,
                                                SongDataAccessor.Result result) {
        final SongData songData = result.songData();
        if (songData == null) {
            return (result.hashType() == BMSHashData.HashType.MD5 && result.hash().equals(hashData.getMD5Hash()))
                    || (result.hashType() == BMSHashData.HashType.SHA256 && result.hash().equals(hashData.getSHA256Hash()));
        }
        return (!songData.md5().isEmpty() && songData.md5().equals(hashData.getMD5Hash()))
                || (!songData.sha256().isEmpty() && songData.sha256().equals(hashData.getSHA256Hash()));
    }

    /**
     * 楽曲データの取得が完了した時に呼び出されるメソッド
     * SongDataPollingControllerに登録される
     */
    private void onCompleteSongDataPolling(SongDataPollingController.MultipleResult multipleResult) {
        final List<BMSHashData> hashDataList =
                new ArrayList<>(controller.getHashTableView().getItems());

        if (multipleResult.data().isEmpty()) return;

        // 走査後に先頭に追加するやつ
        final List<BMSHashData> updatedHashDataList = new ArrayList<>();

        // 既存のハッシュリストを検索
        // 追加済みフラグがONでハッシュのどちらかが一致すれば削除
        // リストになければ先頭に追加して追加済みフラグをONにして続行
        // リストにあれば取り出してデータを更新し、先頭に移動して追加済みフラグをONにして続行
        // ハッシュの一致とは：
        // 結果の楽曲データがnullならリクエスト時のデータに一致
        //
        for (int ri = 0; ri < multipleResult.data().size(); ri++) {
            final SongDataAccessor.Result result = multipleResult.data().get(ri);
            final SongData resultSong = result.songData();
            boolean isFound = false;

            // 既存のハッシュリストを逆順に探索（削除のため）
            for (int hi = hashDataList.size() - 1; hi >= 0; hi--) {
                final BMSHashData hashData = hashDataList.get(hi);

                // 検出2件目以降は削除のみ
                if (isFound) {
                    if (isResponseMatchesEitherHash(hashData, result)) {
                        hashDataList.remove(hi);
                    }
                    continue;
                }

                // 既存の要素に一致
                if (isResponseMatchesEitherHash(hashData, result)) {
                    if (resultSong == null) {
                        if (hashData.getTitle().equals("取得中...")) {
                            hashData.setTitle("不明のBMS");
                            hashDataList.remove(hi);
                            updatedHashDataList.add(hashData);
                        }
                    } else {
                        hashData.setTitle(resultSong.getTitleFull());
                        if (!resultSong.md5().isEmpty()) {
                            hashData.setMD5Hash(resultSong.md5());
                        }
                        if (!resultSong.sha256().isEmpty()) {
                            hashData.setSHA256Hash(resultSong.sha256());
                        }
                        hashDataList.remove(hi);
                        updatedHashDataList.add(hashData);
                    }

                    isFound = true;
                }
            }

            if (!isFound) {
                if (resultSong == null) {
                    updatedHashDataList.add(new BMSHashData(
                            "不明のBMS",
                            result.hashType() == BMSHashData.HashType.MD5 ? result.hash() : "",
                            result.hashType() == BMSHashData.HashType.SHA256 ? result.hash() : ""
                    ));
                } else {
                    updatedHashDataList.add(new BMSHashData(
                            resultSong.getTitleFull(),
                            resultSong.md5(),
                            resultSong.sha256()
                    ));
                }
            }
        }
        hashDataList.addAll(0, updatedHashDataList);
        controller.getHashTableView().getItems().setAll(hashDataList);
        controller.info("データを取得しました");
    }

    /**
     * クリップボードのデータが更新されたら呼び出されるメソッド
     * ClipboardWatcherに登録される
     */
    private synchronized void onUpdateClipboard(String value) {
        // このアプリがコピーしたデータなら無視
        if (appState.isCopyWithThisAppJustBefore()) {
            appState.setCopyWithThisAppJustBefore(false);
            return;
        }

        final List<String> hashList = HashChecker.getHashPartAll(value);
        final List<String> distinctHashList = new HashSet<>(hashList).stream().toList();

        if (distinctHashList.isEmpty()) return;

        final ObservableList<BMSHashData> hashDataList = controller.getHashTableView().getItems();

        // TODO 既に存在する場合の処理

        final List<SongDataAccessor.Request> requests = distinctHashList.stream()
                .map(hash -> {
                    final boolean isMD5Hash = hash.length() == 32;
                    final BMSHashData hashData = new BMSHashData(
                            "取得中...",
                            isMD5Hash ? hash : "",
                            isMD5Hash ? "" : hash
                    );
                    if (songDataPollingController.isDisableAll()) {
                        hashData.setTitle("不明のBMS");
                    }
                    controller.getHashTableView().getItems().add(hashData);
                    return new SongDataAccessor.Request(isMD5Hash ? BMSHashData.HashType.MD5 :
                            BMSHashData.HashType.SHA256, hash);
                })
                .toList();

        if (!songDataPollingController.isDisableAll()) {
            try {
                controller.info("ハッシュを検出しました。データを取得しています...");
                songDataPollingController.pollAll(requests);
            } catch (ExecutionException e) {
                // 例外を伴って完了
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                // 割り込まれた
                throw new RuntimeException(e);
            }
        } else {
            controller.info("ハッシュを検出しました");
        }
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

    /**
     * クリップボードの監視状態を切り替え、メッセージを表示し、設定を保存する
     *
     * @param isEnable 監視するか
     */
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

    /**
     * 設定をアプリケーションに適用する
     *
     * @param model 設定ダイアログのモデルデータ
     */
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

        config.setClipboardDelay(model.getClipboardDelay());
        clipboardWatcher.setDelay(model.getClipboardDelay());

        config.setWebServiceList(model.getWebServices());

        controller.constructContextMenu(config);

        trySaveConfig();
    }

    /**
     * 設定画面を開く
     */
    public void openPreference() {
        final Dialog<ButtonType> dialog = new Dialog<>();
        final FXMLLoader rootLoader = new FXMLLoader(Main.class.getResource("fxml" +
                "/PreferenceDialog.fxml"));
        final Parent root;
        try {
            root = rootLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        dialog.setTitle("設定");
        dialog.setResizable(true);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.APPLY,
                ButtonType.CANCEL);
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
        model.clipboardDelayProperty().set(config.getClipboardDelay());
        model.getWebServices().addAll(config.getWebServiceList());

        dialog.setOnCloseRequest(event -> {
            // APPLYボタンならイベントを使用することで閉じないようにする
            if (dialog.getResult() == ButtonType.APPLY) {
                event.consume();
                dialogController.onPreApply(model);
                applyPreference(model);
            }
        });

        final Optional<ButtonType> result = dialog.showAndWait();
        result.ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                dialogController.onPreApply(model);
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
