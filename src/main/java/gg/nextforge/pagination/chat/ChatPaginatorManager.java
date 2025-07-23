package gg.nextforge.pagination.chat;

import gg.nextforge.pagination.session.PaginationSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chat pagination sessions for players.
 * This class provides methods to set, get, remove, and check the existence of pagination sessions.
 */
public class ChatPaginatorManager {

    // A thread-safe map to store pagination sessions keyed by player UUID.
    private static final Map<UUID, PaginationSession<String>> sessions = new ConcurrentHashMap<>();

    /**
     * Sets a pagination session for a player.
     *
     * @param playerId The UUID of the player.
     * @param session The pagination session to set for the player.
     */
    public static void setSession(UUID playerId, PaginationSession<String> session) {
        sessions.put(playerId, session);
    }

    /**
     * Retrieves the pagination session for a player.
     *
     * @param playerId The UUID of the player.
     * @return The pagination session for the player, or null if no session exists.
     */
    public static PaginationSession<String> getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    /**
     * Removes the pagination session for a player.
     *
     * @param playerId The UUID of the player.
     */
    public static void removeSession(UUID playerId) {
        sessions.remove(playerId);
    }

    /**
     * Checks if a pagination session exists for a player.
     *
     * @param playerId The UUID of the player.
     * @return true if a session exists for the player, false otherwise.
     */
    public static boolean hasSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }
}
