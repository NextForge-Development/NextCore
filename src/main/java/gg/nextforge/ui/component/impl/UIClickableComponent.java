package gg.nextforge.ui.component.impl;

import gg.nextforge.ui.component.UIComponent;
import gg.nextforge.ui.component.action.ClickAction;
import gg.nextforge.ui.component.action.ClickType;
import gg.nextforge.ui.component.action.UIActionContext;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumMap;
import java.util.Map;

@Getter
@Setter
public abstract class UIClickableComponent extends UIComponent {

    private final Map<ClickType, ClickAction> clickActions = new EnumMap<>(ClickType.class);

    public UIClickableComponent(int slot) {
        super(slot);
    }

    /**
     * Registers a click action for a specific click type.
     */
    public void onClick(ClickType type, ClickAction action) {
        this.clickActions.put(type, action);
    }

    /**
     * Called when the player clicks this component.
     */
    public void handleClick(UIActionContext context) {
        ClickAction action = clickActions.get(context.clickType());

        if (action != null) {
            action.execute(context);
        } else {
            handleDefaultClick(context);
        }
    }

    /**
     * Optional default click behavior if no specific ClickType is matched.
     */
    protected void handleDefaultClick(UIActionContext context) {
        // override in subclass if needed
    }
}