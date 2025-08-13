# Dependency Loader – Injection & Runtime Resolution

This document describes the **runtime dependency system** used by NextForge Core: how to **declare**, **download**, and **inject** external JARs at plugin startup without requiring them on the compile-time classpath.

> Components: `DependencyArtifact`, `DependencyRepository`, `DependencyResolver`, `DependencyLoader`, and `ForgedPlugin` lifecycle integration.

---

## TL;DR

- Declare artifacts (GAV: `groupId:artifactId:version`) and repositories.
- On plugin enable, the resolver downloads missing JARs into your plugin data folder.
- The loader adds those JARs to a dedicated `URLClassLoader` so you can reflectively load classes.

### 📥 Dependency Loader – Highlights
- Declarative dependency list via `DependencyArtifact` and `DependencyRepository`.
- Downloads JARs at runtime and loads them into the classpath.
- Maven-style repository layout supported.
- Extensible for authentication or alternative storage backends.

---

## Architecture

```
+-----------------+        declares        +----------------------+
|  ForgedPlugin   |----------------------->|  DependencyResolver  |
|  (enable())     |                        |  (download JARs)     |
+--------+--------+                         +--------+------------+
         |                                            |
         | downloaded JAR paths                       |
         v                                            v
+-----------------+        inject URLs       +----------------------+
|  DependencyLoader|<----------------------- |   dependencies/      |
|  (URLClassLoader)|                         |  (plugin data dir)   |
+-----------------+                          +----------------------+
         |
         | loadClass("com.example.Foo")
         v
 application/runtime usage
```

---

## Core Types

### `DependencyArtifact`
```java
public record DependencyArtifact(String groupId, String artifactId, String version) {
    @Override public String toString() { return groupId + ":" + artifactId + ":" + version; }
}
```

### `DependencyRepository`
```java
public record DependencyRepository(String url, String username, String password) {
    public DependencyRepository(String url) { this(url, null, null); }
}
```
- `url`: base repository URL (e.g., `https://repo1.maven.org/maven2`).
- `username/password`: optional for private repos (currently not injected into HTTP calls in the snippet; see **Auth** below).

### `DependencyResolver`
Downloads declared artifacts into a target directory using Maven-like paths.

Key behavior (excerpt):
```java
String artifactPath = artifact.groupId() + "/" + artifact.artifactId() + "/" + artifact.version() +
                      "/" + artifact.artifactId() + "-" + artifact.version() + ".jar";
URL url = new URL(repo.url() + "/" + artifactPath);
```

