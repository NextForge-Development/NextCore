package gg.nextforge.ui.component.action;

/**
 * Functional interface for click actions inside UI components.
 */
@FunctionalInterface
public interface ClickAction {
    void execute(UIActionContext context);
}
