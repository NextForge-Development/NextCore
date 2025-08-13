# Data Package – Architecture, Usage & Examples

> A lightweight persistence layer with annotation-driven mapping and pluggable backends (MySQL, H2, MongoDB, JSON).  
> Goals: **simple**, **predictable**, **no heavy ORM**, **runtime-friendly**.

---

## Table of Contents

1. [Overview](#overview)  
2. [Quick Start](#quick-start)  
3. [Gradle Setup](#gradle-setup)  
4. [Package Structure](#package-structure)  
5. [Core Annotations](#core-annotations)  
6. [Entities](#entities)  
7. [Reflection Utilities](#reflection-utilities)  
8. [Storage Interface](#storage-interface)  
9. [Backends](#backends)  
   - [JDBC (MySQL/H2)](#jdbc-mysqlh2)  
   - [MongoDB](#mongodb)  
   - [JSON (File-Based)](#json-file-based)  
10. [Schema Management](#schema-management)  
    - [DDL Generator](#ddl-generator)  
    - [Schema Inspector](#schema-inspector)  
    - [Index / Unique](#index--unique)  
11. [Transactions & Parallel IO](#transactions--parallel-io)  
12. [Error Handling & Logging](#error-handling--logging)  
13. [Extensibility](#extensibility)  
14. [FAQ](#faq)  
15. [Examples](#examples)  
16. [Glossary](#glossary)

---

## Overview

This package provides a minimal, annotation-driven persistence layer:

- **Annotations** describe persistence metadata (`@DataClass`, `@PrimaryKey`, `@Transient`, `@Index`, `@Unique`).
- **Reflection** resolves table/collection names, JSON file names, and the primary key.
- **Storage interface** defines a unified CRUD API across backends.
- **Backends**:  
  - JDBC (MySQL, H2)  
  - MongoDB  
  - JSON (file-based, single file per entity type)
- **Schema tools** for JDBC: automatic table creation, schema diffs, and index enforcement.
- **Operational features**: Auto-UUID, transactions, and parallel IO.

The design deliberately favors **predictability** and **low magic**. Field names map directly to columns/keys unless customized via annotations.

---

## Quick Start

1. Add dependencies (see [Gradle Setup](#gradle-setup)).  
2. Create an entity:
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

       public User() {} // required
       // getters/setters or Lombok
   }
   ```
3. Pick a backend and use it:
   ```java
   // JSON
   var json = new JSONStorage<>(User.class, Path.of("data"));
   json.init();
   var u = new User();
   u.setUniqueId(UUID.randomUUID());
   u.setUsername("neo");
   u.setEmail("neo@matrix.io");
   u.setAge(29);
   json.save(u);
   ```

---

## Gradle Setup

```kotlin
plugins { java }
repositories { mavenCentral() }

dependencies {
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")

    // JDBC Backends
    implementation("mysql:mysql-connector-j:8.4.0")
    implementation("com.h2database:h2:2.2.224")

    // MongoDB
    implementation("org.mongodb:mongodb-driver-sync:5.1.0")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")

    // Lombok (optional)
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}
```

---

## Package Structure

```
gg.nextforge.core.data
├── annotation
│   ├── DataClass.java
│   ├── PrimaryKey.java
│   ├── Transient.java
│   ├── Index.java / Indexes.java
│   └── Unique.java / Uniques.java
├── model
│   └── BaseEntity.java
├── storage
│   ├── Storage.java
│   ├── jdbc
│   │   ├── JdbcStorage.java
│   │   ├── SchemaGenerator.java
│   │   ├── SchemaInspector.java
│   │   └── IndexApplier.java
│   ├── mysql
│   │   ├── MySQLStorage.java
│   │   └── MySQLConfig.java (optional)
│   ├── h2
│   │   ├── H2Storage.java
│   │   └── H2Config.java (optional)
│   ├── mongodb
│   │   ├── MongoDBStorage.java
│   │   └── MongoDBConfig.java (optional)
│   └── json
│       ├── JSONStorage.java
│       └── JSONConfig.java (optional)
└── util
    ├── ReflectionUtil.java
    └── UUIDUtil.java
```

---

## Core Annotations

| Annotation     | Target  | Purpose                                                                                          |
|----------------|---------|--------------------------------------------------------------------------------------------------|
| `@DataClass`   | TYPE    | Declares an entity as persistable. Optional names for SQL table, Mongo collection, JSON file.   |
| `@PrimaryKey`  | FIELD   | Marks the primary key. `mongoId=true` maps to Mongo `_id`. `autoGenerate` hint for JDBC.        |
| `@Transient`   | FIELD   | Excludes field from persistence.                                                                 |
| `@Index`       | TYPE/FIELD | Declares a (non-unique by default) index on field(s).                                        |
| `@Unique`      | TYPE/FIELD | Declares a unique constraint (unique index) on field(s).                                     |

**Notes**
- Field names become column/keys unless overridden in storage-specific logic.  
- MongoDB will use `_id` if `@PrimaryKey(mongoId = true)`.

---

## Entities

- Must have a **no-args constructor**.
- Extend `BaseEntity` (optional but recommended): provides PK helpers, timestamps, and sane `equals/hashCode`.
- Use `UUID` for identifiers (common across all backends).

Example:
```java
@DataClass(table = "orders", collection = "orders", file = "orders")
@Index(columns = {"customerId", "createdAt"})
public class Order extends BaseEntity {
    @PrimaryKey(mongoId = true)
    private UUID id;

    private UUID customerId;
    private Instant createdAt;
    private String status;

    public Order() {}
}
```

---

## Reflection Utilities

- `ReflectionUtil.getPrimaryKeyField(Class<?>)` – find the `@PrimaryKey` field.
- `getTableName / getCollectionName / getJsonFileName` – resolved names with sensible fallbacks.
- Caches reflection results for performance.

---

## Storage Interface

```java
public interface Storage<T, ID> extends AutoCloseable {
    Class<T> entityType();

    default void init() throws Exception {}
    T insert(T entity) throws Exception;
    T update(T entity) throws Exception;
    default T upsert(T entity) throws Exception { ... }
    default T save(T entity) throws Exception { ... }
    default List<T> saveAll(Collection<T> entities) throws Exception { ... }
    Optional<T> findById(ID id) throws Exception;
    List<T> findAll(int limit, int offset) throws Exception;
    default List<T> findAll() throws Exception { ... }
    boolean deleteById(ID id) throws Exception;
    long count() throws Exception;
    boolean existsById(ID id) throws Exception;

    default List<T> saveAllParallel(Collection<T> entities, int threads) throws Exception { ... }
}
```

**Behavior**
- `save` is an upsert.  
- `saveAll` and `saveAllParallel` batch operations; choose parallel with care (see [Transactions & Parallel IO](#transactions--parallel-io)).

---

## Backends

### JDBC (MySQL/H2)

- Class: `JdbcStorage<T, ID>`; extended by `MySQLStorage` and `H2Storage`.
- Field ↔ column mapping by name.
- Auto-UUID on insert when PK field is `UUID` and value is null.
- `init()`:
  - Auto-create table via `SchemaGenerator.ensureTable`.
  - Ensure indexes via `IndexApplier.ensureIndexes`.
  - Dialect detection from `DatabaseMetaData#getDatabaseProductName`.

**Example**
```java
var h2 = new H2Storage<>(User.class,
    "jdbc:h2:./data/app;MODE=MySQL;DATABASE_TO_UPPER=false", "sa", "");
h2.init();
h2.save(newUser);
```

### MongoDB

- Class: `MongoDBStorage<T, ID>`.
- Uses `@PrimaryKey(mongoId=true)` → `_id`.
- Auto-UUID on insert if PK is `UUID` and null.
- `init()` ensures indexes (`createIndex`) idempotently; drops & recreates when definition mismatches.
- Optional transactions (ReplicaSet required).

**Example**
```java
var mongo = new MongoDBStorage<>(User.class, "mongodb://localhost:27017", "appdb", true);
mongo.init();
mongo.save(user);
```

### JSON (File-Based)

- Class: `JSONStorage<T, ID>`.
- Stores all entities of a type in a single `*.json` file.
- In-memory map + atomic writes (`.tmp` + `ATOMIC_MOVE`).
- Auto-UUID on insert if PK is `UUID` and null.

**Example**
```java
var json = new JSONStorage<>(User.class, Path.of("data"));
json.init();
json.saveAllParallel(List.of(u1, u2, u3), 4);
```

---

## Schema Management

### DDL Generator

- `SchemaGenerator.ensureTable(connection, type)` creates a minimal table if it does not exist.  
- Types map as follows (simplified):  
  `String → VARCHAR(255)`, `int → INT`, `long → BIGINT`, `boolean → BOOLEAN`,  
  `double → DOUBLE`, `float → REAL`, `BigDecimal → DECIMAL(38,10)`,  
  `Instant → TIMESTAMP`, `Enum → VARCHAR(64)`, `UUID → VARCHAR(36)`.

**Note:** The generator targets **simplicity**. For stricter constraints (lengths, defaults), extend it.

### Schema Inspector

- `SchemaInspector.diff(connection, type, dialect, dropUnknownColumns)` computes differences:
  - Missing columns → `ADD COLUMN`
  - Type/nullable changes → `MODIFY/ALTER COLUMN`
  - Optional removal of unknown columns → `DROP COLUMN`
- `SchemaInspector.apply(connection, diff)` applies changes in a transaction.

### Index / Unique

- Annotations: `@Index`, `@Unique` on TYPE or FIELD.
- Extracted by `IndexUtil.indexesFor(type)`.
- JDBC: `IndexApplier.ensureIndexes(connection, type, dialect)` reads metadata and creates missing indexes.
- MongoDB: compares name/keys/unique; drops and recreates when mismatched.

**Naming convention (deterministic)**:
- Non-unique: `idx_<table>_<col1>_<col2>...`  
- Unique: `uq_<table>_<col1>_<col2>...`

---

## Transactions & Parallel IO

- **JDBC**: `JdbcStorage` exposes `inTransaction(Function<Connection,R>)` for multi-operation atomicity.  
  Use for **batch imports** or **cross-entity** operations on the same connection.
- **MongoDB**: Transactions optional; require Replica Set or sharded cluster.  
  If unsupported, operations run non-transactionally.
- **Parallel IO**:
  - `saveAllParallel(entities, threads)` submits per-entity operations to a thread pool.
  - Prefer transactions + sequential for **strong consistency**; use parallel for **throughput** when acceptable.

**Rule of thumb**
- **Critical writes** (must all succeed/fail): single transaction, sequential.  
- **High-volume independent writes**: parallel.

---

## Error Handling & Logging

- Fail fast on misconfiguration (missing `@PrimaryKey`, null PK on insert).  
- Logs at INFO for normal lifecycle events, DEBUG for detailed IO, and ERROR for failures.
