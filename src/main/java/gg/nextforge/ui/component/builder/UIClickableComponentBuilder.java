package gg.nextforge.ui.component.builder;

import gg.nextforge.ui.component.impl.UIButtonComponent;
import gg.nextforge.ui.component.action.ClickAction;
import gg.nextforge.ui.component.action.ClickType;
import net.kyori.adventure.text.Component;

import java.util.EnumMap;
import java.util.Map;

public class UIClickableComponentBuilder extends UIComponentBuilder {

    private Component tooltip;
    private final Map<ClickType, ClickAction> clickActions = new EnumMap<>(ClickType.class);

    public static UIClickableComponentBuilder create() {
        return new UIClickableComponentBuilder();
    }

    public UIClickableComponentBuilder tooltip(Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public UIClickableComponentBuilder onClick(ClickType type, ClickAction action) {
        this.clickActions.put(type, action);
        return this;
    }

    @Override
    public UIButtonComponent build() {
        UIButtonComponent button = new UIButtonComponent(slot, item, tooltip);
        button.setId(id);
        clickActions.forEach(button::onClick);
        return button;
    }
}
