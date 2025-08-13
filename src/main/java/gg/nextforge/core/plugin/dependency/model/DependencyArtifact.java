package gg.nextforge.core.plugin.dependency.model;

public record DependencyArtifact(String groupId, String artifactId, String version) {

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
