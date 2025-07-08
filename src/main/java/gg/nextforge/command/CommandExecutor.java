package gg.nextforge.command;

/**
 * Functional interface for command execution.
 * This interface defines a single method to execute a command with a given context.
 * Implementations of this interface should provide the logic for handling commands
 * and their associated context.
 */
@FunctionalInterface
public interface CommandExecutor {

    /**
     * Execute the command with context.
     * @param context The command context containing information about the command execution.
     */
    void execute(CommandContext context);
}
