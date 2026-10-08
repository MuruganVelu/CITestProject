package com.mrgn.knowledge.generate;

import com.mrgn.knowledge.config.KnowledgeProperties;
import com.mrgn.knowledge.corpus.KnowledgeCorpus;
import com.mrgn.knowledge.domain.ArtifactType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class InductionService {

    private final KnowledgeCorpus corpus;
    private final StoryTestLinker linker;
    private final HeuristicInductionGenerator heuristic;
    private final GitHubModelsInductionGenerator githubModels;
    private final KnowledgeProperties properties;

    public InductionService(
            KnowledgeCorpus corpus,
            StoryTestLinker linker,
            HeuristicInductionGenerator heuristic,
            GitHubModelsInductionGenerator githubModels,
            KnowledgeProperties properties) {
        this.corpus = corpus;
        this.linker = linker;
        this.heuristic = heuristic;
        this.githubModels = githubModels;
        this.properties = properties;
    }

    public InductionResult generate() {
        if (corpus.findByType(ArtifactType.JIRA_STORY).isEmpty()) {
            throw new IllegalStateException("No Jira stories ingested. Upload a stories file first.");
        }
        if (corpus.findByType(ArtifactType.REGRESSION_TEST).isEmpty()) {
            throw new IllegalStateException("No regression tests ingested. Upload a regression master first.");
        }
        StoryTestLinker.Coverage coverage = linker.link(corpus);
        String engine;
        String markdown;
        if (githubModels.available()) {
            try {
                markdown = githubModels.generate(coverage);
                engine = "github-models";
            } catch (RuntimeException ex) {
                markdown = heuristic.generate(coverage, "heuristic fallback after GitHub Models error: " + ex.getMessage());
                engine = "heuristic-fallback";
            }
        } else {
            markdown = heuristic.generate(coverage, "heuristic (GitHub Models disabled)");
            engine = "heuristic";
        }
        Path written = write(markdown);
        return new InductionResult(engine, markdown, written.toString(), coverage.linked().size(),
                corpus.findByType(ArtifactType.REGRESSION_TEST).size());
    }

    private Path write(String markdown) {
        try {
            Path dir = Path.of(properties.getOutputDir());
            Files.createDirectories(dir);
            String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(Instant.now());
            Path timestamped = dir.resolve(stamp + "-induction.md");
            Path latest = dir.resolve("induction.md");
            Files.writeString(timestamped, markdown);
            Files.writeString(latest, markdown);
            return latest;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write induction markdown: " + ex.getMessage(), ex);
        }
    }

    public record InductionResult(String engine, String markdown, String outputPath, int storyCount, int testCount) {
    }
}
