package com.mrgn.knowledge.api;

import com.mrgn.knowledge.corpus.KnowledgeCorpus;
import com.mrgn.knowledge.domain.KnowledgeArtifact;
import com.mrgn.knowledge.ingest.ArtifactIngestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class IngestController {

    private final ArtifactIngestService ingestService;
    private final KnowledgeCorpus corpus;

    public IngestController(ArtifactIngestService ingestService, KnowledgeCorpus corpus) {
        this.ingestService = ingestService;
        this.corpus = corpus;
    }

    @PostMapping("/ingest")
    public Map<String, Object> ingestBoth(
            @RequestParam("jiraStories") MultipartFile jiraStories,
            @RequestParam("regressionTests") MultipartFile regressionTests) {
        List<KnowledgeArtifact> stories = ingestService.ingestJiraStories(jiraStories, true);
        List<KnowledgeArtifact> tests = ingestService.ingestRegressionTests(regressionTests, true);
        return summary("Replaced corpus from two uploads", stories.size(), tests.size());
    }

    @PostMapping("/ingest/jira-stories")
    public Map<String, Object> ingestStories(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "replace", defaultValue = "true") boolean replace) {
        List<KnowledgeArtifact> stories = ingestService.ingestJiraStories(file, replace);
        return Map.of("ingested", stories.size(), "type", "JIRA_STORY", "items", stories);
    }

    @PostMapping("/ingest/regression-tests")
    public Map<String, Object> ingestTests(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "replace", defaultValue = "true") boolean replace) {
        List<KnowledgeArtifact> tests = ingestService.ingestRegressionTests(file, replace);
        return Map.of("ingested", tests.size(), "type", "REGRESSION_TEST", "items", tests);
    }

    @GetMapping("/artifacts")
    public Map<String, Object> artifacts() {
        return Map.of("items", corpus.findAll(), "count", corpus.findAll().size());
    }

    private Map<String, Object> summary(String message, int stories, int tests) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("jiraStories", stories);
        body.put("regressionTests", tests);
        return body;
    }
}
