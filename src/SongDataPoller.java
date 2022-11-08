import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 楽曲データをCachedThreadPoolで取得するクラス
 */
public class SongDataPoller {
    private final SongDataAccessor accessor = new SongDataAccessor();
    private final ExecutorService executorService;
    private final Config config;
    private Consumer<SongDataAccessor.Result> consumer;

    public SongDataPoller(Config config) {
        this(config, null);
    }

    public SongDataPoller(Config config, Consumer<SongDataAccessor.Result> consumer) {
        this.config = config;
        this.consumer = consumer;
        executorService = Executors.newCachedThreadPool(r -> {
            final Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
    }

    public void setConsumer(Consumer<SongDataAccessor.Result> consumer) {
        this.consumer = consumer;
    }

    public void poll(HashData.HashType type, String hash) throws SQLException, ClassNotFoundException {
        executorService.submit(() -> {
            try {
                accessor.open(config);
                SongDataAccessor.Result songData;
                // TODO: アローはJava SE 12以降らしい
                switch (type) {
                    case MD5 -> songData = accessor.findBMSByMD5(hash);
                    case SHA256 -> songData = accessor.findBMSBySHA256(hash);
                    default ->
                            throw new IllegalArgumentException("不明なハッシュタイプ: " + type);
                }
                if (consumer != null) {
                    consumer.accept(songData);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
