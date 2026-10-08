package com.mrgn.knowledge.ingest;

import java.util.ArrayList;
import java.util.List;

final class SimpleCsv {

    private SimpleCsv() {
    }

    static List<List<String>> parse(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < normalized.length() && normalized.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                current.add(field.toString().trim());
                field.setLength(0);
            } else if (c == '\n') {
                current.add(field.toString().trim());
                field.setLength(0);
                if (current.stream().anyMatch(s -> !s.isBlank())) {
                    rows.add(current);
                }
                current = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        current.add(field.toString().trim());
        if (current.stream().anyMatch(s -> !s.isBlank())) {
            rows.add(current);
        }
        return rows;
    }
}
