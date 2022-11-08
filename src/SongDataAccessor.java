import java.sql.SQLException;

public interface SongDataAccessor {
    boolean isOpen();

    void open(Config config) throws SQLException, ClassNotFoundException, IllegalStateException;

    void close() throws SQLException;

    Result findBMSByMD5(String hash) throws SQLException;

    Result findBMSBySHA256(String hash) throws SQLException;

    SupportedHashType getSupportedHashType();

    default boolean isSupportHashType(HashData.HashType hashType) {
        switch (getSupportedHashType()) {
            case MD5 -> {
                return hashType == HashData.HashType.MD5;
            }
            case SHA256 -> {
                return hashType == HashData.HashType.SHA256;
            }
            case MD5_AND_SHA256 -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    record Result(HashData.HashType hashType, String hash, SongData songData) {
    }
}
