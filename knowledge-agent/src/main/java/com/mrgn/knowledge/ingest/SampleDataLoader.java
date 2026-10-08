package com.mrgn.knowledge.ingest;

import com.mrgn.knowledge.config.KnowledgeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Order(0)
public class SampleDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataLoader.class);

    private final KnowledgeProperties properties;
    private final ArtifactIngestService ingestService;

    public SampleDataLoader(KnowledgeProperties properties, ArtifactIngestService ingestService) {
        this.properties = properties;
        this.ingestService = ingestService;
    }

    @Override
    public void run(ApplicationArguments args) {
        loadIfPresent();
    }

    public void loadIfPresent() {
        try {
            Path sampleDir = Path.of(properties.getSampleDir());
            Path stories = sampleDir.resolve("jira").resolve("stories.json");
            Path tests = sampleDir.resolve("regression").resolve("regression-master.csv");
            if (Files.isRegularFile(stories) && Files.isRegularFile(tests)) {
                ingestService.ingestJiraStories(read(stories), true);
                ingestService.ingestRegressionTests(read(tests), true);
                log.info("Loaded sample corpus from {}", sampleDir.toAbsolutePath());
                return;
            }
            loadFromClasspath();
        } catch (Exception ex) {
            log.warn("Sample corpus not loaded: {}", ex.getMessage());
        }
    }

    private void loadFromClasspath() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] storyResources = resolver.getResources("classpath*:sample-data/jira/stories.json");
        Resource[] testResources = resolver.getResources("classpath*:sample-data/regression/regression-master.csv");
        if (storyResources.length == 0 || testResources.length == 0) {
            log.info("No classpath sample-data found");
            return;
        }
        ingestService.ingestJiraStories(read(storyResources[0]), true);
        ingestService.ingestRegressionTests(read(testResources[0]), true);
        log.info("Loaded sample corpus from classpath");
    }

    private static UploadedFile read(Path path) throws Exception {
        return new UploadedFile(path.getFileName().toString(), Files.readString(path, StandardCharsets.UTF_8));
    }

    private static UploadedFile read(Resource resource) throws Exception {
        String name = resource.getFilename() == null ? "sample" : resource.getFilename();
        return new UploadedFile(name, resource.getContentAsString(StandardCharsets.UTF_8));
    }
}
