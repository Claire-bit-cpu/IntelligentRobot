package com.example.intelligentxtsystem.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {

    /**
     * 解析 /weather 城市名
     * 示例：
     *   "/weather 北京"     -> "北京"
     *   "/weather   上海"   -> "上海"
     *   "/weather"          -> null
     */
    private static final Pattern WEATHER_PATTERN =
            Pattern.compile("^/weather\\s+(\\S+)$");

    public static String parseWeatherCity(String text) {
        if (text == null) {
            return null;
        }

        Matcher matcher = WEATHER_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
