package gg.nextforge.pagination.model;

import java.util.List;

/**
 * Represents a paginated result set.
 * @param index the current page index (0-based)
 * @param totalPages the total number of pages available
 * @param content the list of items on the current page
 * @param <T> the type of items in the page
 */
public record Page<T>(int index, int totalPages, List<T> content) {

    /**
     * Checks if the page is empty.
     * @return true if the page has no content, false otherwise
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }
}
