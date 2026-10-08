package com.mrgn.knowledge.corpus;

import com.mrgn.knowledge.domain.ArtifactType;
import com.mrgn.knowledge.domain.KnowledgeArtifact;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class KnowledgeCorpus {

    private final Map<String, KnowledgeArtifact> artifacts = new ConcurrentHashMap<>();

    public KnowledgeArtifact save(KnowledgeArtifact artifact) {
        artifacts.put(artifact.getId(), artifact);
        return artifact;
    }

    public List<KnowledgeArtifact> saveAll(List<KnowledgeArtifact> incoming) {
        incoming.forEach(this::save);
        return incoming;
    }

    public List<KnowledgeArtifact> findAll() {
        return new ArrayList<>(artifacts.values());
    }

    public List<KnowledgeArtifact> findByType(ArtifactType type) {
        return artifacts.values().stream()
                .filter(a -> a.getType() == type)
                .collect(Collectors.toList());
    }

    public void clearType(ArtifactType type) {
        artifacts.values().removeIf(a -> a.getType() == type);
    }

    public void clear() {
        artifacts.clear();
    }

    public KnowledgeArtifact findByExternalId(String externalId) {
        if (externalId == null) {
            return null;
        }
        String needle = externalId.trim().toUpperCase(Locale.ROOT);
        return artifacts.values().stream()
                .filter(a -> a.getExternalId() != null && a.getExternalId().toUpperCase(Locale.ROOT).equals(needle))
                .findFirst()
                .orElse(null);
    }
}
