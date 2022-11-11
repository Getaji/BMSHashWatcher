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
 * beatorajaの楽曲データを取得するクラス
 */
public class BeatorajaSongDataAccessor implements SongDataAccessor {
    private Connection connection;

    @Override
    public boolean isOpen() {
        return connection != null;
    }

    @Override
    public void open(Config config) throws SQLException, ClassNotFoundException,
            IllegalStateException {
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
        final List<String> sha256HashList =
                groupedHashList.getOrDefault(BMSHashData.HashType.SHA256, Collections.emptyList());
        String queryBuilder = "SELECT DISTINCT md5, sha256, title, subtitle FROM song\n" +
                "WHERE path <> ''\n" +
                "AND (md5 IN (" +
                String.join(",", Collections.nCopies(md5HashList.size(), "?")) +
                ") OR sha256 IN (" +
                String.join(",", Collections.nCopies(sha256HashList.size(), "?")) +
                "))\n";
        final PreparedStatement statement = connection.prepareStatement(queryBuilder);
        for (int i = 0; i < md5HashList.size(); i++) {
            statement.setString(i + 1, md5HashList.get(i));
        }
        for (int i = 0; i < sha256HashList.size(); i++) {
            statement.setString(i + md5HashList.size() + 1, sha256HashList.get(i));
        }
        final ResultSet resultSet = statement.executeQuery();
        List<Result> foundResults = new ArrayList<>();
        while (resultSet.next()) {
            final SongData songData = new SongData(
                    resultSet.getString("md5"),
                    resultSet.getString("sha256"),
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
