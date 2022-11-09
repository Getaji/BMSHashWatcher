package com.getaji.bmshashwatcher;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * クリップボードを監視するクラス
 * 値を保持し、新たな値が文字列かつ異なる値なら状態を更新して通知する
 */
public class ClipboardWatcher {
    private TimerTask timerTask;
    private Timer timer;
    private String value = "";
    private final long delay;
    private boolean isRunning = false;
    private Consumer<String> callback;

    /**
     * 監視間隔を指定してインスタンスを作成する
     * @param delay 監視間隔（ミリ秒）
     */
    public ClipboardWatcher(long delay) {
        this.delay = delay;
    }

    /**
     * クリップボードのデータが更新された時に呼び出される関数を設定する
     * データを文字列に変換できない、またはデータに変更がない場合は呼び出されない
     * @param callback 関数
     */
    public void setCallback(Consumer<String> callback) {
        this.callback = callback;
    }

    /**
     * クリップボードの監視を開始する
     * 既に開始している場合は何も起こらない
     */
    public void start() {
        if (isRunning) return;
        timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    updateClipboard();
                } catch (IllegalStateException e) {
                    Main.getInstance().getController().error("クリップボードを参照できません");
                    e.printStackTrace();
                }
            }
        };
        timer = new Timer(true);
        timer.schedule(timerTask, 0, delay);
        isRunning = true;
    }

    /**
     * クリップボードの監視を停止する
     */
    public void stop() {
        if (!isRunning) return;
        timer.cancel();
        timer = null;
        timerTask = null;
        isRunning = false;
    }

    /**
     * クリップボードのデータを取得し、変更があったら更新する
     *
     * @throws IllegalStateException クリップボードが利用できない
     */
    private void updateClipboard() throws IllegalStateException {
        final String data;
        try {
            data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor).toString();
        } catch (UnsupportedFlavorException | IOException e) {
            // 画像データなど文字列に変換できないタイプのデータは無視
            return;
        }
        if (!data.equals(value)) {
            value = data;
            if (callback != null) callback.accept(data);
        }
    }

    /**
     * クリップボードを監視中か
     * @return
     */
    public boolean isRunning() {
        return isRunning;
    }
}
