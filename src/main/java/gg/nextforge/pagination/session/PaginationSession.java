package gg.nextforge.pagination.session;

import gg.nextforge.pagination.Pagination;
import gg.nextforge.pagination.model.Page;

/**
 * A session for managing pagination state.
 * This class allows navigating through pages of a {@link Pagination} object.
 *
 * @param <T> the type of items in the pagination
 */
public class PaginationSession<T> {

    // The Pagination object that holds the items and pagination logic
    private final Pagination<T> pagination;
    // The current page index, starting from 0
    private int currentPage = 0;

    /**
     * Constructs a PaginationSession with the given Pagination object.
     *
     * @param pagination the Pagination object to manage
     */
    public PaginationSession(Pagination<T> pagination) {
        this.pagination = pagination;
    }

    /**
     * Moves to the next page if available and returns it.
     *
     * @return the next {@link Page} of items
     */
    public Page<T> next() {
        if (currentPage + 1 < pagination.getTotalPages()) currentPage++;
        return pagination.getPage(currentPage);
    }

    /**
     * Moves to the previous page if available and returns it.
     *
     * @return the previous {@link Page} of items
     */
    public Page<T> previous() {
        if (currentPage > 0) currentPage--;
        return pagination.getPage(currentPage);
    }

    /**
     * Returns the current page without changing the state.
     *
     * @return the current {@link Page} of items
     */
    public Page<T> current() {
        return pagination.getPage(currentPage);
    }

    /**
     * Checks if there is a next page available.
     *
     * @return true if a next page exists, false otherwise
     */
    public boolean hasNext() {
        return currentPage + 1 < pagination.getTotalPages();
    }

    /**
     * Checks if there is a previous page available.
     *
     * @return true if a previous page exists, false otherwise
     */
    public boolean hasPrevious() {
        return currentPage > 0;
    }

    /**
     * Resets the session to the first page.
     */
    public void reset() {
        currentPage = 0;
    }
}