package com.getaji.bmshashwatcher.db;

import com.getaji.bmshashwatcher.model.BMSHashData;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 楽曲データの取得を制御するクラス
 * 複数のAccessor
 */
public class SongDataPollingController {
    private final List<SongDataAccessor> accessors = new ArrayList<>();
    private final List<SongDataPoller> pollers = new ArrayList<>();

    private boolean isEnableFallback = true;
    private Consumer<Result> singleConsumer;
    private Consumer<MultipleResult> multipleConsumer;

    private SongDataPoller createPoller(SongDataAccessor accessor) {
        final SongDataPoller poller = new SongDataPoller(accessor);
        poller.setSingleConsumer(result -> {
            // found or fallback disabled
            if (result.songData() != null || !isEnableFallback) {
                singleConsumer.accept(new Result(accessor, result));
                return;
            }

            // 次のpollerに渡す
            final int pollerIndex = pollers.indexOf(poller);
            for (int i = pollerIndex + 1; i < pollers.size(); i++) {
                final SongDataPoller nextPoller = pollers.get(i);

                // 有効状態かつハッシュタイプをサポートしていれば実行
                if (nextPoller.isEnable() && nextPoller.getSongDataAccessor().isSupportHashType(result.hashType())) {
                    nextPoller.poll(result.hashType(), result.hash());
                    return;
                }
            }
            // 最後のpollerに到達したので終了
            singleConsumer.accept(new Result(accessor, result));
        });
        return poller;
    }

    public SongDataPoller addAccessor(SongDataAccessor accessor) {
        accessors.add(accessor);
        final SongDataPoller poller = createPoller(accessor);
        pollers.add(poller);
        return poller;
    }

    public SongDataPoller addAccessor(int index, SongDataAccessor accessor) {
        accessors.add(index, accessor);
        final SongDataPoller poller = createPoller(accessor);
        pollers.add(index, poller);
        return poller;
    }

    public Optional<SongDataPoller> getPoller(SongDataAccessor accessor) {
        for (final SongDataPoller poller : pollers) {
            if (poller.getSongDataAccessor() == accessor) {
                return Optional.of(poller);
            }
        }
        return Optional.empty();
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

    public void setSingleConsumer(Consumer<Result> singleConsumer) {
        this.singleConsumer = singleConsumer;
    }

    /**
     * 単一のpollerを受け取り、複数のリクエストを処理する。
     * 楽曲データがnullではない結果のリストは内部のmultipleConsumerに渡す。
     * 楽曲データがnullの結果のリスト（0件の場合は空のリスト）はCompletableFutureにセットする。
     *
     * @param poller   poller
     * @param requests リクエストのリスト
     * @return 結果処理後に完了されるCompletableFuture
     */
    private CompletableFuture<List<SongDataAccessor.Result>> pollAll(SongDataPoller poller,
                                                                     List<SongDataAccessor.Request> requests) {
        final CompletableFuture<List<SongDataAccessor.Result>> completableFuture =
                new CompletableFuture<>();
        poller.pollAllAsync(requests, results -> {
            if (results.isEmpty() || results.get().isEmpty()) {
                multipleConsumer.accept(
                        new MultipleResult(poller.getSongDataAccessor(),
                                results.orElse(Collections.emptyList()))
                );
                return;
            }
            final Map<Boolean, List<SongDataAccessor.Result>> resultsByFound =
                    results.get().stream().collect(
                    Collectors.groupingBy(result -> result.songData() != null)
            );
            final Optional<List<SongDataAccessor.Result>> foundResults =
                    Optional.of(resultsByFound.getOrDefault(true, Collections.emptyList()));
            final Optional<List<SongDataAccessor.Result>> notFoundResults =
                    Optional.of(resultsByFound.getOrDefault(false, Collections.emptyList()));
            if (!foundResults.get().isEmpty()) {
                multipleConsumer.accept(new MultipleResult(poller.getSongDataAccessor(),
                        foundResults.get()));
            }
            completableFuture.complete(notFoundResults.get());
        });
        return completableFuture;
    }

    /**
     * 内部で有効になっているpollerを使用して複数のリクエストを処理する。
     * 各pollerで楽曲データがnullの結果を集約して次のpollerに渡す。
     *
     * @param requests リクエストのリスト
     * @throws ExecutionException   例外を伴って完了した場合
     * @throws InterruptedException 現在のスレッドが待ち時間に割り込まれた場合
     */
    public void pollAll(List<SongDataAccessor.Request> requests) throws ExecutionException,
            InterruptedException {
        if (pollers.isEmpty()) {
            throw new IllegalStateException("pollerが登録されていません");
        }
        List<SongDataAccessor.Request> nextRequests = requests;
        SongDataAccessor lastAccessor = null;
        for (final SongDataPoller poller : pollers) {
            if (poller.isEnable()) {
                lastAccessor = poller.getSongDataAccessor();
                final CompletableFuture<List<SongDataAccessor.Result>> future = pollAll(poller,
                        nextRequests);
                final List<SongDataAccessor.Result> results = future.get();
                if (results.isEmpty()) break;
                nextRequests =
                        results.stream().map(result -> new SongDataAccessor.Request(result.hashType(), result.hash())).toList();
            }
        }
        if (!nextRequests.isEmpty()) {
            final List<SongDataAccessor.Result> results = nextRequests.stream()
                    .map(request -> new SongDataAccessor.Result(request.hashType(),
                            request.hash(), null))
                    .toList();
            multipleConsumer.accept(new MultipleResult(lastAccessor, results));
        }
    }

    public void poll(BMSHashData.HashType type, String hash) {
        if (pollers.size() == 0) {
            throw new IllegalStateException("pollerが登録されていません");
        }
        for (final SongDataPoller poller : pollers) {
            if (poller.isEnable()) {
                poller.poll(type, hash);
                break;
            }
        }
    }

    public boolean isDisableAll() {
        return pollers.stream().noneMatch(SongDataPoller::isEnable);
    }

    public Consumer<MultipleResult> getMultipleConsumer() {
        return multipleConsumer;
    }

    public void setMultipleConsumer(Consumer<MultipleResult> multipleConsumer) {
        this.multipleConsumer = multipleConsumer;
    }

    public record Result(SongDataAccessor accessor, SongDataAccessor.Result data) {
    }

    public record MultipleResult(SongDataAccessor accessor, List<SongDataAccessor.Result> data) {
    }
}
