package com.getaji.bmshashwatcher.db;

import com.getaji.bmshashwatcher.Main;
import com.getaji.bmshashwatcher.model.BMSHashData;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 楽曲データをCachedThreadPoolで取得するクラス
 */
public class SongDataPoller {
    private final SongDataAccessor accessor;
    private final ExecutorService executorService;
    private Consumer<SongDataAccessor.Result> singleConsumer;
    private Consumer<List<SongDataAccessor.Result>> multipleConsumer;
    private boolean isReconnectRequired = false;
    private boolean isEnable = true;

    public SongDataPoller(SongDataAccessor accessor) {
        this.accessor = accessor;
        executorService = Executors.newCachedThreadPool(r -> {
            final Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
    }

    public void setSingleConsumer(Consumer<BeatorajaSongDataAccessor.Result> singleConsumer) {
        this.singleConsumer = singleConsumer;
    }

    public Optional<List<SongDataAccessor.Result>> pollAll(List<SongDataAccessor.Request> requests) {
        try {
            if (isReconnectRequired) {
                if (accessor.isOpen()) {
                    accessor.close();
                }
                isReconnectRequired = false;
            }
            if (!accessor.isOpen()) {
                accessor.open(Main.getInstance().getConfig());
            }
            return Optional.of(accessor.findAll(requests));
        } catch (SQLException e) {
            e.printStackTrace();
            Main.getInstance().getController().error(
                    "データベースにアクセスできません。ファイルの有無やアクセス権限を確認してください"
            );
            return Optional.empty();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("内部エラー");
            alert.setHeaderText(
                    "データベースにアクセスするための機能が破損しています"
            );
            alert.showAndWait();
            Platform.exit();
            return Optional.empty();
        }
    }

    public void pollAllAsync(List<SongDataAccessor.Request> requests,
                             Consumer<Optional<List<SongDataAccessor.Result>>> consumer) {
        CompletableFuture
                .supplyAsync(() -> pollAll(requests), executorService)
                .thenAccept(consumer);
    }

    public void poll(BMSHashData.HashType type, String hash,
                     Consumer<SongDataAccessor.Result> callback) {
        if (!accessor.isSupportHashType(type)) {
            throw new IllegalArgumentException("このpollerは" + type + "をサポートしていません");
        }
        executorService.submit(() -> {
            try {
                if (isReconnectRequired) {
                    if (accessor.isOpen()) {
                        accessor.close();
                    }
                    isReconnectRequired = false;
                }
                if (!accessor.isOpen()) {
                    accessor.open(Main.getInstance().getConfig());
                }
                final SongDataAccessor.Result songData;
                switch (type) {
                    case MD5 -> songData = accessor.findBMSByMD5(hash);
                    case SHA256 -> songData = accessor.findBMSBySHA256(hash);
                    default -> throw new IllegalArgumentException("不明なハッシュタイプ: " + type);
                }
                callback.accept(songData);
            } catch (SQLException e) {
                e.printStackTrace();
                Main.getInstance().getController().error(
                        "データベースにアクセスできません。ファイルの有無やアクセス権限を確認してください"
                );
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                final Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("内部エラー");
                alert.setHeaderText(
                        "データベースにアクセスするための機能が破損しています"
                );
                alert.showAndWait();
                Platform.exit();
            }
        });
    }

    public void poll(BMSHashData.HashType type, String hash) {
        poll(type, hash, singleConsumer);
    }

    public SongDataAccessor getSongDataAccessor() {
        return accessor;
    }

    public boolean isReconnectRequired() {
        return isReconnectRequired;
    }

    public void setReconnectRequired(boolean reconnectRequired) {
        isReconnectRequired = reconnectRequired;
    }

    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
    }
}
