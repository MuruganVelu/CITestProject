package com.mrgn.knowledge.ingest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class IdExtractor {

    private static final Pattern STORY_KEY = Pattern.compile("\\b([A-Z][A-Z0-9]+-\\d+)\\b");
    private static final Pattern TEST_ID = Pattern.compile("\\b(TC[-_]?\\d+)\\b", Pattern.CASE_INSENSITIVE);

    private IdExtractor() {
    }

    static List<String> storyKeys(String... texts) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String text : texts) {
            if (text == null) {
                continue;
            }
            Matcher matcher = STORY_KEY.matcher(text);
            while (matcher.find()) {
                keys.add(matcher.group(1).toUpperCase());
            }
        }
        return new ArrayList<>(keys);
    }

    static List<String> testIds(String... texts) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String text : texts) {
            if (text == null) {
                continue;
            }
            Matcher matcher = TEST_ID.matcher(text);
            while (matcher.find()) {
                keys.add(normalizeTestId(matcher.group(1)));
            }
        }
        return new ArrayList<>(keys);
    }

    static String normalizeTestId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("(?i)[^0-9]", "");
        if (digits.isBlank()) {
            return raw.trim().toUpperCase();
        }
        return String.format("TC-%02d", Integer.parseInt(digits));
    }
}
