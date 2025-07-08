package gg.nextforge.command;

import java.util.List;

/**
 * Functional interface for tab completion.
 * Returns a list of suggestions based on context.
 */
@FunctionalInterface
public interface TabCompleter {

    /**
     * Completes the command based on the provided context.
     * @return empty list for no suggestions, or a list of suggestions.
     */
    List<String> complete(CommandContext context);
}