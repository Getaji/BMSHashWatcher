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

public class ClipboardWatcher {
    private final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    private final TimerTask timerTask;
    private final Timer timer;
    private String value = "";
    private final long delay;
    private boolean isRunning = false;
    private final List<Consumer<String>> callbacks;

    public ClipboardWatcher(long delay) {
        this.delay = delay;
        this.callbacks = new ArrayList<>();
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
    }

    public void addCallback(Consumer<String> callback) {
        callbacks.add(callback);
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        timer.schedule(timerTask, 0, delay);
    }

    public void stop() {
        if (!isRunning) return;
        isRunning = false;
        timer.cancel();
    }

    /**
     * クリップボードのデータを取得し、変更があったら更新する
     * @throws IllegalStateException クリップボードが利用できない
     */
    private void updateClipboard() throws IllegalStateException {
        final String data;
        try {
            data = clipboard.getData(DataFlavor.stringFlavor).toString();
        } catch (UnsupportedFlavorException | IOException e) {
            // 画像データなど文字列に変換できないタイプのデータは無視
            return;
        }
        if (!data.equals(value)) {
            value = data;
            this.callbacks.forEach((consumer) -> consumer.accept(data));
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}
