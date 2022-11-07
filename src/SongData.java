public class SongData {
    private final String md5;
    private final String sha256;
    private final String title;
    private final String subtitle;

    public SongData(String md5, String sha256, String title, String subtitle) {
        this.md5 = md5;
        this.sha256 = sha256;
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getMd5() {
        return md5;
    }

    public String getSha256() {
        return sha256;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getTitleFull() {
        return subtitle.length() > 0 ? title + " " + subtitle : title;
    }
}
