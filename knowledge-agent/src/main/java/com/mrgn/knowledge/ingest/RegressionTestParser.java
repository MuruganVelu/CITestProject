package com.mrgn.knowledge.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrgn.knowledge.domain.ArtifactType;
import com.mrgn.knowledge.domain.KnowledgeArtifact;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class RegressionTestParser {

    private final ObjectMapper objectMapper;

    public RegressionTestParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<KnowledgeArtifact> parse(String filename, String content) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".json") || content.trim().startsWith("{") || content.trim().startsWith("[")) {
            return parseJson(filename, content);
        }
        if (name.endsWith(".csv") || looksLikeCsv(content)) {
            return parseCsv(filename, content);
        }
        return parseMarkdown(filename, content);
    }

    private List<KnowledgeArtifact> parseJson(String filename, String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode array = root.isArray() ? root : root.path("tests");
            List<KnowledgeArtifact> artifacts = new ArrayList<>();
            if (array.isArray()) {
                for (JsonNode test : array) {
                    artifacts.add(fromNode(filename, test));
                }
            }
            return artifacts;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to parse regression tests JSON from " + filename + ": " + ex.getMessage(), ex);
        }
    }

    private KnowledgeArtifact fromNode(String filename, JsonNode test) {
        String id = firstNonBlank(text(test, "id"), text(test, "key"), text(test, "tcId"));
        String title = firstNonBlank(text(test, "title"), text(test, "name"), text(test, "summary"));
        String steps = firstNonBlank(text(test, "steps"), text(test, "procedure"));
        String expected = text(test, "expected");
        String story = firstNonBlank(text(test, "story"), text(test, "jira"), text(test, "relatedStory"));
        String body = buildBody(steps, expected, story);
        KnowledgeArtifact artifact = new KnowledgeArtifact(
                ArtifactType.REGRESSION_TEST,
                IdExtractor.normalizeTestId(id.isBlank() ? firstOrNull(IdExtractor.testIds(title)) : id),
                title,
                body,
                filename);
        artifact.getRelatedIds().addAll(IdExtractor.storyKeys(story, title, body));
        return artifact;
    }

    private List<KnowledgeArtifact> parseCsv(String filename, String content) {
        List<List<String>> rows = SimpleCsv.parse(content);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> header = rows.get(0).stream()
                .map(h -> h.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", ""))
                .toList();
        Map<String, Integer> idx = index(header);
        List<KnowledgeArtifact> artifacts = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            String id = cell(row, idx, "tcid", "id", "testid", "caseid");
            String title = cell(row, idx, "title", "name", "summary", "testcase");
            String steps = cell(row, idx, "steps", "procedure", "actions");
            String expected = cell(row, idx, "expected", "expectedresult", "result");
            String story = cell(row, idx, "story", "jira", "storyid", "relatedstory");
            KnowledgeArtifact artifact = new KnowledgeArtifact(
                    ArtifactType.REGRESSION_TEST,
                    IdExtractor.normalizeTestId(id.isBlank() ? firstOrNull(IdExtractor.testIds(title)) : id),
                    title,
                    buildBody(steps, expected, story),
                    filename);
            artifact.getRelatedIds().addAll(IdExtractor.storyKeys(story, title, steps, expected));
            artifacts.add(artifact);
        }
        return artifacts;
    }

    private List<KnowledgeArtifact> parseMarkdown(String filename, String content) {
        List<KnowledgeArtifact> artifacts = new ArrayList<>();
        String[] blocks = content.split("(?m)^#{1,3}\\s+");
        for (String block : blocks) {
            if (block.isBlank()) {
                continue;
            }
            String[] lines = block.split("\\R", 2);
            String heading = lines[0].trim();
            String body = lines.length > 1 ? lines[1].trim() : "";
            List<String> ids = IdExtractor.testIds(heading, body);
            if (ids.isEmpty()) {
                continue;
            }
            String title = heading.replaceAll("(?i)TC[-_]?\\d+", "").replace(":", "").trim();
            KnowledgeArtifact artifact = new KnowledgeArtifact(
                    ArtifactType.REGRESSION_TEST,
                    ids.get(0),
                    title,
                    body,
                    filename);
            artifact.getRelatedIds().addAll(IdExtractor.storyKeys(heading, body));
            artifacts.add(artifact);
        }
        return artifacts;
    }

    private static boolean looksLikeCsv(String content) {
        String first = content.lines().findFirst().orElse("");
        String lower = first.toLowerCase(Locale.ROOT);
        return first.contains(",") && (lower.contains("tc") || lower.contains("test"));
    }

    private static Map<String, Integer> index(List<String> header) {
        java.util.HashMap<String, Integer> map = new java.util.HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            map.put(header.get(i), i);
        }
        return map;
    }

    private static String cell(List<String> row, Map<String, Integer> idx, String... names) {
        for (String name : names) {
            Integer i = idx.get(name);
            if (i != null && i < row.size()) {
                return row.get(i);
            }
        }
        return "";
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.isTextual() ? value.asText() : value.toString();
    }

    private static String buildBody(String steps, String expected, String story) {
        StringBuilder sb = new StringBuilder();
        if (story != null && !story.isBlank()) {
            sb.append("Story: ").append(story.trim()).append("\n");
        }
        if (steps != null && !steps.isBlank()) {
            sb.append("Steps:\n").append(steps.trim()).append("\n");
        }
        if (expected != null && !expected.isBlank()) {
            sb.append("Expected: ").append(expected.trim());
        }
        return sb.toString().trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String firstOrNull(List<String> values) {
        return values.isEmpty() ? "" : values.get(0);
    }
}
