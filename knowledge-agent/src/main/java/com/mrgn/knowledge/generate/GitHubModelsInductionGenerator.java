package com.mrgn.knowledge.generate;

import com.mrgn.knowledge.config.KnowledgeProperties;
import com.mrgn.knowledge.domain.KnowledgeArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class GitHubModelsInductionGenerator {

    private static final Logger log = LoggerFactory.getLogger(GitHubModelsInductionGenerator.class);

    private static final String SYSTEM = """
            You are a knowledge-documentation agent for an internet banking application.
            Write Markdown induction/onboarding documentation for new engineers and testers.
            Use ONLY the Jira stories and regression-master test cases provided.
            Required sections:
            1. Application overview
            2. Jira story walkthrough (each story with linked tests)
            3. Regression landscape (coverage and gaps)
            4. First-week onboarding checklist
            5. Automation context
            Do not invent products, APIs, or test cases that are not in the corpus.
            """;

    private final KnowledgeProperties properties;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilder;

    public GitHubModelsInductionGenerator(
            KnowledgeProperties properties,
            ObjectProvider<ChatClient.Builder> chatClientBuilder) {
        this.properties = properties;
        this.chatClientBuilder = chatClientBuilder;
    }

    public boolean available() {
        return properties.getLlm().isEnabled() && chatClientBuilder.getIfAvailable() != null;
    }

    public String generate(StoryTestLinker.Coverage coverage) {
        ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("GitHub Models ChatClient is not configured. Set KNOWLEDGE_LLM_ENABLED=true, KNOWLEDGE_CHAT_MODEL=openai, and GITHUB_TOKEN.");
        }
        ChatClient client = builder.build();
        String user = corpusPrompt(coverage);
        log.info("Calling GitHub Models ({}) for induction generation", properties.getLlm().getModel());
        return client.prompt()
                .system(SYSTEM)
                .user(user)
                .call()
                .content();
    }

    private String corpusPrompt(StoryTestLinker.Coverage coverage) {
        StringBuilder sb = new StringBuilder();
        sb.append("Jira stories and regression tests for internet banking:\n\n");
        for (StoryTestLinker.LinkedStory linked : coverage.linked()) {
            KnowledgeArtifact story = linked.story();
            sb.append("STORY ").append(story.getExternalId()).append(": ").append(story.getTitle()).append('\n');
            sb.append(story.getContent()).append("\n");
            sb.append("Linked tests:\n");
            if (linked.tests().isEmpty()) {
                sb.append("- (none)\n");
            } else {
                for (KnowledgeArtifact test : linked.tests()) {
                    sb.append("- ").append(test.getExternalId()).append(" ").append(test.getTitle()).append('\n');
                    sb.append(test.getContent()).append('\n');
                }
            }
            sb.append('\n');
        }
        if (!coverage.orphanTests().isEmpty()) {
            sb.append("Unlinked tests:\n");
            coverage.orphanTests().forEach(t -> sb.append("- ").append(t.getExternalId()).append(" ").append(t.getTitle()).append('\n'));
        }
        return sb.toString();
    }
}
