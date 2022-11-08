import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 楽曲データの取得を制御するクラス
 * 複数のAccessor
 */
public class SongDataPollingController {
    private final List<SongDataAccessor> accessors = new ArrayList<>();
    private final List<SongDataPoller> pollers = new ArrayList<>();

    private boolean isEnableFallback = true;
    private Consumer<Result> consumer;

    private SongDataPoller createPoller(SongDataAccessor accessor) {
        final SongDataPoller poller = new SongDataPoller(accessor);
        poller.setConsumer(result -> {
            // found
            if (result.songData() != null) {
                consumer.accept(new Result(accessor, result));
                return;
            }

            // 次のpollerに渡す
            final int pollerIndex = pollers.indexOf(poller);
            for (int i = pollerIndex + 1; i < pollers.size(); i++) {
                final SongDataPoller nextPoller = pollers.get(i);

                // ハッシュタイプをサポートしていなければ次へ
                if (nextPoller.getSongDataAccessor().isSupportHashType(result.hashType())) {
                    nextPoller.poll(result.hashType(), result.hash());
                    return;
                }
            }
            // 最後のpollerに到達したので終了
            consumer.accept(new Result(accessor, result));
        });
        return poller;
    }

    public void addAccessor(SongDataAccessor accessor) {
        accessors.add(accessor);
        pollers.add(createPoller(accessor));
    }

    public void addAccessor(int index, SongDataAccessor accessor) {
        accessors.add(index, accessor);
        pollers.add(index, createPoller(accessor));
    }

    public List<SongDataAccessor> getAccessors() {
        return accessors;
    }

    public List<SongDataPoller> getPollers() {
        return pollers;
    }

    public boolean isEnableFallback() {
        return isEnableFallback;
    }

    public void setEnableFallback(boolean enableFallback) {
        isEnableFallback = enableFallback;
    }

    public void setConsumer(Consumer<Result> consumer) {
        this.consumer = consumer;
    }

    public void poll(HashData.HashType type, String hash) {
        if (pollers.size() == 0) {
            throw new IllegalStateException("pollerが登録されていません");
        }
        pollers.get(0).poll(type, hash);
    }

    record Result(SongDataAccessor accessor, SongDataAccessor.Result data) {
    }
}
