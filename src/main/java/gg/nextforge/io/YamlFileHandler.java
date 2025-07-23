package gg.nextforge.io;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YamlFileHandler {

    private static final Logger LOGGER = Logger.getLogger("YamlFileHandler");

    private static final DumperOptions OPTIONS = new DumperOptions();
    private static final Yaml YAML;

    static {
        OPTIONS.setIndent(2);
        OPTIONS.setPrettyFlow(true);
        OPTIONS.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        YAML = new Yaml(OPTIONS);
    }

    public static <T> Optional<T> load(Path file, Class<T> type) {
        if (!Files.exists(file)) return Optional.empty();

        try (InputStream input = Files.newInputStream(file)) {
            Yaml yaml = new Yaml(new Constructor(type, new LoaderOptions()));
            T obj = yaml.load(input);
            return Optional.ofNullable(obj);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load YAML: " + file, e);
            return Optional.empty();
        }
    }

    public static boolean save(Path file, Object data) {
        try (Writer writer = Files.newBufferedWriter(file)) {
            YAML.dump(data, writer);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save YAML: " + file, e);
            return false;
        }
    }
}