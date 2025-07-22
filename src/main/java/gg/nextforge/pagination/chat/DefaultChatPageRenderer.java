package gg.nextforge.pagination.chat;

import gg.nextforge.pagination.model.Page;
import gg.nextforge.pagination.renderer.PageRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link PageRenderer} for rendering chat pages.
 * @param <T> the type of items in the page
 */
public class DefaultChatPageRenderer<T> implements PageRenderer<T> {

    // Header and footer templates for the chat page
    private final String header;
    private final String footer;

    /**
     * Creates a new DefaultChatPageRenderer with the specified header and footer.
     * @param header the header template, which should contain placeholders for page index and total pages
     * @param footer the footer template
     */
    public DefaultChatPageRenderer(String header, String footer) {
        this.header = header;
        this.footer = footer;
    }

    /**
     * Renders the given page into a list of strings suitable for chat display.
     * @param page the page to render
     * @return a list of strings representing the rendered page
     */
    @Override
    public List<String> render(Page<T> page) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(header, page.index() + 1, page.totalPages()));

        for (T item : page.content()) {
            lines.add(item.toString());
        }

        lines.add(footer);
        return lines;
    }
}
