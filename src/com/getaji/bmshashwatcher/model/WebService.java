package com.getaji.bmshashwatcher.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.getaji.bmshashwatcher.Main;
import com.getaji.bmshashwatcher.lib.Either;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

public class WebService {
    private String title;
    private String md5UrlPattern;
    private String sha256UrlPattern;

    @JsonCreator
    public WebService(
            @JsonProperty("title") String title,
            @JsonProperty("md5UrlPattern") String md5UrlPattern,
            @JsonProperty("sha256UrlPattern") String sha256UrlPattern
    ) {
        this.title = title;
        this.md5UrlPattern = md5UrlPattern;
        this.sha256UrlPattern = sha256UrlPattern;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("md5UrlPattern")
    public String getMD5UrlPattern() {
        return md5UrlPattern;
    }

    @JsonProperty("md5UrlPattern")
    public void setMD5UrlPattern(String md5UrlPattern) {
        this.md5UrlPattern = md5UrlPattern;
    }

    @JsonProperty("sha256UrlPattern")
    public String getSHA256UrlPattern() {
        return sha256UrlPattern;
    }

    @JsonProperty("sha256UrlPattern")
    public void setSHA256UrlPattern(String sha256UrlPattern) {
        this.sha256UrlPattern = sha256UrlPattern;
    }

    private String createUrl(String pattern, BMSHashData.HashType hashType, BMSHashData data) {
        final String hash;
        switch (hashType) {
            case MD5 -> hash = data.getMD5Hash();
            case SHA256 -> hash = data.getSHA256Hash();
            default -> throw new IllegalArgumentException();
        }
        return pattern.replace("%h", hash).replace("%t", data.getTitle());
    }

    @JsonIgnore
    public Either<String, String> getURL(BMSHashData data) {
        switch (getSupportedHashType()) {
            case MD5 -> {
                if (data.getMD5Hash().equals("")) {
                    return Either.left("このデータにはMD5ハッシュがありません");
                }
                return Either.right(createUrl(md5UrlPattern, BMSHashData.HashType.MD5, data));
            }
            case SHA256 -> {
                if (data.getSHA256Hash().equals("")) {
                    return Either.left("このデータにはSHA256ハッシュがありません");
                }
                return Either.right(createUrl(sha256UrlPattern, BMSHashData.HashType.SHA256, data));
            }
            case MD5_AND_SHA256 -> {
                if (data.getMD5Hash().equals("") && data.getSHA256Hash().equals("")) {
                    return Either.left("このデータにはハッシュがありません");
                }
                if (!data.getMD5Hash().equals("")) {
                    return Either.right(createUrl(md5UrlPattern, BMSHashData.HashType.MD5, data));
                }
                return Either.right(createUrl(sha256UrlPattern, BMSHashData.HashType.SHA256, data));
            }
            default -> throw new IllegalStateException("illegal WebService supported hash type");
        }
    }

    @JsonIgnore
    public SupportedHashType getSupportedHashType() {
        if (!md5UrlPattern.equals("") && !sha256UrlPattern.equals("")) {
            return SupportedHashType.MD5_AND_SHA256;
        }
        if (!md5UrlPattern.equals("")) {
            return SupportedHashType.MD5;
        }
        if (!sha256UrlPattern.equals("")) {
            return SupportedHashType.SHA256;
        }
        return SupportedHashType.NONE;
    }

    public void browse(BMSHashData hashData) {
        getURL(hashData).ifRightOrElse(url -> {
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (IOException e) {
                Main.getInstance().getController().error(
                        "ブラウザを開けません。OSの既定のブラウザ構成などを確認してください"
                );
                e.printStackTrace();
            }
        }, message -> {
            Main.getInstance().getController().error(message);
        });
    }
}
