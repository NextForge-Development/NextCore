package gg.nextforge.io;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileCache<T> {

    private static final Logger LOGGER = Logger.getLogger("FileCache");

    private final Map<Path, T> cache = new ConcurrentHashMap<>();
    private final Function<Path, T> loader;
    private final DirectoryWatcher watcher;

    public FileCache(Path directory, Function<Path, T> loader) throws IOException {
        this.loader = loader;
        this.watcher = new DirectoryWatcher(directory);

        // Automatically refresh cache on file change
        this.watcher.startWatching(event -> {
            Path path = directory.resolve(event.context());
            switch (event.kind().name()) {
                case "ENTRY_DELETE" -> cache.remove(path);
                case "ENTRY_MODIFY", "ENTRY_CREATE" -> loadFile(path);
            }
        });
    }

    public T get(Path path) {
        return cache.computeIfAbsent(path, this::loadFile);
    }

    public boolean isCached(Path path) {
        return cache.containsKey(path);
    }

    public void clear() {
        cache.clear();
    }

    public void shutdown() {
        watcher.stopWatching();
    }

    private T loadFile(Path path) {
        try {
            T result = loader.apply(path);
            if (result != null) {
                cache.put(path, result);
                LOGGER.info("FileCache: Loaded " + path);
            }
            return result;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load file into cache: " + path, e);
            return null;
        }
    }
}
