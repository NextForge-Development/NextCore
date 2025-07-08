package gg.nextforge.version;

import org.bukkit.Bukkit;

/**
 * Enum representing different Minecraft versions and their associated version strings.
 * Provides utility methods to determine the current server version, compare versions,
 * and retrieve the corresponding NMS (net.minecraft.server) package name.
 */
public enum MinecraftVersion {
    // Enum constants representing specific Minecraft versions
    v1_19_R1("1.19", "1.19.1", "1.19.2"),
    v1_19_R2("1.19.3"),
    v1_19_R3("1.19.4"),

    v1_20_R1("1.20", "1.20.1"),
    v1_20_R2("1.20.2"),
    v1_20_R3("1.20.3", "1.20.4"),

    v1_21_R1("1.21", "1.21.1"),
    v1_21_R2("1.21.2", "1.21.3"),
    v1_21_R3("1.21.4", "1.21.5", "1.21.6", "1.21.7"), //Might be wrong, check later

    UNKNOWN; // Represents an unsupported or unknown Minecraft version

    private final String[] versionStrings; // Array of version strings associated with the enum constant
    private static MinecraftVersion current; // Cached current Minecraft version

    /**
     * Constructor for MinecraftVersion enum.
     *
     * @param versions The version strings associated with this Minecraft version.
     */
    MinecraftVersion(String... versions) {
        this.versionStrings = versions;
    }

    /**
     * Retrieves the current Minecraft version based on the server's version string.
     *
     * @return The current MinecraftVersion enum constant.
     */
    public static MinecraftVersion getCurrent() {
        if (current != null) return current;

        // Get the actual server version string
        String version = Bukkit.getVersion();
        String mcVersion = version.substring(version.indexOf("MC: ") + 4, version.length() - 1);

        // Try to match it to our known versions
        for (MinecraftVersion v : values()) {
            if (v == UNKNOWN) continue;

            for (String s : v.versionStrings) {
                if (mcVersion.equals(s)) {
                    current = v;
                    return current;
                }
            }
        }

        // If no match is found, set the version to UNKNOWN
        current = UNKNOWN;
        return current;
    }

    /**
     * Checks if the current version is at least the specified version.
     *
     * @param version The MinecraftVersion to compare against.
     * @return True if the current version is greater than or equal to the specified version.
     */
    public boolean isAtLeast(MinecraftVersion version) {
        return this.ordinal() >= version.ordinal();
    }

    /**
     * Retrieves the NMS (net.minecraft.server) package name for the current version.
     *
     * @return The NMS package name as a string.
     * @throws UnsupportedOperationException If the version is UNKNOWN.
     */
    public String getNMSPackage() {
        if (this == UNKNOWN) {
            throw new UnsupportedOperationException(
                    "Can't get NMS package for unknown version you absolute donkey"
            );
        }
        return "net.minecraft.server." + this.name();
    }

    /**
     * Determines if the current version uses Mojang mappings.
     *
     * @return True, as Mojang mappings are assumed to be used for all versions.
     */
    public boolean usesMojangMappings() {
        return true;
    }
}