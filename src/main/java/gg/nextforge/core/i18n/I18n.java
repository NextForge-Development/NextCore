// gg/nextforge/core/i18n/I18n.java
package gg.nextforge.core.i18n;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.*;
import java.util.stream.Collectors;

public final class I18n {
    private final YamlMessageSource source;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final LocaleResolver resolver;

    public I18n(YamlMessageSource source, LocaleResolver resolver) {
        this.source = source;
        this.resolver = resolver;
    }

    public Component component(Object audience, String key, Map<String, String> vars) {
        Locale loc = locale(audience);
        String raw = source.getRaw(loc, key).orElse(key);
        TagResolver[] tags = vars.entrySet().stream()
                .map(e -> Placeholder.parsed(e.getKey(), e.getValue()))
                .toArray(TagResolver[]::new);
        return mm.deserialize(raw, TagResolver.resolver(tags));
    }

    public Component component(String key, Map<String, String> vars) {
        return component(null, key, vars);
    }

    public Component component(Object audience, String key) {
        return component(audience, key, Map.of());
    }

    public String raw(Object audience, String key) {
        return source.getRaw(locale(audience), key).orElse(key);
    }

    private Locale locale(Object audience) {
        Locale l = resolver != null ? resolver.resolve(audience) : null;
        return l != null ? l : source.getDefaultLocale();
    }

    /* Convenience Builder */
    public static Map<String, String> vars(Object... kv) {
        if (kv.length % 2 != 0) throw new IllegalArgumentException("vars must be key,value pairs");
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(String.valueOf(kv[i]), String.valueOf(kv[i + 1]));
        return m;
    }
}
