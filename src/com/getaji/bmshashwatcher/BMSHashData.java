package com.getaji.bmshashwatcher;

/**
 * BMSハッシュのデータを管理するクラス（変更可能）
 */
public class BMSHashData {
    private String title;
    private String md5Hash;
    private String sha256Hash;

    public BMSHashData() {
        this("", "", "");
    }

    public BMSHashData(String title) {
        this(title, "", "");
    }

    public BMSHashData(String title, String md5Hash) {
        this(title, md5Hash, "");
    }

    public BMSHashData(String title, String md5Hash, String sha256Hash) {
        this.title = title;
        this.md5Hash = md5Hash;
        this.sha256Hash = sha256Hash;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMD5Hash() {
        return md5Hash;
    }

    public void setMD5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public String getSHA256Hash() {
        return sha256Hash;
    }

    public void setSHA56Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public enum HashType {
        MD5,
        SHA256,
    }
}
