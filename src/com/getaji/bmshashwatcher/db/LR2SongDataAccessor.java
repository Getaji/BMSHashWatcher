package com.getaji.bmshashwatcher.db;

import com.getaji.bmshashwatcher.model.BMSHashData;
import com.getaji.bmshashwatcher.model.Config;
import com.getaji.bmshashwatcher.model.SongData;
import com.getaji.bmshashwatcher.model.SupportedHashType;
import org.sqlite.SQLiteConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

/**
 * beatorajaの楽曲データを取得するクラス
 */
public class LR2SongDataAccessor implements SongDataAccessor {
    private Connection connection;

    @Override
    public boolean isOpen() {
        return connection != null;
    }

    @Override
    public void open(Config config) throws SQLException, ClassNotFoundException, IllegalStateException {
        if (config.getLr2Path().equals("")) {
            throw new IllegalStateException("LR2のパスが設定されていません");
        }
        if (connection != null) return;
        Class.forName("org.sqlite.JDBC");
        final SQLiteConfig sqliteConfig = new SQLiteConfig();
        sqliteConfig.setReadOnly(true);
        final Path dbPath = Paths.get(config.getLr2Path()).resolve("LR2files/Database/song.db");
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
                SELECT hash, title, subtitle FROM song
                WHERE hash = ?
                LIMIT 1
                """);
        statement.setString(1, hash);
        ResultSet resultSet = statement.executeQuery();
        SongData songData = null;
        if (resultSet.next()) {
            songData = new SongData(
                    resultSet.getString("hash"),
                    "",
                    resultSet.getString("title"),
                    resultSet.getString("subtitle")
            );
        }
        statement.close();
        resultSet.close();
        return new Result(BMSHashData.HashType.MD5, hash, songData);
    }

    @Override
    public Result findBMSBySHA256(String hash) {
        throw new UnsupportedOperationException("LR2はSHA-256ハッシュで検索できません");
    }

    @Override
    public SupportedHashType getSupportedHashType() {
        return SupportedHashType.MD5;
    }

    @Override
    public boolean isValidPath(String baseDir) {
        if (baseDir.equals("")) return false;

        return Files.exists(Path.of(baseDir, "LR2files/Database/song.db"));
    }

    @Override
    public boolean isValidPath(Config config) {
        return isValidPath(config.getLr2Path());
    }
}
