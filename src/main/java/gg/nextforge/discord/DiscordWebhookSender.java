package gg.nextforge.discord;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple Discord webhook sender with message builder support.
 */
public class DiscordWebhookSender {

    private static final Logger LOGGER = Logger.getLogger("DiscordWebhookSender");

    private final String webhookUrl;

    public DiscordWebhookSender(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void send(DiscordMessage message) {
        sendPayload(message.toJson());
    }

    private void sendPayload(String json) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_NO_CONTENT && responseCode != HttpURLConnection.HTTP_OK) {
                LOGGER.warning("Discord webhook failed with response code: " + responseCode);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send Discord webhook", e);
        }
    }

    // Builder class for messages
    public static class DiscordMessage {
        private String content;
        private final List<String> embeds = new ArrayList<>();

        public DiscordMessage setContent(String content) {
            this.content = content;
            return this;
        }

        public DiscordMessage addEmbed(String embedJson) {
            this.embeds.add(embedJson);
            return this;
        }

        public String toJson() {
            StringBuilder builder = new StringBuilder();
            builder.append("{");

            if (content != null) {
                builder.append("\"content\":\"").append(escape(content)).append("\"");
            }

            if (!embeds.isEmpty()) {
                if (content != null) builder.append(",");
                builder.append("\"embeds\":[");
                for (int i = 0; i < embeds.size(); i++) {
                    builder.append(embeds.get(i));
                    if (i < embeds.size() - 1) builder.append(",");
                }
                builder.append("]");
            }

            builder.append("}");
            return builder.toString();
        }

        private String escape(String input) {
            return input.replace("\"", "\\\"").replace("\n", "\\n");
        }
    }
}
