# NextForge Core

A modular core framework for **NextForge** projects, combining:

- **Data Package** – annotation-driven, pluggable persistence with multiple backends.
- **Plugin Framework** – annotation-based plugin bootstrap system for NextForge.
- **Dependency Loader** – runtime dependency resolution and downloading.

---

## Features

### 📦 Data Package
- **Backends:** MySQL, H2, MongoDB, JSON file storage.
- **Annotation-driven mapping:** `@DataClass`, `@PrimaryKey`, `@Index`, `@Unique`, `@Transient`.
- **Unified Storage API** – same CRUD methods across all backends.
- **Schema management:** automatic table creation, schema diffs, index enforcement.
- **Auto UUID generation** for entities without IDs on insert.
- **Transactions** (JDBC), optional transactions in MongoDB.
- **Parallel I/O** for high-throughput batch operations.
- **Atomic JSON writes** for file-based persistence.

Full details: [📄 Data Package Documentation](.github/docs/Data-Package.md)

---

### 🔌 Plugin Framework
- `@NextForgePlugin` annotation for plugin entrypoints.
- `ForgedPlugin` base class for easy lifecycle management.
- Integrated dependency resolution via `DependencyLoader` & `DependencyResolver`.
- Supports **runtime dependency downloading** from Maven repositories.

---

### 📥 Dependency Loader
- Declarative dependency list via `DependencyArtifact` and `DependencyRepository`.
- Downloads JARs at runtime and loads them into the classpath.
- Maven-style repository layout supported.
- Extensible for authentication or alternative storage backends.

---

## Project Structure

```
src/main/java/gg/nextforge/core
├── data                # Data Package (annotations, model, storage, utils)
│   ├── annotations     # @DataClass, @PrimaryKey, @Index, @Unique, ...
│   ├── model           # BaseEntity
│   ├── storage         # Storage API + backends: JDBC, MySQL, H2, MongoDB, JSON
│   └── util            # Reflection, UUID, Index utilities
├── plugin              # Plugin base and annotation
│   └── dependency      # Dependency loader and resolver
└── NextCore.java       # Entry point
```

---

## Getting Started

### Requirements
- Java 17+
- Gradle 8.x

### Build
```bash
./gradlew build
```

### Run Tests
```bash
./gradlew test
```

---

## Example: Creating a Persisted Entity

```java
@DataClass(table = "users", collection = "users", file = "users")
@Unique(columns = {"email"})
public class User extends BaseEntity {

    @PrimaryKey(mongoId = true)
    private UUID uniqueId;

    @Index
    private String username;

    @Unique
    private String email;

    private int age;

    public User() {}
}
```

---

## Example: Using MySQL Storage

```java
var storage = new MySQLStorage<>(
    User.class,
    "jdbc:mysql://localhost:3306/appdb",
    "root",
    "password"
);
storage.init();
-
var user = new User();
user.setUniqueId(UUID.randomUUID());
user.setUsername("soldier");
user.setEmail("soldier@example.com");
user.setAge(25);

storage.save(user);
```

---

## Documentation

For a detailed guide to the Data Package, including all annotations, backends, schema generation, and index handling, see:  
📄 [Data Package Documentation](.github/docs/Data-Package.md)

For the Dependency Injection Framework, including how to manage dependencies, and use them, see:
📄 [Dependency Injection Documentation](.github/docs/Dependency-Injection.md)

---

## License

MIT – see [LICNESE](LICENSE) for details.
