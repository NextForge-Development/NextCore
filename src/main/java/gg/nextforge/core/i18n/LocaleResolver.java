// gg/nextforge/core/i18n/LocaleResolver.java
package gg.nextforge.core.i18n;

import java.util.Locale;

@FunctionalInterface
public interface LocaleResolver {
    Locale resolve(Object audience);
}
