package gg.nextforge.ui.context;


import gg.nextforge.ui.inventory.UI;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents global context for a UI instance.
 */
@Getter
@AllArgsConstructor
public class UIContext {
    private final UI ui;
}