package com.mrgn.knowledge.ingest;

import com.mrgn.knowledge.corpus.KnowledgeCorpus;
import com.mrgn.knowledge.domain.ArtifactType;
import com.mrgn.knowledge.domain.KnowledgeArtifact;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ArtifactIngestService {

    private final KnowledgeCorpus corpus;
    private final JiraStoryParser jiraStoryParser;
    private final RegressionTestParser regressionTestParser;

    public ArtifactIngestService(
            KnowledgeCorpus corpus,
            JiraStoryParser jiraStoryParser,
            RegressionTestParser regressionTestParser) {
        this.corpus = corpus;
        this.jiraStoryParser = jiraStoryParser;
        this.regressionTestParser = regressionTestParser;
    }

    public List<KnowledgeArtifact> ingestJiraStories(MultipartFile file, boolean replace) {
        return ingestJiraStories(toUploaded(file), replace);
    }

    public List<KnowledgeArtifact> ingestJiraStories(UploadedFile file, boolean replace) {
        List<KnowledgeArtifact> parsed = jiraStoryParser.parse(file.filename(), file.content());
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("No Jira stories found in " + file.filename());
        }
        if (replace) {
            corpus.clearType(ArtifactType.JIRA_STORY);
        }
        return corpus.saveAll(parsed);
    }

    public List<KnowledgeArtifact> ingestRegressionTests(MultipartFile file, boolean replace) {
        return ingestRegressionTests(toUploaded(file), replace);
    }

    public List<KnowledgeArtifact> ingestRegressionTests(UploadedFile file, boolean replace) {
        List<KnowledgeArtifact> parsed = regressionTestParser.parse(file.filename(), file.content());
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("No regression test cases found in " + file.filename());
        }
        if (replace) {
            corpus.clearType(ArtifactType.REGRESSION_TEST);
        }
        return corpus.saveAll(parsed);
    }

    private static UploadedFile toUploaded(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is required");
        }
        try {
            return new UploadedFile(
                    file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename(),
                    new String(file.getBytes(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read upload: " + ex.getMessage(), ex);
        }
    }
}
