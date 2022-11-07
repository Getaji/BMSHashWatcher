import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HashChecker {
    private HashChecker() {}

    public static final Pattern PATTERN_MD5 = Pattern.compile("^[a-f\\d]{32}$", Pattern.CASE_INSENSITIVE);
    public static final Pattern PATTERN_SHA256 = Pattern.compile("^[a-fA-F\\d]{64}$");

    public static final Pattern PATTERN_MD5_PART = Pattern.compile("[a-f\\d]{32}", Pattern.CASE_INSENSITIVE);
    public static final Pattern PATTERN_SHA256_PART = Pattern.compile("[a-fA-F\\d]{64}");

    public static boolean isMD5Hash(String s) {
        return PATTERN_MD5.matcher(s).matches();
    }

    public static boolean isSHA256Hash(String s) {
        return PATTERN_SHA256.matcher(s).matches();
    }

    public static Optional<String> getMD5HashPart(String s) {
        final Matcher matcher = PATTERN_MD5_PART.matcher(s);
        return Optional.ofNullable(matcher.find() ? matcher.group(0) : null);
    }

    public static Optional<String> getSHA256HashPart(String s) {
        final Matcher matcher = PATTERN_SHA256_PART.matcher(s);
        return Optional.ofNullable(matcher.find() ? matcher.group(0) : null);
    }
}
