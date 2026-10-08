package com.mrgn.knowledge.generate;

import com.mrgn.knowledge.config.KnowledgeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class StartupGenerationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupGenerationRunner.class);

    private final KnowledgeProperties properties;
    private final InductionService inductionService;

    public StartupGenerationRunner(KnowledgeProperties properties, InductionService inductionService) {
        this.properties = properties;
        this.inductionService = inductionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isGenerateOnStartup()) {
            return;
        }
        InductionService.InductionResult result = inductionService.generate();
        log.info("Startup induction written with {} ({} stories, {} tests) -> {}",
                result.engine(), result.storyCount(), result.testCount(), result.outputPath());
    }
}
