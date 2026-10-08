package com.mrgn.knowledge.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrgn.knowledge.domain.KnowledgeArtifact;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RegressionMasterParseTest {

    @Test
    void parsesFiftyInternetBankingCases() throws Exception {
        String csv = Files.readString(Path.of("sample-data/regression/regression-master.csv"), StandardCharsets.UTF_8);
        List<KnowledgeArtifact> tests = new RegressionTestParser(new ObjectMapper()).parse("regression-master.csv", csv);
        assertThat(tests).hasSize(50);
        Set<String> ids = tests.stream().map(KnowledgeArtifact::getExternalId).collect(Collectors.toSet());
        assertThat(ids).hasSize(50);
        assertThat(ids).contains("TC-01", "TC-50");
        assertThat(tests).allMatch(t -> t.getRelatedIds() != null && !t.getRelatedIds().isEmpty());
    }

    @Test
    void parsesTwentyJiraStories() throws Exception {
        String json = Files.readString(Path.of("sample-data/jira/stories.json"), StandardCharsets.UTF_8);
        List<KnowledgeArtifact> stories = new JiraStoryParser(new ObjectMapper()).parse("stories.json", json);
        assertThat(stories).hasSize(20);
        assertThat(stories.stream().map(KnowledgeArtifact::getExternalId)).contains("IB-101", "IB-120");
    }
}