> **Note**: Maven Central layout requires **groupId with slashes** (dots replaced by `/`). See [Path Mapping](#path-mapping).

### `DependencyLoader`
```java
public class DependencyLoader {
    private final URLClassLoader classLoader;
    public DependencyLoader(List<Path> jarFiles) {
        URL[] urls = jarFiles.stream().map(p -> p.toUri().toURL()).toArray(URL[]::new);
        classLoader = new URLClassLoader(urls, getClass().getClassLoader());
    }
    public Class<?> loadClass(String className) throws ClassNotFoundException { return classLoader.loadClass(className); }
}
```

### `ForgedPlugin` Integration
```java
public abstract class ForgedPlugin extends LicensedPlugin {
    @Override public void enablePlugin() {
        if (!this.getClass().isAnnotationPresent(NextForgePlugin.class))
            throw new IllegalStateException("Plugin must be annotated with @NextForgePlugin");

        Path libDir = this.getDataFolder().toPath().resolve("dependencies");
        DependencyResolver resolver = resolveDependencies(DependencyResolver.create());
        resolver.downloadDependencies(libDir);

        List<Path> jars = Files.list(libDir).toList();
        DependencyLoader loader = new DependencyLoader(jars); // keep reference if you need to load classes
        enable();
    }
    public abstract DependencyResolver resolveDependencies(DependencyResolver dr);
}
```

---

## Usage

### 1) Declare Repositories and Artifacts
```java
@NextForgePlugin
public final class MyPlugin extends ForgedPlugin {

    @Override public DependencyResolver resolveDependencies(DependencyResolver dr) {
        return dr
            .addRepository(new DependencyRepository("https://repo1.maven.org/maven2"))
            // Private repo (auth optional; see Security & Auth)
            .addRepository(new DependencyRepository("https://repo.mycompany.com/releases", "user", "token"))
            // Artifacts
            .addArtifact(new DependencyArtifact("org.slf4j", "slf4j-api", "2.0.13"))
            .addArtifact(new DependencyArtifact("com.fasterxml.jackson.core", "jackson-databind", "2.17.1"));
    }

    @Override public UUID pluginId() { return UUID.fromString("00000000-0000-0000-0000-000000000001"); }
    @Override public void enable() { /* use classes after download */ }
    @Override public void disable() { /* cleanup */ }
}
```

### 2) Load Classes at Runtime
```java
Path libDir = getDataFolder().toPath().resolve("dependencies");
List<Path> jars = Files.list(libDir).toList();
DependencyLoader loader = new DependencyLoader(jars);

Class<?> mapper = loader.loadClass("com.fasterxml.jackson.databind.ObjectMapper");
Object instance = mapper.getDeclaredConstructor().newInstance();
```

---

## Path Mapping

Maven repository layout expects slashes in the group path:

```
groupId: com.fasterxml.jackson.core
→ path:  com/fasterxml/jackson/core/jackson-databind/2.17.1/jackson-databind-2.17.1.jar
```

Your current resolver builds:
```java
artifact.groupId() + "/" + artifact.artifactId() + "/" + artifact.version() + "/..."
```
If `groupId` contains dots, convert them:
```java
String groupPath = artifact.groupId().replace('.', '/');
String artifactPath = groupPath + "/" + artifact.artifactId() + "/" + artifact.version() + "/" +
                      artifact.artifactId() + "-" + artifact.version() + ".jar";
```

> **Recommendation**: Update `DependencyResolver` accordingly to ensure compatibility with public Maven repos.

---

## Security & Auth

The `DependencyRepository` type supports `username/password`, but the HTTP request in `DependencyResolver` does **not** yet use them. To add basic auth:

```java
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
if (repo.username() != null && repo.password() != null) {
    String basic = Base64.getEncoder().encodeToString((repo.username() + ":" + repo.password()).getBytes(StandardCharsets.UTF_8));
    conn.setRequestProperty("Authorization", "Basic " + basic);
}
```

Suggested hardening:
- Set reasonable **timeouts**:
  ```java
  conn.setConnectTimeout(5000);
  conn.setReadTimeout(15000);
  ```
- Support **proxies** via JVM properties (`http.proxyHost`, `http.proxyPort`, etc.).
- Validate **checksums** (see **Integrity** below).

---

## Advanced Topics

### Parallel Downloads
Speed up large dependency sets:
```java
ExecutorService pool = Executors.newFixedThreadPool(Math.min(8, Runtime.getRuntime().availableProcessors()));
List<Future<Path>> tasks = artifacts.stream().map(a -> pool.submit(() -> downloadOne(a, repo, libDir))).toList();
for (Future<Path> f : tasks) f.get();
pool.shutdown();
```

### Integrity (SHA-1/MD5/SHA-256)
For Maven-compatible repos, you can fetch `<jar>.sha1` or `<jar>.md5` and verify after download.

### Caching
Skip downloads if the target JAR exists and passes checksum verification.

### Retry Policy
Use exponential backoff on transient HTTP errors (e.g., 429/5xx).

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| 404 Not Found | Group path not slash-converted | Use `groupId.replace('.', '/')` |
| 401/403 | Private repo requires auth | Provide credentials; set `Authorization` header |
| `ClassNotFoundException` | JAR not on loader classpath | Ensure file exists; re-create `DependencyLoader` after downloads |
| Timeout | Slow or blocked network | Increase timeouts; configure proxies |
| Corrupted JAR | Partial download | Enable checksum verification and retry |

---

## Best Practices

- Keep a dedicated `dependencies/` directory per plugin.
- Version-pin artifacts strictly (avoid `LATEST`).
- Use checksums to guarantee integrity.
- Log at `INFO` for success paths, `DEBUG` for HTTP URLs and retries (avoid logging secrets).
- Consider shading absolutely critical libs rather than downloading them at runtime.

---

## API Reference (Summary)

### `DependencyResolver`
- `addRepository(DependencyRepository repo)`
- `addArtifact(DependencyArtifact artifact)`
- `downloadDependencies(Path libraryDirectory)`

### `DependencyLoader`
- `DependencyLoader(List<Path> jarFiles)`
- `Class<?> loadClass(String className)`

### `DependencyArtifact`
- `groupId()`, `artifactId()`, `version()`

### `DependencyRepository`
- `url()`, `username()`, `password()`

---

## Example: Full Flow

```java
Path libDir = plugin.getDataFolder().toPath().resolve("dependencies");
DependencyResolver resolver = DependencyResolver.create()
        .addRepository(new DependencyRepository("https://repo1.maven.org/maven2"))
        .addArtifact(new DependencyArtifact("org.mongodb", "mongodb-driver-sync", "5.1.0"));

resolver.downloadDependencies(libDir);

List<Path> jars = Files.list(libDir).toList();
DependencyLoader loader = new DependencyLoader(jars);

Class<?> mongoClient = loader.loadClass("com.mongodb.client.MongoClients");
```

---

## Roadmap Ideas

- Credentials support for HTTP(S) downloads out of the box (Basic/Bearer).
- Pluggable `Transport` (HTTP client abstraction) to add retries and metrics.
- Support for **POM resolution** and **transitive dependencies**.
- Local cache directory with checksum index.
- Optional OSGi-style classloader isolation per dependency group.

---

*Generated for the NextForge Core project. Keep calm and inject dependencies at runtime.*
