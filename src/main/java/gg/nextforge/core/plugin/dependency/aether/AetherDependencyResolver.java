package gg.nextforge.core.plugin.dependency.aether;

import gg.nextforge.core.plugin.dependency.model.DependencyArtifact;
import gg.nextforge.core.plugin.dependency.model.DependencyRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.*;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.NoTransporterException;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class AetherDependencyResolver {

    private static final Logger log = LoggerFactory.getLogger(AetherDependencyResolver.class);

    private final RepositorySystem system;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> remotes;              // für Aether
    private final List<DependencyRepository> remotesAuth;      // für Checksums (Auth)
    private final ExecutorService pool;

    /** Ergebnis mit GAV + Datei (für spätere Checksums etc.) */
    public record ResolvedJar(String groupId, String artifactId, String version, String extension, Path file) {
        public String jarFileName() { return artifactId + "-" + version + "." + extension; }
        public String mavenRelPath() {
            return groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + jarFileName();
        }
    }

    public AetherDependencyResolver(Path cacheDir, List<DependencyRepository> repositories, int threads) {
        this.system = newRepositorySystem();
        this.session = newSession(system, cacheDir);
        this.remotes = repositories.stream().map(this::toRemoteRepo).collect(Collectors.toList());
        this.remotesAuth = List.copyOf(repositories);
        this.pool = Executors.newFixedThreadPool(Math.max(2, Math.min(threads, 16)));
    }

    public void shutdown() { pool.shutdown(); }

    /** Transitiv auflösen (COMPILE/RUNTIME), Dateien im lokalen Cache, Ergebnis inkl. GAV zurückgeben. */
    public List<ResolvedJar> resolveTransitiveJars(Collection<DependencyArtifact> gavs) throws Exception {
        List<ArtifactResult> all = new ArrayList<>();

        for (DependencyArtifact a : gavs) {
            Artifact rootA = new DefaultArtifact(a.groupId() + ":" + a.artifactId() + ":" + a.version());
            Dependency root = new Dependency(rootA, JavaScopes.COMPILE);

            CollectRequest collect = new CollectRequest();
            collect.setRoot(root);
            for (RemoteRepository rr : remotes) collect.addRepository(rr);

            DependencyRequest depReq = new DependencyRequest(
                    collect,
                    DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE, JavaScopes.RUNTIME)
            );

            DependencyResult depResult = system.resolveDependencies(session, depReq);
            for (ArtifactResult ar : depResult.getArtifactResults()) {
                if (ar.isResolved() && ar.getArtifact() != null) all.add(ar);
            }
        }

        // Dedupe nur auf JARs
        Map<String, ResolvedJar> unique = new LinkedHashMap<>();
        for (ArtifactResult r : all) {
            Artifact a = r.getArtifact();
            if (a == null) continue;
            if (!"jar".equalsIgnoreCase(a.getExtension())) continue;
            String key = a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion();
            unique.put(key, new ResolvedJar(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getExtension(), a.getFile().toPath()));
        }

        List<ResolvedJar> out = new ArrayList<>(unique.values());
        log.info("Resolved {} jar(s) (transitive included)", out.size());
        return out;
    }

    /** Optionale Checksum-Verifikation (SHA-1/MD5) – parallel. */
    public void verifyChecksums(Collection<ResolvedJar> jars, Duration timeoutPerFile) throws InterruptedException {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (ResolvedJar jar : jars) tasks.add(() -> { verifyOne(jar, timeoutPerFile); return null; });
        for (Future<Void> f : pool.invokeAll(tasks)) {
            try { f.get(); } catch (ExecutionException e) {
                log.warn("Checksum verification failed: {}", e.getCause() != null ? e.getCause().getMessage() : e.toString());
            }
        }
    }

    /* ---------------- intern ---------------- */

    private void verifyOne(ResolvedJar jar, Duration timeout) {
        try {
            String sha1Local = sha1Hex(jar.file());
            String md5Local  = md5Hex(jar.file());

            String rel = jar.mavenRelPath();
            for (int i = 0; i < remotes.size(); i++) {
                RemoteRepository rr = remotes.get(i);
                DependencyRepository auth = remotesAuth.get(i);

                String base = rr.getUrl().endsWith("/") ? rr.getUrl() : rr.getUrl() + "/";
                URL sha1Url = new URL(base + rel + ".sha1");
                URL md5Url  = new URL(base + rel + ".md5");

                String remote = fetchText(sha1Url, auth, timeout);
                if (remote != null && remote.length() >= 10) {
                    if (remote.startsWith(sha1Local)) { log.debug("SHA1 ok for {}", jar.jarFileName()); return; }
                    else log.warn("SHA1 mismatch for {} (local={}, remote={})", jar.jarFileName(), sha1Local, remote);
                }
                remote = fetchText(md5Url, auth, timeout);
                if (remote != null && remote.length() >= 10) {
                    if (remote.startsWith(md5Local)) { log.debug("MD5 ok for {}", jar.jarFileName()); return; }
                    else log.warn("MD5 mismatch for {} (local={}, remote={})", jar.jarFileName(), md5Local, remote);
                }
            }
        } catch (Exception e) {
            log.debug("Checksum verify error for {}: {}", jar.jarFileName(), e.toString());
        }
    }

    private static String sha1Hex(Path p) throws Exception { try (InputStream in = Files.newInputStream(p)) { return DigestUtils.sha1Hex(in); } }
    private static String md5Hex(Path p)  throws Exception { try (InputStream in = Files.newInputStream(p))  { return DigestUtils.md5Hex(in); } }

    private String fetchText(URL url, DependencyRepository auth, Duration timeout) {
        try {
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout((int) timeout.toMillis());
            c.setReadTimeout((int) timeout.toMillis());
            if (auth.username() != null && auth.password() != null) {
                String basic = Base64.getEncoder().encodeToString((auth.username() + ":" + auth.password()).getBytes(StandardCharsets.UTF_8));
                c.setRequestProperty("Authorization", "Basic " + basic);
            }
            try (InputStream in = c.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ignore) {
            return null;
        }
    }

    /* ---------- Aether Bootstrap ---------- */

    private RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                if (!(exception instanceof NoTransporterException)) {
                    log.warn("Aether service creation failed: {} -> {}", type, impl, exception.toString());
                }
            }
        });
        return locator.getService(RepositorySystem.class);
    }

    private RepositorySystemSession newSession(RepositorySystem system, Path cacheDir) {
        DefaultRepositorySystemSession s = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(cacheDir.toFile());
        s.setLocalRepositoryManager(system.newLocalRepositoryManager(s, localRepo));

        // WICHTIG: Selektoren in die Session, nicht in den Request!
        s.setDependencySelector(new AndDependencySelector(
                new ScopeDependencySelector("test", "provided", "system"),
                new OptionalDependencySelector(),
                new ExclusionDependencySelector()
        ));

        s.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN);
        s.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_DAILY);
        return s;
    }

    private RemoteRepository toRemoteRepo(DependencyRepository r) {
        RemoteRepository.Builder b = new RemoteRepository.Builder(UUID.randomUUID().toString(), "default", r.url());
        if (r.username() != null && r.password() != null) {
            b.setAuthentication(new AuthenticationBuilder().addUsername(r.username()).addPassword(r.password()).build());
        }
        b.setReleasePolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN));
        b.setSnapshotPolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN));
        return b.build();
    }
}
