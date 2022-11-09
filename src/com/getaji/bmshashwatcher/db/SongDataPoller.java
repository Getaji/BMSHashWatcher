package com.getaji.bmshashwatcher.db;

import com.getaji.bmshashwatcher.model.BMSHashData;
import com.getaji.bmshashwatcher.Main;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 楽曲データをCachedThreadPoolで取得するクラス
 */
public class SongDataPoller {
    private final SongDataAccessor accessor;
    private final ExecutorService executorService;
    private Consumer<SongDataAccessor.Result> consumer;
    private boolean isReconnectRequired = false;
    private boolean isEnable = true;

    public SongDataPoller(SongDataAccessor accessor) {
        this(accessor, null);
    }

    public SongDataPoller(SongDataAccessor accessor, Consumer<SongDataAccessor.Result> consumer) {
        this.accessor = accessor;
        this.consumer = consumer;
        executorService = Executors.newCachedThreadPool(r -> {
            final Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
    }

    public void setConsumer(Consumer<BeatorajaSongDataAccessor.Result> consumer) {
        this.consumer = consumer;
    }

    public void poll(BMSHashData.HashType type, String hash) {
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
                SongDataAccessor.Result songData;
                switch (type) {
                    case MD5 -> songData = accessor.findBMSByMD5(hash);
                    case SHA256 -> songData = accessor.findBMSBySHA256(hash);
                    default -> throw new IllegalArgumentException("不明なハッシュタイプ: " + type);
                }
                if (consumer != null) {
                    consumer.accept(songData);
                }
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
