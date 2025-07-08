package gg.nextforge.protocol;

import gg.nextforge.protocol.packet.PacketType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiter for packets.
 *
 * Prevents script kiddies with their mom's credit card
 * hacked clients from sending 10,000 packets per second
 * and turning your server into a slideshow.
 *
 * "bUt I nEeD 500 cLiCkS pEr SeCoNd" - No you fucking don't,
 * you're not Technoblade (RIP), you're just lagging everyone.
 *
 * Uses a sliding window because I'm too lazy to implement
 * token bucket and this works good enough.
 */
public class RateLimiter {

    // Player -> PacketType -> RateLimit
    private final Map<UUID, Map<PacketType, RateLimit>> limits = new ConcurrentHashMap<>();

    // Default limits per packet type (per second)
    private final Map<PacketType, Integer> defaultLimits = new ConcurrentHashMap<>();

    public RateLimiter() {
        // Set some sensible defaults
        setDefaultLimit(PacketType.PLAY_CLIENT_CHAT, 3); // 3 messages per second max
        setDefaultLimit(PacketType.PLAY_CLIENT_ARM_ANIMATION, 20); // 20 swings per second
        setDefaultLimit(PacketType.PLAY_CLIENT_POSITION, 50); // Movement packets
        setDefaultLimit(PacketType.PLAY_CLIENT_POSITION_LOOK, 50);
        setDefaultLimit(PacketType.PLAY_CLIENT_LOOK, 50);
        setDefaultLimit(PacketType.PLAY_CLIENT_FLYING, 50);
        setDefaultLimit(PacketType.PLAY_CLIENT_BLOCK_DIG, 30); // Block breaking
        setDefaultLimit(PacketType.PLAY_CLIENT_BLOCK_PLACE, 30); // Block placing
    }

    /**
     * Set default limit for a packet type.
     * 0 = unlimited (no rate limiting)
     */
    public void setDefaultLimit(PacketType type, int perSecond) {
        if (perSecond > 0) {
            defaultLimits.put(type, perSecond);
        } else {
            defaultLimits.remove(type);
        }
    }

    /**
     * Check if a packet should be rate limited.
     *
     * @return true if packet should be dropped, false if allowed
     */
    public boolean shouldLimit(UUID player, PacketType type) {
        Integer limit = defaultLimits.get(type);
        if (limit == null || limit == 0) {
            return false; // No limit for this type
        }

        // Get or create rate limit for this player/type combo
        RateLimit rateLimit = limits
                .computeIfAbsent(player, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(type, k -> new RateLimit(limit));

        return !rateLimit.tryAcquire();
    }

    /**
     * Clear limits for a player.
     * Call when they disconnect.
     */
    public void clearPlayer(UUID player) {
        limits.remove(player);
    }

    /**
     * Simple rate limit implementation.
     * Tracks packet count in a sliding window.
     */
    private static class RateLimit {
        private final int maxPerSecond;
        private final long windowNanos = TimeUnit.SECONDS.toNanos(1);
        private long windowStart = System.nanoTime();
        private int count = 0;

        RateLimit(int maxPerSecond) {
            this.maxPerSecond = maxPerSecond;
        }

        /**
         * Try to acquire a permit to send a packet.
         *
         * @return true if allowed, false if rate limited
         */
        synchronized boolean tryAcquire() {
            long now = System.nanoTime();
            long elapsed = now - windowStart;

            // Reset window if it's expired
            if (elapsed > windowNanos) {
                windowStart = now;
                count = 1;
                return true;
            }

            // Check if we're over the limit
            if (count >= maxPerSecond) {
                return false; // Rate limited, eat shit
            }

            count++;
            return true;
        }
    }
}