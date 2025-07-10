package gg.nextforge.updater;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides utilities for checking for updates and downloading the latest JAR
 * from GitHub releases. Supports „Dev releases“ (Prereleases mit '-SNAPSHOT').
 */
public class CoreAutoUpdater {

    private static final Logger LOGGER = Logger.getLogger(CoreAutoUpdater.class.getName());
    private static final String OWNER = "NextForge-Development";
    private static final String REPO = "NextCore";
    private static final String RELEASE_API = "https://api.github.com/repos/%s/%s/releases/latest";
    private static final String LIST_RELEASES_API = "https://api.github.com/repos/%s/%s/releases";

    private final Gson gson = new Gson();
    private final File downloadDir;
    private final String currentVersion;

    /**
     * Creates an updater using the version specified in {@code plugin.yml}
     * shipped within this JAR.
     *
     * @param downloadDir the directory to download new JARs to
     */
    public CoreAutoUpdater(File downloadDir) {
        this.currentVersion = readVersionFromPlugin();
        this.downloadDir = downloadDir;
    }

    /**
     * Creates an updater with a provided current version.
     *
     * @param currentVersion the current version string
     * @param downloadDir    the directory to download new JARs to
     */
    public CoreAutoUpdater(String currentVersion, File downloadDir) {
        this.currentVersion = currentVersion;
        this.downloadDir = downloadDir;
    }

    private String readVersionFromPlugin() {
        Pattern p = Pattern.compile("^version:\\s*(.+)$");
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            if (in == null) {
                LOGGER.warning("plugin.yml not found in classpath");
                return "";
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher m = p.matcher(line.trim());
                    if (m.find()) {
                        return m.group(1).trim();
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read plugin.yml", e);
        }
        return "";
    }

    private HttpURLConnection openConnection(String url) throws IOException {
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        String token = System.getenv("GITHUB_TOKEN");
        if (token != null && !token.isBlank()) {
            conn.setRequestProperty("Authorization", "token " + token);
        }
        return conn;
    }

    /**
     * Holt das neueste „stable“ Release (non-prerelease).
     */
    private JsonObject getLatestRelease() throws IOException {
        HttpURLConnection conn = openConnection(String.format(RELEASE_API, OWNER, REPO));
        try (InputStream in = conn.getInputStream();
             InputStreamReader reader = new InputStreamReader(in)) {
            return gson.fromJson(reader, JsonObject.class);
        }
    }

    /**
     * Holt das neueste „Dev“-Release, also das neueste Prerelease mit '-SNAPSHOT'.
     * Fällt zurück auf das neueste stabile Release, falls kein Dev-Release gefunden wird.
     */
    private JsonObject getLatestDevRelease() throws IOException {
        HttpURLConnection conn = openConnection(String.format(LIST_RELEASES_API, OWNER, REPO));
        try (InputStream in = conn.getInputStream();
             InputStreamReader reader = new InputStreamReader(in)) {
            JsonArray releases = gson.fromJson(reader, JsonArray.class);
            JsonObject latestDev = null;
            String latestDate = null;
            for (JsonElement el : releases) {
                JsonObject rel = el.getAsJsonObject();
                boolean prerelease = rel.get("prerelease").getAsBoolean();
                String tagName = rel.get("tag_name").getAsString();
                if (prerelease && tagName.endsWith("-SNAPSHOT")) {
                    String createdAt = rel.get("created_at").getAsString();
                    if (latestDev == null || createdAt.compareTo(latestDate) > 0) {
                        latestDev = rel;
                        latestDate = createdAt;
                    }
                }
            }
            if (latestDev != null) {
                return latestDev;
            }
        }
        // Fallback
        return getLatestRelease();
    }

    /**
     * Fetches the tag name of the latest release on GitHub.
     * Bei '-SNAPSHOT' im currentVersion: Dev-Release, sonst Stable.
     *
     * @return the latest release tag
     * @throws IOException if the request fails
     */
    public String fetchLatestVersion() throws IOException {
        JsonObject release;
        if (currentVersion != null && currentVersion.endsWith("-SNAPSHOT")) {
            release = getLatestDevRelease();
        } else {
            release = getLatestRelease();
        }
        return release.get("tag_name").getAsString();
    }

    /**
     * Determines whether the current version matches the latest release.
     * Ignores build metadata (e.g., +build123).
     *
     * @return true if the current version is up-to-date
     * @throws IOException if the GitHub request fails
     */
    public boolean isLatestVersion() throws IOException {
        String latest = fetchLatestVersion();
        return normalizeVersion(currentVersion).equalsIgnoreCase(normalizeVersion(latest));
    }

    /**
     * Downloads the latest release JAR if a newer version is available.
     * Bei '-SNAPSHOT' im currentVersion oder dev==true: Dev-Release, sonst Stable.
     *
     * @return the downloaded file, or null if no download occurred
     * @throws IOException if downloading fails
     */
    public CompletableFuture<File> downloadLatestJar(boolean dev) throws IOException {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject release;
            try {
                if ((currentVersion != null && currentVersion.endsWith("-SNAPSHOT")) || dev) {
                    release = getLatestDevRelease();
                } else {
                    release = getLatestRelease();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String latest = release.get("tag_name").getAsString();
            if (normalizeVersion(latest).equalsIgnoreCase(normalizeVersion(currentVersion))) {
                LOGGER.info("Already on the latest version: " + latest);
                return null;
            }

            JsonArray assets = release.getAsJsonArray("assets");
            for (JsonElement el : assets) {
                JsonObject asset = el.getAsJsonObject();
                String name = asset.get("name").getAsString();
                if (name.endsWith(".jar")) {
                    String url = asset.get("browser_download_url").getAsString();
                    try {
                        return downloadFile(url, name);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            LOGGER.warning("No JAR asset found in the latest release");
            return null;
        });
    }

    /**
     * Normalizes a version string by stripping build metadata (e.g., +build123).
     */
    private String normalizeVersion(String version) {
        if (version == null) return "";
        int plus = version.indexOf('+');
        if (plus >= 0) {
            return version.substring(0, plus);
        }
        return version;
    }

    private File downloadFile(String url, String name) throws IOException {
        if (!downloadDir.exists()) {
            Files.createDirectories(downloadDir.toPath());
        }
        File out = new File(downloadDir, name);
        LOGGER.info("Downloading " + url + " to " + out.getAbsolutePath());
        HttpURLConnection conn = openConnection(url);
        try (InputStream in = conn.getInputStream();
             FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        }
        return out;
    }
}
