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
public class JiraStoryParser {

    private final ObjectMapper objectMapper;

    public JiraStoryParser(ObjectMapper objectMapper) {
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
            List<KnowledgeArtifact> artifacts = new ArrayList<>();
            if (root.has("issues") && root.get("issues").isArray()) {
                for (JsonNode issue : root.get("issues")) {
                    artifacts.add(fromJiraIssue(filename, issue));
                }
                return artifacts;
            }
            JsonNode array = root.isArray() ? root : root.path("stories");
            if (array.isArray()) {
                for (JsonNode story : array) {
                    artifacts.add(fromSimpleStory(filename, story));
                }
            }
            return artifacts;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to parse Jira stories JSON from " + filename + ": " + ex.getMessage(), ex);
        }
    }

    private KnowledgeArtifact fromJiraIssue(String filename, JsonNode issue) {
        String key = text(issue, "key");
        JsonNode fields = issue.path("fields");
        String summary = firstNonBlank(text(fields, "summary"), text(issue, "summary"));
        String description = firstNonBlank(text(fields, "description"), text(issue, "description"));
        String acceptance = firstNonBlank(
                text(fields, "customfield_acceptance"),
                text(fields, "acceptanceCriteria"),
                text(issue, "acceptanceCriteria"));
        String body = joinSections(description, acceptance);
        KnowledgeArtifact artifact = new KnowledgeArtifact(ArtifactType.JIRA_STORY, key, summary, body, filename);
        artifact.getRelatedIds().addAll(IdExtractor.testIds(body, jsonArrayAsText(issue.path("relatedTests"))));
        return artifact;
    }

    private KnowledgeArtifact fromSimpleStory(String filename, JsonNode story) {
        String key = firstNonBlank(text(story, "key"), text(story, "id"), text(story, "externalId"));
        String title = firstNonBlank(text(story, "summary"), text(story, "title"));
        String description = firstNonBlank(text(story, "description"), text(story, "content"));
        String acceptance = text(story, "acceptanceCriteria");
        if (story.path("acceptanceCriteria").isArray()) {
            acceptance = jsonArrayAsText(story.path("acceptanceCriteria"));
        }
        String body = joinSections(description, acceptance);
        KnowledgeArtifact artifact = new KnowledgeArtifact(ArtifactType.JIRA_STORY, key, title, body, filename);
        artifact.getRelatedIds().addAll(IdExtractor.testIds(body, jsonArrayAsText(story.path("relatedTests"))));
        artifact.getRelatedIds().addAll(IdExtractor.storyKeys(key));
        return artifact;
    }

    private List<KnowledgeArtifact> parseCsv(String filename, String content) {
        List<List<String>> rows = SimpleCsv.parse(content);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> header = rows.get(0).stream().map(h -> h.toLowerCase(Locale.ROOT)).toList();
        Map<String, Integer> idx = index(header);
        List<KnowledgeArtifact> artifacts = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            String key = cell(row, idx, "key", "id", "story", "issue");
            String title = cell(row, idx, "summary", "title");
            String description = cell(row, idx, "description", "content", "details");
            String acceptance = cell(row, idx, "acceptance", "acceptancecriteria", "ac");
            String related = cell(row, idx, "tests", "relatedtests", "tc");
            KnowledgeArtifact artifact = new KnowledgeArtifact(
                    ArtifactType.JIRA_STORY,
                    key,
                    title,
                    joinSections(description, acceptance),
                    filename);
            artifact.getRelatedIds().addAll(IdExtractor.testIds(related, description, acceptance));
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
            List<String> keys = IdExtractor.storyKeys(heading, body);
            if (keys.isEmpty()) {
                continue;
            }
            String key = keys.get(0);
            String title = heading.replace(key, "").replace(":", "").trim();
            KnowledgeArtifact artifact = new KnowledgeArtifact(ArtifactType.JIRA_STORY, key, title, body, filename);
            artifact.getRelatedIds().addAll(IdExtractor.testIds(heading, body));
            artifacts.add(artifact);
        }
        return artifacts;
    }

    private static boolean looksLikeCsv(String content) {
        String first = content.lines().findFirst().orElse("");
        return first.contains(",") && (first.toLowerCase(Locale.ROOT).contains("key")
                || first.toLowerCase(Locale.ROOT).contains("summary"));
    }

    private static Map<String, Integer> index(List<String> header) {
        java.util.HashMap<String, Integer> map = new java.util.HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            map.put(header.get(i).replace(" ", ""), i);
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
        if (value.isTextual()) {
            return value.asText();
        }
        if (value.isArray() || value.isObject()) {
            return value.toString();
        }
        return value.asText();
    }

    private static String jsonArrayAsText(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return "";
        }
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : node) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(item.isTextual() ? item.asText() : item.toString());
            }
            return sb.toString();
        }
        return node.isTextual() ? node.asText() : "";
    }

    private static String joinSections(String description, String acceptance) {
        StringBuilder sb = new StringBuilder();
        if (description != null && !description.isBlank()) {
            sb.append(description.trim());
        }
        if (acceptance != null && !acceptance.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n\nAcceptance criteria:\n");
            }
            sb.append(acceptance.trim());
        }
        return sb.toString();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
