package ru.igla.duocamera.utils;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Created by lashkov on 06.09.21.
 * Copyright (c) 2021 igla LLC. All rights reserved.
 */
public final class StringUtils {

    public static final String NO_VALUE_CONST = "N/A";

    private StringUtils() {
    }

    /***
     * Check string is null or empty
     * @param cs string to check
     * @return true if null or empty, otherwise false
     */
    public static boolean isNullOrEmpty(final @Nullable CharSequence cs) {
        return (cs == null || String.valueOf(cs).trim().length() == 0);
    }

    public static boolean isNullOrEmpty(final @Nullable List<String> list) {
        return list == null || list.isEmpty();
    }

    @NotNull
    public static String getReadableFileSize(long byteSize, boolean includeUnit) {
        if (byteSize < 1024L) return "0"; //less than 1 kb not to show
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(byteSize) / Math.log10(1024));
        String unit = includeUnit ? " " + units[digitGroups] : "";

        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.US);
        otherSymbols.setDecimalSeparator('.');
        otherSymbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0.#", otherSymbols).format((int) (byteSize / Math.pow(1024, digitGroups))) + unit;
    }

    @NotNull
    public static String localeToString(@NotNull Locale l) {
        return l.getLanguage() + "," + l.getCountry();
    }

    @Nullable
    public static Locale stringToLocale(String s) {
        StringTokenizer tempStringTokenizer = new StringTokenizer(s, ",");
        String language = null;
        String code = null;
        if (tempStringTokenizer.hasMoreTokens()) {
            language = tempStringTokenizer.nextToken();
        }
        if (tempStringTokenizer.hasMoreTokens()) {
            code = tempStringTokenizer.nextToken();
        }
        if (StringUtils.isNullOrEmpty(language) ||
                StringUtils.isNullOrEmpty(code)) {
            return null;
        }
        return new Locale(language, code);
    }
}
