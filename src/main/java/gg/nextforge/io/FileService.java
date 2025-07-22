package gg.nextforge.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileService {

    private static final Logger LOGGER = Logger.getLogger("FileService");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean ensureDirectoryExists(Path dir) {
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
                return true;
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to create directory: " + dir, e);
            }
        }
        return false;
    }

    public static boolean writeJson(Path file, Object data) {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write JSON: " + file, e);
            return false;
        }
    }

    public static <T> Optional<T> readJson(Path file, Class<T> type) {
        if (!Files.exists(file)) return Optional.empty();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return Optional.ofNullable(GSON.fromJson(reader, type));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read JSON: " + file, e);
            return Optional.empty();
        }
    }

    public static boolean copyFile(Path source, Path target, boolean overwrite) {
        try {
            if (overwrite || !Files.exists(target)) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to copy file: " + source + " -> " + target, e);
        }
        return false;
    }

    public static boolean deleteFile(Path file) {
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to delete file: " + file, e);
            return false;
        }
    }
}