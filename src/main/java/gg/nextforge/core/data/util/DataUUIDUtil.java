// gg/nextforge/core/data/util/UUIDUtil.java
package gg.nextforge.core.data.util;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

public final class DataUUIDUtil {

    private DataUUIDUtil() {}

    /** Erzeugt eine neue zufällige UUID */
    public static UUID randomUUID() {
        return UUID.randomUUID();
    }

    /** Prüft, ob der String eine gültige UUID ist */
    public static boolean isValid(String uuid) {
        if (uuid == null) return false;
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /** Parst eine UUID oder wirft IllegalArgumentException */
    public static UUID parse(String uuid) {
        return UUID.fromString(uuid);
    }

    /** Konvertiert UUID zu Base64-String (kompakt für JSON, URLs) */
    public static String toBase64(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
    }

    /** Parst Base64-String zurück zu UUID */
    public static UUID fromBase64(String base64) {
        byte[] bytes = Base64.getUrlDecoder().decode(base64);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long mostSig = buffer.getLong();
        long leastSig = buffer.getLong();
        return new UUID(mostSig, leastSig);
    }

    /** Kürzt UUID für Logs/Debug auf 8 Zeichen */
    public static String shortId(UUID uuid) {
        return uuid.toString().split("-")[0];
    }
}
