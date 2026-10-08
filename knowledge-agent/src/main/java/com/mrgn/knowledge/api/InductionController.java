package com.mrgn.knowledge.api;

import com.mrgn.knowledge.config.KnowledgeProperties;
import com.mrgn.knowledge.generate.InductionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InductionController {

    private final InductionService inductionService;
    private final KnowledgeProperties properties;

    public InductionController(InductionService inductionService, KnowledgeProperties properties) {
        this.inductionService = inductionService;
        this.properties = properties;
    }

    @PostMapping("/induction")
    public Map<String, Object> generate() {
        InductionService.InductionResult result = inductionService.generate();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("engine", result.engine());
        body.put("outputPath", result.outputPath());
        body.put("storyCount", result.storyCount());
        body.put("testCount", result.testCount());
        body.put("markdown", result.markdown());
        return body;
    }

    @GetMapping(value = "/induction", produces = MediaType.TEXT_MARKDOWN_VALUE)
    public String latest() throws IOException {
        Path latest = Path.of(properties.getOutputDir()).resolve("induction.md");
        if (!Files.isRegularFile(latest)) {
            throw new IllegalStateException("No induction document yet. POST /api/induction first.");
        }
        return Files.readString(latest);
    }
}
