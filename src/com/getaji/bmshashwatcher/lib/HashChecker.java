package com.getaji.bmshashwatcher.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ハッシュ値をチェックするユーティリティクラス
 */
public final class HashChecker {
    private HashChecker() {
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    public static final Pattern PATTERN_MD5 = Pattern.compile("^[a-f\\d]{32}$",
            Pattern.CASE_INSENSITIVE);
    public static final Pattern PATTERN_SHA256 = Pattern.compile("^[a-fA-F\\d]{64}$");

    public static final Pattern PATTERN_MD5_PART = Pattern.compile("[a-f\\d]{32}",
            Pattern.CASE_INSENSITIVE);
    public static final Pattern PATTERN_SHA256_PART = Pattern.compile("[a-fA-F\\d]{64}");
    public static final Pattern PATTERN_PART = Pattern.compile("[a-fA-F\\d]{64}|[a-fA-F\\d]{32}");

    /**
     * 与えられた文字列がMD5ハッシュの形式に一致するかを返す
     *
     * @param s 文字列
     * @return 一致するか
     */
    public static boolean isMD5Hash(String s) {
        return PATTERN_MD5.matcher(s).matches();
    }

    /**
     * 与えられた文字列がSHA-256ハッシュの形式に一致するかを返す
     *
     * @param s 文字列
     * @return 一致するか
     */
    public static boolean isSHA256Hash(String s) {
        return PATTERN_SHA256.matcher(s).matches();
    }

    /**
     * 与えられた文字列にMD5ハッシュの形式に一致する文字列が含まれているかを返す
     *
     * @param s 文字列
     * @return 含まれているか
     */
    public static Optional<String> getMD5HashPart(String s) {
        final Matcher matcher = PATTERN_MD5_PART.matcher(s);
        return Optional.ofNullable(matcher.find() ? matcher.group(0) : null);
    }

    /**
     * 与えられた文字列にSHA-256ハッシュの形式に一致する文字列が含まれているかを返す
     *
     * @param s 文字列
     * @return 含まれているか
     */
    public static Optional<String> getSHA256HashPart(String s) {
        final Matcher matcher = PATTERN_SHA256_PART.matcher(s);
        return Optional.ofNullable(matcher.find() ? matcher.group(0) : null);
    }

    public static List<String> getMD5HashPartAll(String s) {
        final Matcher matcher = PATTERN_MD5_PART.matcher(s);
        final ArrayList<String> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(matcher.group());
        }
        return list;
    }

    public static List<String> getSHA256HashPartAll(String s) {
        final Matcher matcher = PATTERN_SHA256_PART.matcher(s);
        final ArrayList<String> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(matcher.group());
        }
        return list;
    }

    public static List<String> getHashPartAll(String s) {
        final Matcher matcher = PATTERN_PART.matcher(s);
        final ArrayList<String> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(matcher.group());
        }
        return list;
    }
}
