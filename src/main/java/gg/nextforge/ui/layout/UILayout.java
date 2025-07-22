package gg.nextforge.ui.layout;

import gg.nextforge.ui.component.UIComponent;

import java.util.Map;

/**
 * Defines a contract for arranging components into a slot map.
 */
public interface UILayout {

    /**
     * Produces a map of slot index to UIComponent.
     *
     * @return layout mapping
     */
    Map<Integer, UIComponent> layout();

}