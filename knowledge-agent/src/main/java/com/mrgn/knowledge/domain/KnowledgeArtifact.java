package com.mrgn.knowledge.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KnowledgeArtifact {

    private String id;
    private ArtifactType type;
    private String externalId;
    private String title;
    private String content;
    private String source;
    private List<String> relatedIds = new ArrayList<>();

    public KnowledgeArtifact() {
        this.id = UUID.randomUUID().toString();
    }

    public KnowledgeArtifact(ArtifactType type, String externalId, String title, String content, String source) {
        this();
        this.type = type;
        this.externalId = externalId;
        this.title = title;
        this.content = content;
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArtifactType getType() {
        return type;
    }

    public void setType(ArtifactType type) {
        this.type = type;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<String> getRelatedIds() {
        return relatedIds;
    }

    public void setRelatedIds(List<String> relatedIds) {
        this.relatedIds = relatedIds != null ? relatedIds : new ArrayList<>();
    }
}
