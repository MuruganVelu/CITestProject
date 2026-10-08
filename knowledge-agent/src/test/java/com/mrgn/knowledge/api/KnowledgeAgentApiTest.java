package com.mrgn.knowledge.api;

import com.mrgn.knowledge.corpus.KnowledgeCorpus;
import com.mrgn.knowledge.domain.ArtifactType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "knowledge.sample-dir=sample-data",
        "knowledge.output-dir=target/test-output",
        "knowledge.llm.enabled=false",
        "spring.ai.model.chat=none",
        "spring.ai.model.embedding=none",
        "spring.ai.model.image=none",
        "spring.ai.model.moderation=none",
        "spring.ai.model.audio.speech=none",
        "spring.ai.model.audio.transcription=none"
})
@AutoConfigureMockMvc
class KnowledgeAgentApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    KnowledgeCorpus corpus;

    @Test
    void sampleCorpusLoadedAndInductionGenerated() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corpus.jiraStories").value(20))
                .andExpect(jsonPath("$.corpus.regressionTests").value(50))
                .andExpect(jsonPath("$.llm.provider").value("github-models"))
                .andExpect(jsonPath("$.llm.enabled").value(false));

        MvcResult result = mockMvc.perform(post("/api/induction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.engine").value("heuristic"))
                .andExpect(jsonPath("$.storyCount").value(20))
                .andExpect(jsonPath("$.testCount").value(50))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("IB-101");
        assertThat(body).contains("TC-01");
        assertThat(body.toLowerCase()).contains("internet banking");
        assertThat(corpus.findByType(ArtifactType.REGRESSION_TEST)).hasSize(50);

        Path latest = Path.of("target/test-output/induction.md");
        assertThat(latest).exists();
        assertThat(Files.readString(latest)).contains("First-week onboarding checklist");
    }

    @Test
    void multipartUploadReplacesCorpus() throws Exception {
        MockMultipartFile stories = new MockMultipartFile(
                "jiraStories",
                "stories.json",
                "application/json",
                Files.readAllBytes(Path.of("sample-data/jira/stories.json")));
        MockMultipartFile tests = new MockMultipartFile(
                "regressionTests",
                "regression-master.csv",
                "text/csv",
                Files.readAllBytes(Path.of("sample-data/regression/regression-master.csv")));

        mockMvc.perform(multipart("/api/ingest").file(stories).file(tests))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jiraStories").value(20))
                .andExpect(jsonPath("$.regressionTests").value(50));
    }
}
