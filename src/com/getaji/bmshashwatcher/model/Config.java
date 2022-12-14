package com.getaji.bmshashwatcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * アプリケーションの設定を管理するクラス
 * Jacksonで変換可能
 */
public class Config {
    public static final List<WebService> DEFAULT_WEB_SERVICE_LIST = Arrays.asList(
            new WebService("Mocha-Repository", "", "https://mocha-repository.info/song" +
                    ".php?sha256=%h"),
            new WebService("MinIR", "", "https://www.gaftalk.com/minir/#/viewer/song/%h/0"),
            new WebService("Cinnamon", "", "https://cinnamon.link/charts/%h"),
            new WebService("LR2IR", "http://www.dream-pro.info/~lavalse/LR2IR/search" +
                    ".cgi?mode=ranking&bmsmd5=%h", "")
    );
    public static final int LATEST_CONFIG_VERSION = 1;

    private int configVersion = 0;

    private List<WebService> webServiceList = new ArrayList<>();

    private String beatorajaPath = "";

    private String lr2Path = "";

    private boolean useBeatorajaDB = true;

    private boolean useLR2DB = true;

    private boolean enableWatchClipboard = false;

    private int clipboardDelay = 1000;

    @JsonProperty("configVersion")
    public int getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
    }

    @JsonProperty("webServiceList")
    public List<WebService> getWebServiceList() {
        return webServiceList;
    }

    public void setWebServiceList(List<WebService> list) {
        webServiceList = list;
    }

    @JsonProperty("beatorajaPath")
    public String getBeatorajaPath() {
        return beatorajaPath;
    }

    public void setBeatorajaPath(String beatorajaPath) {
        this.beatorajaPath = beatorajaPath;
    }

    @JsonProperty("lr2Path")
    public String getLr2Path() {
        return lr2Path;
    }

    public void setLr2Path(String lr2Path) {
        this.lr2Path = lr2Path;
    }

    public boolean isUseBeatorajaDB() {
        return useBeatorajaDB;
    }

    public void setUseBeatorajaDB(boolean useBeatorajaDB) {
        this.useBeatorajaDB = useBeatorajaDB;
    }

    public boolean isUseLR2DB() {
        return useLR2DB;
    }

    public void setUseLR2DB(boolean useLR2DB) {
        this.useLR2DB = useLR2DB;
    }

    @JsonProperty("enableWatchClipboard")
    public boolean isEnableWatchClipboard() {
        return enableWatchClipboard;
    }

    public void setEnableWatchClipboard(boolean enableWatchClipboard) {
        this.enableWatchClipboard = enableWatchClipboard;
    }

    @JsonProperty("clipboardDelay")
    public int getClipboardDelay() {
        return clipboardDelay;
    }

    public void setClipboardDelay(int clipboardDelay) {
        this.clipboardDelay = clipboardDelay;
    }

    private static boolean migrate(Config config) {
        boolean isMigrated = false;
        if (config.configVersion < 1) {
            config.getWebServiceList().forEach(webService -> {
                webService.setMD5UrlPattern(webService.getMD5UrlPattern().replace("%s", "%h"));
                webService.setSHA256UrlPattern(webService.getSHA256UrlPattern().replace("%s", "%h"));
            });
            isMigrated = true;
        }
        if (isMigrated) {
            config.configVersion = LATEST_CONFIG_VERSION;
        }
        return isMigrated;
    }

    /**
     * 設定をファイルから読み込んでConfigインスタンスを構築する
     *
     * @param pathname 設定ファイルの絶対パス
     * @return 構築したConfig
     * @throws IOException ファイルの読み込みまたは書き込みに失敗
     */
    public static Config load(String pathname) throws IOException {
        final File file = new File(pathname);

        if (file.exists()) {
            // TODO validate
            final String text = Files.readString(file.toPath());
            final ObjectMapper objectMapper = new ObjectMapper();
            final Config config = objectMapper.readValue(text, new TypeReference<>() {
            });

            final boolean isMigrated = migrate(config);

            if (isMigrated) {
                try {
                    save(pathname, config);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return config;
        } else {
            final Config config = new Config();
            config.getWebServiceList().addAll(DEFAULT_WEB_SERVICE_LIST);
            config.setConfigVersion(LATEST_CONFIG_VERSION);
            final ObjectMapper objectMapper = new ObjectMapper();
            final String text = objectMapper.writeValueAsString(config);
            Files.writeString(file.toPath(), text);
            return config;
        }
    }

    /**
     * 設定をファイルに保存する
     *
     * @param pathname ファイルのパス
     * @param config   設定
     * @throws IOException 書き込みに失敗
     */
    public static void save(String pathname, Config config) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final String text = objectMapper.writeValueAsString(config);
        Files.writeString(Path.of(pathname), text);
    }
}
