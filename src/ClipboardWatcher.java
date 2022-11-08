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

    public ClipboardWatcher() {
        this(1000);
    }

    public ClipboardWatcher(long delay) {
        this.delay = delay;
        this.callbacks = new ArrayList<>();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    getClipboard();
                } catch (IllegalStateException e) {
                    System.err.println("[ERROR]クリップボードが利用できない");
                } catch (IOException e) {
                    System.err.println("[ERROR]このデータは文字列にできない");
                } catch (Exception e) {
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
        if (isRunning) {
            throw new IllegalStateException("ClipboardWatcherは既に実行中です");
        }
        isRunning = true;
        timer.schedule(timerTask, 0, delay);
    }

    public void stop() {
        if (!isRunning) {
            throw new IllegalStateException("ClipboardWatcherは既に停止中です");
        }
        isRunning = false;
        timer.cancel();
    }

    private void getClipboard() throws IOException, IllegalStateException {
        final String data;
        try {
            data = clipboard.getData(DataFlavor.stringFlavor).toString();
        } catch (UnsupportedFlavorException e) {
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
