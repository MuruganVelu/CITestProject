package com.mrgn.knowledge.generate;

import com.mrgn.knowledge.corpus.KnowledgeCorpus;
import com.mrgn.knowledge.domain.ArtifactType;
import com.mrgn.knowledge.domain.KnowledgeArtifact;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class StoryTestLinker {

    public record LinkedStory(KnowledgeArtifact story, List<KnowledgeArtifact> tests) {
    }

    public record Coverage(List<LinkedStory> linked, List<KnowledgeArtifact> orphanTests, List<KnowledgeArtifact> uncoveredStories) {
    }

    public Coverage link(KnowledgeCorpus corpus) {
        List<KnowledgeArtifact> stories = corpus.findByType(ArtifactType.JIRA_STORY);
        List<KnowledgeArtifact> tests = corpus.findByType(ArtifactType.REGRESSION_TEST);
        Map<String, KnowledgeArtifact> testsById = new LinkedHashMap<>();
        for (KnowledgeArtifact test : tests) {
            if (test.getExternalId() != null) {
                testsById.put(normalize(test.getExternalId()), test);
            }
        }

        List<LinkedStory> linked = new ArrayList<>();
        List<KnowledgeArtifact> uncovered = new ArrayList<>();
        for (KnowledgeArtifact story : stories) {
            List<KnowledgeArtifact> matches = new ArrayList<>();
            for (KnowledgeArtifact test : tests) {
                if (references(test, story) || references(story, test)) {
                    if (matches.stream().noneMatch(t -> t.getId().equals(test.getId()))) {
                        matches.add(test);
                    }
                }
            }
            for (String related : story.getRelatedIds()) {
                KnowledgeArtifact test = testsById.get(normalize(related));
                if (test != null && matches.stream().noneMatch(t -> t.getId().equals(test.getId()))) {
                    matches.add(test);
                }
            }
            linked.add(new LinkedStory(story, matches));
            if (matches.isEmpty()) {
                uncovered.add(story);
            }
        }

        List<KnowledgeArtifact> orphans = tests.stream()
                .filter(test -> stories.stream().noneMatch(story -> references(test, story) || references(story, test)))
                .toList();
        return new Coverage(linked, orphans, uncovered);
    }

    private static boolean references(KnowledgeArtifact from, KnowledgeArtifact to) {
        if (to.getExternalId() == null) {
            return false;
        }
        String needle = normalize(to.getExternalId());
        if (from.getRelatedIds().stream().anyMatch(id -> normalize(id).equals(needle))) {
            return true;
        }
        String haystack = ((from.getTitle() == null ? "" : from.getTitle()) + "\n" + (from.getContent() == null ? "" : from.getContent()))
                .toUpperCase(Locale.ROOT);
        return haystack.contains(to.getExternalId().toUpperCase(Locale.ROOT));
    }

    private static String normalize(String id) {
        if (id == null) {
            return "";
        }
        String trimmed = id.trim().toUpperCase(Locale.ROOT);
        if (trimmed.matches("TC[-_]?\\d+")) {
            String digits = trimmed.replaceAll("TC[-_]?", "");
            return "TC-" + Integer.parseInt(digits);
        }
        return trimmed;
    }
}
