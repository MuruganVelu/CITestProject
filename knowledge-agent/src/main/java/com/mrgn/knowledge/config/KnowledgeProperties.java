package com.mrgn.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowledge")
public class KnowledgeProperties {

    private String sampleDir = "sample-data";
    private String outputDir = "output";
    private boolean generateOnStartup;
    private final Llm llm = new Llm();

    public String getSampleDir() {
        return sampleDir;
    }

    public void setSampleDir(String sampleDir) {
        this.sampleDir = sampleDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public boolean isGenerateOnStartup() {
        return generateOnStartup;
    }

    public void setGenerateOnStartup(boolean generateOnStartup) {
        this.generateOnStartup = generateOnStartup;
    }

    public Llm getLlm() {
        return llm;
    }

    public static class Llm {
        private boolean enabled;
        private String model = "openai/gpt-4o-mini";
        private String provider = "github-models";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }
    }
}
