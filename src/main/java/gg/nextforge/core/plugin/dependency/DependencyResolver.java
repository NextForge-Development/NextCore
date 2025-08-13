package gg.nextforge.core.plugin.dependency;

import gg.nextforge.core.plugin.dependency.model.DependencyArtifact;
import gg.nextforge.core.plugin.dependency.model.DependencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class DependencyResolver {

    private static final Logger log = LoggerFactory.getLogger(DependencyResolver.class);
    List<DependencyRepository> repositories;
    List<DependencyArtifact> artifacts;

    public static DependencyResolver create() {;
        return new DependencyResolver();
    }

    private DependencyResolver() {
        this.repositories = new ArrayList<>();
        this.artifacts = new ArrayList<>();
    }

    public DependencyResolver addRepository(DependencyRepository repository) {
        this.repositories.add(repository);
        return this;
    }

    public DependencyResolver addArtifact(DependencyArtifact artifact) {
        this.artifacts.add(artifact);
        return this;
    }

    public void downloadDependencies(Path libraryDirectory) {
        for (DependencyArtifact artifact : artifacts) {
            boolean found = false;
            log.info("Downloading artifact: {}", artifact);
            for (DependencyRepository repo : repositories) {
                try {
                    String artifactPath = artifact.groupId() + "/" + artifact.artifactId() + "/" + artifact.version() + "/" + artifact.artifactId() + "-" + artifact.version() + ".jar"; // z.B. groupId/artifactId/version/artifactId-version.jar
                    URL url = new URL(repo.url() + "/" + artifactPath);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    if (conn.getResponseCode() == 200) {
                        String fileName = artifact.artifactId() + "-" + artifact.version() + ".jar";
                        Path target = libraryDirectory.resolve(fileName);
                        Files.createDirectories(target.getParent());
                        try (InputStream in = conn.getInputStream()) {
                            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                        found = true;
                        log.info("Downloaded artifact {} from repository {}", artifact, repo.url());
                        break;
                    } else {
                        log.debug("Artifact {} not found in repository {}: HTTP {}", artifact, repo.url(), conn.getResponseCode());
                        log.debug("Are you sure the artifact exists in this repository? Check the URL and artifact details aswell as credentials, if required.");
                    }
                } catch (IOException e) {
                    log.debug("Error whilst downloading artifact {} from repository {}: {}", artifact, repo.url(), e.getMessage());
                    // Continue to the next repository if this one fails
                } catch (Exception e) {
                    log.debug("Unexpected error while downloading artifact {} from repository {}: {}", artifact, repo.url(), e.getMessage());
                    // Continue to the next repository if this one fails
                }
            }
            if (!found) {
                log.error("Could not find artifact {} in any of the following repositories:", artifact);
                for (DependencyRepository repo : repositories) {
                    log.error(" - {}", repo.url());
                }
                log.error("Make sure the artifact exists in one of the repositories or add a new repository with the correct URL.");
            }
        }
    }

}
