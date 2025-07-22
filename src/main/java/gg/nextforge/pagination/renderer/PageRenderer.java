package gg.nextforge.pagination.renderer;

import gg.nextforge.pagination.model.Page;

import java.util.List;

/**
 * Functional interface for rendering a page of items.
 * @param <T> the type of items in the page
 */
@FunctionalInterface
public interface PageRenderer<T> {
    /**
     * Render a page of items.
     * @param page the page to render
     * @return a list of strings representing the rendered items
     */
    List<String> render(Page<T> page);
}
