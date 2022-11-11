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
import java.util.*;
import java.util.stream.Collectors;

/**
 * LR2の楽曲データを取得するクラス
 */
public class LR2SongDataAccessor implements SongDataAccessor {
    private Connection connection;

    @Override
    public boolean isOpen() {
        return connection != null;
    }

    @Override
    public void open(Config config) throws SQLException, ClassNotFoundException,
            IllegalStateException {
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
    public List<Result> findAll(List<Request> requests) throws SQLException {
        final LinkedHashMap<BMSHashData.HashType, List<String>> groupedHashList =
                requests.stream().collect(
                Collectors.groupingBy(
                        Request::hashType,
                        LinkedHashMap::new,
                        Collectors.mapping(Request::hash, Collectors.toList())
                )
        );
        final List<String> md5HashList = groupedHashList.getOrDefault(BMSHashData.HashType.MD5,
                Collections.emptyList());
        String queryBuilder = "SELECT DISTINCT hash, title, subtitle FROM song\n" +
                "WHERE hash IN (" +
                String.join(",", Collections.nCopies(md5HashList.size(), "?")) +
                ")";
        final PreparedStatement statement = connection.prepareStatement(queryBuilder);
        for (int i = 0; i < md5HashList.size(); i++) {
            statement.setString(i + 1, md5HashList.get(i));
        }
        final ResultSet resultSet = statement.executeQuery();
        final List<Result> foundResults = new ArrayList<>();
        while (resultSet.next()) {
            final SongData songData = new SongData(
                    resultSet.getString("hash"),
                    "",
                    resultSet.getString("title"),
                    resultSet.getString("subtitle")
            );
            final Optional<Request> request = requests.stream().filter(req -> req.hash().equals(
                    req.hashType() == BMSHashData.HashType.MD5 ? songData.md5() : songData.sha256()
            )).findFirst();
            request.ifPresent(req -> foundResults.add(new Result(req.hashType(), req.hash(),
                    songData)));
        }
        final List<Result> results = requests.stream().map(req -> foundResults.stream()
                        .filter(res -> res.hash().equals(req.hash()))
                        .findFirst()
                        .orElse(new Result(req.hashType(), req.hash(), null)))
                .toList();
        statement.close();
        resultSet.close();
        return results;
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
