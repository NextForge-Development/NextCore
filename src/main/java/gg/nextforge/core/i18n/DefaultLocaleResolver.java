// gg/nextforge/core/i18n/DefaultLocaleResolver.java
package gg.nextforge.core.i18n;

import java.util.Locale;

public class DefaultLocaleResolver implements LocaleResolver {
    private final Locale fallback;
    public DefaultLocaleResolver(Locale fallback) { this.fallback = fallback == null ? Locale.ENGLISH : fallback; }
    @Override public Locale resolve(Object audience) { return fallback; }
}
