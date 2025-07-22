package gg.nextforge.discord;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builder for Discord embed objects.
 */
public class DiscordEmbedBuilder {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    public DiscordEmbedBuilder setTitle(String title) {
        fields.put("title", title);
        return this;
    }

    public DiscordEmbedBuilder setDescription(String description) {
        fields.put("description", description);
        return this;
    }

    public DiscordEmbedBuilder setUrl(String url) {
        fields.put("url", url);
        return this;
    }

    public DiscordEmbedBuilder setColor(int rgb) {
        fields.put("color", rgb);
        return this;
    }

    public DiscordEmbedBuilder setFooter(String text, String iconUrl) {
        Map<String, String> footer = new LinkedHashMap<>();
        footer.put("text", text);
        if (iconUrl != null) footer.put("icon_url", iconUrl);
        fields.put("footer", footer);
        return this;
    }

    public DiscordEmbedBuilder setImage(String imageUrl) {
        Map<String, String> image = new LinkedHashMap<>();
        image.put("url", imageUrl);
        fields.put("image", image);
        return this;
    }

    public DiscordEmbedBuilder setThumbnail(String url) {
        Map<String, String> thumbnail = new LinkedHashMap<>();
        thumbnail.put("url", url);
        fields.put("thumbnail", thumbnail);
        return this;
    }

    public DiscordEmbedBuilder setAuthor(String name, String url, String iconUrl) {
        Map<String, String> author = new LinkedHashMap<>();
        author.put("name", name);
        if (url != null) author.put("url", url);
        if (iconUrl != null) author.put("icon_url", iconUrl);
        fields.put("author", author);
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        boolean first = true;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(toJson(entry.getValue()));
            first = false;
        }

        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String toJson(Object value) {
        if (value instanceof String) {
            return "\"" + escape((String) value) + "\"";
        } else if (value instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> entry : ((Map<String, String>) value).entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":\"").append(escape(entry.getValue())).append("\"");
                first = false;
            }
            sb.append("}");
            return sb.toString();
        } else {
            return String.valueOf(value);
        }
    }

    private String escape(String input) {
        return input.replace("\"", "\\\"").replace("\n", "\\n");
    }
}
