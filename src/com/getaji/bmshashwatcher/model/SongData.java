package com.getaji.bmshashwatcher.model;

public record SongData(String md5, String sha256, String title, String subtitle) {

    public String getTitleFull() {
        return subtitle.length() > 0 ? title + " " + subtitle : title;
    }
}
