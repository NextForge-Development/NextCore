package gg.nextforge.io;

import java.io.IOException;
import java.nio.file.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectoryWatcher {

    private static final Logger LOGGER = Logger.getLogger("DirectoryWatcher");

    private final Path directory;
    private final WatchService watchService;
    private Thread watcherThread;
    private boolean running = false;

    public DirectoryWatcher(Path directory) throws IOException {
        this.directory = directory;
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    public void startWatching(Consumer<WatchEvent<Path>> onEvent) {
        if (running) return;

        running = true;

        try {
            directory.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
            );
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to register directory watcher for: " + directory, e);
            return;
        }

        watcherThread = new Thread(() -> {
            while (running) {
                try {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.context() instanceof Path path) {
                            onEvent.accept((WatchEvent<Path>) event);
                        }
                    }
                    key.reset();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.warning("Directory watcher interrupted.");
                }
            }
        }, "DirectoryWatcher-" + directory.getFileName());

        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    public void stopWatching() {
        running = false;
        try {
            watchService.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to stop directory watcher.", e);
        }
    }
}
