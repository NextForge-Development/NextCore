package gg.nextforge.core.plugin.dependency.model;

public record DependencyRepository(String url, String username, String password) {
    public DependencyRepository(String url) {
        this(url, null, null);
    }

}
