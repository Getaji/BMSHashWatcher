package com.getaji.bmshashwatcher;

import org.sqlite.SQLiteConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

/**
 * beatorajaの楽曲データを取得するクラス
 */
public class BeatorajaSongDataAccessor implements SongDataAccessor {
    private Connection connection;

    @Override
    public boolean isOpen() {
        return connection != null;
    }

    @Override
    public void open(Config config) throws SQLException, ClassNotFoundException, IllegalStateException {
        if (config.getBeatorajaPath().equals("")) {
            throw new IllegalStateException("beatorajaのパスが設定されていません");
        }
        if (connection != null) return;
        Class.forName("org.sqlite.JDBC");
        final SQLiteConfig sqliteConfig = new SQLiteConfig();
        sqliteConfig.setReadOnly(true);
        final Path dbPath = Paths.get(config.getBeatorajaPath()).resolve("songdata.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    @Override
    public void close() throws SQLException {
        connection.close();
        connection = null;
    }

    @Override
    public Result findBMSByMD5(String hash) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT md5, sha256, title, subtitle FROM song
                WHERE md5 = ? AND path <> ''
                LIMIT 1
                """);
        statement.setString(1, hash);
        ResultSet resultSet = statement.executeQuery();
        SongData songData = null;
        if (resultSet.next()) {
            songData = new SongData(
                    resultSet.getString("md5"),
                    resultSet.getString("sha256"),
                    resultSet.getString("title"),
                    resultSet.getString("subtitle")
            );
        }
        statement.close();
        resultSet.close();
        return new Result(BMSHashData.HashType.MD5, hash, songData);
    }

    @Override
    public Result findBMSBySHA256(String hash) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT md5, sha256, title, subtitle FROM song
                WHERE sha256 = ? AND path <> ''
                LIMIT 1
                """);
        statement.setString(1, hash);
        ResultSet resultSet = statement.executeQuery();
        SongData songData = null;
        if (resultSet.next()) {
            songData = new SongData(
                    resultSet.getString("md5"),
                    resultSet.getString("sha256"),
                    resultSet.getString("title"),
                    resultSet.getString("subtitle")
            );
        }
        statement.close();
        resultSet.close();
        return new Result(BMSHashData.HashType.SHA256, hash, songData);
    }

    @Override
    public SupportedHashType getSupportedHashType() {
        return SupportedHashType.MD5_AND_SHA256;
    }

    @Override
    public boolean isValidPath(String baseDir) {
        if (baseDir.equals("")) return false;

        return Files.exists(Path.of(baseDir, "songdata.db"));
    }

    @Override
    public boolean isValidPath(Config config) {
        return isValidPath(config.getBeatorajaPath());
    }
}
