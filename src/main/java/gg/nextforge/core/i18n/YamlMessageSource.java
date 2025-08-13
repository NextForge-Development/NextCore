// gg/nextforge/core/i18n/YamlMessageSource.java
package gg.nextforge.core.i18n;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class YamlMessageSource {
    private final Path messagesDir;               // <plugin-data>/messages
    private final Locale defaultLocale;
    private final Set<Locale> supported;
    private final Map<Locale, Map<String, String>> bundles = new HashMap<>();

    public YamlMessageSource(Path dataFolder, Locale defaultLocale, Set<Locale> supported) {
        this.messagesDir = dataFolder.resolve("messages");
        this.defaultLocale = defaultLocale == null ? Locale.ENGLISH : defaultLocale;
        this.supported = Set.copyOf(supported);
    }

    /** Erststart: legt de.yml/en.yml an, wenn nicht vorhanden (aus Jar-Defaults) */
    public void ensureDefaults(ClassLoader pluginLoader) throws IOException {
        Files.createDirectories(messagesDir);
        for (Locale loc : supported) {
            String fname = fileName(loc);
            Path target = messagesDir.resolve(fname);
            if (!Files.exists(target)) {
                // Default aus Ressourcen kopieren: /messages/de.yml im Plugin-Jar
                String cp = "messages/" + fname;
                try (InputStream in = pluginLoader.getResourceAsStream(cp)) {
                    if (in != null) {
                        Files.copy(in, target);
                    } else {
                        // minimaler Fallback
                        Files.writeString(target, "# " + fname + "\n", StandardCharsets.UTF_8);
                    }
                }
            }
        }
    }

    public void loadAll() throws IOException {
        bundles.clear();
        for (Locale loc : supported) {
            bundles.put(loc, loadOne(loc));
        }
    }

    public void reload() throws IOException {
        loadAll();
    }

    public Optional<String> getRaw(Locale locale, String key) {
        Map<String, String> map = bundles.getOrDefault(locale, Map.of());
        if (map.containsKey(key)) return Optional.ofNullable(map.get(key));
        // Fallback: Default-Locale
        Map<String, String> def = bundles.getOrDefault(defaultLocale, Map.of());
        return Optional.ofNullable(def.get(key));
    }

    public Locale getDefaultLocale() { return defaultLocale; }

    private Map<String, String> loadOne(Locale loc) throws IOException {
        Path file = messagesDir.resolve(fileName(loc));
        if (!Files.exists(file)) return Map.of();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object data = new Yaml().load(r);
            if (!(data instanceof Map)) return Map.of();
            // Flach erwarten: key: value
            Map<?, ?> m = (Map<?, ?>) data;
            Map<String, String> out = new LinkedHashMap<>();
            for (var e : m.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    out.put(e.getKey().toString(), e.getValue().toString());
                }
            }
            return out;
        }
    }

    private static String fileName(Locale loc) {
        // "de", "en" – ohne Region
        String lang = loc.getLanguage();
        return (lang == null || lang.isBlank() ? "en" : lang) + ".yml";
    }
}
