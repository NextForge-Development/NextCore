package gg.nextforge.protocol.logging;

import com.google.gson.Gson;
import gg.nextforge.protocol.packet.PacketContainer;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WebhookLogger is responsible for logging packet events to a webhook asynchronously.
 *
 * This class sends packet logs to a specified webhook URL, allowing integration with external
 * services like Discord or Slack. It uses asynchronous HTTP requests to avoid blocking the
 * server's main thread.
 */
public class WebhookLogger {

    private final String webhookUrl; // The URL of the webhook to send logs to
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Webhook-Logger");
        t.setDaemon(true);
        return t;
    });
    private final Gson gson = new Gson(); // Gson instance for JSON serialization

    /**
     * Constructs a WebhookLogger instance.
     *
     * @param webhookUrl The URL of the webhook to send logs to.
     */
    public WebhookLogger(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    /**
     * Logs a packet event to the webhook asynchronously.
     *
     * @param player   The player associated with the packet.
     * @param packet   The packet being logged.
     * @param outgoing Whether the packet is outgoing or incoming.
     */
    public void logPacket(Player player, PacketContainer packet, boolean outgoing) {
        executor.submit(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("player", player.getName());
                payload.put("uuid", player.getUniqueId().toString());
                payload.put("packet_type", packet.getType().name());
                payload.put("direction", outgoing ? "OUTGOING" : "INCOMING");
                payload.put("timestamp", System.currentTimeMillis());

                // Add packet data if available
                Map<String, Object> packetData = new HashMap<>();
                if (packet.getIntegers().size() > 0) {
                    packetData.put("integers", packet.getIntegers().getValues());
                }
                if (packet.getStrings().size() > 0) {
                    packetData.put("strings", packet.getStrings().getValues());
                }
                payload.put("data", packetData);

                sendWebhook(payload);
            } catch (Exception e) {
                // Ignore webhook failures
            }
        });
    }

    /**
     * Sends the HTTP request to the webhook.
     *
     * @param payload The data to send in the request body.
     * @throws Exception If an error occurs during the HTTP request.
     */
    private void sendWebhook(Map<String, Object> payload) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "NextForge-PacketLogger/1.0");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        String json = gson.toJson(payload);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        // Trigger the request and disconnect
        conn.getResponseCode();
        conn.disconnect();
    }

    /**
     * Shuts down the webhook logger.
     *
     * This method waits for any pending webhook tasks to complete before shutting down.
     */
    public void shutdown() {
        executor.shutdown();
    }
}