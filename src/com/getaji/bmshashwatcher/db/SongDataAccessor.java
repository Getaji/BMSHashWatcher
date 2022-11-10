package com.getaji.bmshashwatcher.db;

import com.getaji.bmshashwatcher.model.BMSHashData;
import com.getaji.bmshashwatcher.model.Config;
import com.getaji.bmshashwatcher.model.SongData;
import com.getaji.bmshashwatcher.model.SupportedHashType;

import java.sql.SQLException;
import java.util.List;

/**
 * 楽曲データにアクセスするインターフェース
 */
public interface SongDataAccessor {
    /**
     * アクセサが開かれているか
     */
    boolean isOpen();

    /**
     * アクセサを開く
     *
     * @param config 設定データ
     * @throws SQLException           SQLに関する例外
     * @throws ClassNotFoundException JDBC初期化失敗などの例外
     * @throws IllegalStateException  不正な状態に関する例外
     */
    void open(Config config) throws SQLException, ClassNotFoundException, IllegalStateException;

    /**
     * アクセサを閉じる
     *
     * @throws SQLException SQLに関する例外
     */
    void close() throws SQLException;

    /**
     * MD5ハッシュで楽曲データを検索する
     *
     * @param hash MD5ハッシュ
     * @return 結果
     * @throws SQLException SQLに関する例外
     */
    Result findBMSByMD5(String hash) throws SQLException;

    /**
     * SHA-256ハッシュで楽曲データを検索する
     *
     * @param hash SHA-256ハッシュ
     * @return 結果
     * @throws SQLException SQLに関する例外
     */
    Result findBMSBySHA256(String hash) throws SQLException;

    List<Result> findAll(List<Request> hashList) throws SQLException;

    /**
     * このアクセサがサポートしているハッシュの種類を返す
     */
    SupportedHashType getSupportedHashType();

    /**
     * 与えられたパスがこのアクセサで利用できるかを返す
     *
     * @param baseDir パス文字列
     * @return 利用できるか
     */
    boolean isValidPath(String baseDir);

    /**
     * 設定から利用するパスを取得し、このアクセサで利用できるかを返す
     *
     * @param config 設定
     * @return 利用できるか
     */
    boolean isValidPath(Config config);

    /**
     * 与えられたHashDataのハッシュタイプがこのアクセサで利用できるかを返す
     *
     * @param hashType ハッシュタイプ
     * @return 利用できるか
     */
    default boolean isSupportHashType(BMSHashData.HashType hashType) {
        switch (getSupportedHashType()) {
            case MD5 -> {
                return hashType == BMSHashData.HashType.MD5;
            }
            case SHA256 -> {
                return hashType == BMSHashData.HashType.SHA256;
            }
            case MD5_AND_SHA256 -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    record Request(BMSHashData.HashType hashType, String hash) {
    }

    /**
     * 楽曲データを取得した結果を格納するレコード
     *
     * @param hashType 取得時に指定されたハッシュの種類
     * @param hash     取得時に指定されたハッシュ値
     * @param songData 取得した結果（存在しなければnull）
     */
    record Result(BMSHashData.HashType hashType, String hash, SongData songData) {
    }
}
