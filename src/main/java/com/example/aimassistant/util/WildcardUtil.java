package com.example.aimassistant.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class WildcardUtil {
    private static final ConcurrentHashMap<String, Pattern> CACHE = new ConcurrentHashMap<>();

    public static boolean matches(String pattern, String text) {
        Pattern regex = CACHE.computeIfAbsent(pattern, p -> {
            StringBuilder sb = new StringBuilder();
            sb.append("^");
            String[] parts = p.split("\\*", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append(".*");
                if (!parts[i].isEmpty()) sb.append(Pattern.quote(parts[i]));
            }
            sb.append("$");
            return Pattern.compile(sb.toString());
        });
        return regex.matcher(text).matches();
    }
}
