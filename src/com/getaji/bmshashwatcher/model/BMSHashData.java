package com.getaji.bmshashwatcher.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;

/**
 * BMSハッシュのデータを管理するクラス（変更可能）
 */
public class BMSHashData {
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty md5Hash = new SimpleStringProperty();
    private final StringProperty sha256Hash = new SimpleStringProperty();

    public BMSHashData(String title, String md5Hash, String sha256Hash) {
        this.title.set(title);
        this.md5Hash.set(md5Hash);
        this.sha256Hash.set(sha256Hash);
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public String getMD5Hash() {
        return md5Hash.get();
    }

    public StringProperty md5HashProperty() {
        return md5Hash;
    }

    public void setMD5Hash(String md5Hash) {
        this.md5Hash.set(md5Hash);
    }

    public String getSHA256Hash() {
        return sha256Hash.get();
    }

    public StringProperty sha256HashProperty() {
        return sha256Hash;
    }

    public void setSHA256Hash(String sha256Hash) {
        this.sha256Hash.set(sha256Hash);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BMSHashData that = (BMSHashData) o;
        return Objects.equals(title, that.title) && Objects.equals(md5Hash, that.md5Hash) && Objects.equals(sha256Hash, that.sha256Hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, md5Hash, sha256Hash);
    }

    public enum HashType {
        MD5,
        SHA256,
    }
}
