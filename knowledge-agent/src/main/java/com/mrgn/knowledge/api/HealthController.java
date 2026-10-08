package com.mrgn.knowledge.api;

import com.mrgn.knowledge.config.KnowledgeProperties;
import com.mrgn.knowledge.corpus.KnowledgeCorpus;
import com.mrgn.knowledge.domain.ArtifactType;
import com.mrgn.knowledge.generate.GitHubModelsInductionGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final KnowledgeProperties properties;
    private final KnowledgeCorpus corpus;
    private final GitHubModelsInductionGenerator githubModels;

    public HealthController(
            KnowledgeProperties properties,
            KnowledgeCorpus corpus,
            GitHubModelsInductionGenerator githubModels) {
        this.properties = properties;
        this.corpus = corpus;
        this.githubModels = githubModels;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "llm", Map.of(
                        "provider", properties.getLlm().getProvider(),
                        "enabled", properties.getLlm().isEnabled(),
                        "available", githubModels.available(),
                        "model", properties.getLlm().getModel()),
                "corpus", Map.of(
                        "jiraStories", corpus.findByType(ArtifactType.JIRA_STORY).size(),
                        "regressionTests", corpus.findByType(ArtifactType.REGRESSION_TEST).size()));
    }
}
