package gg.nextforge.pagination;

import gg.nextforge.pagination.model.Page;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A utility class for managing pagination of a list of items.
 * This class provides methods to divide a list of items into pages
 * and retrieve specific pages based on the index.
 *
 * @param <T> the type of items to be paginated
 */
public class Pagination<T> {

    // The list of items to be paginated
    private final List<T> items = new ArrayList<>();
    // The number of items per page
    @Getter
    private final int itemsPerPage;

    /**
     * Constructs a Pagination object with the specified number of items per page.
     *
     * @param itemsPerPage the number of items per page; must be greater than 0
     * @throws IllegalArgumentException if itemsPerPage is less than or equal to 0
     */
    public Pagination(int itemsPerPage) {
        if (itemsPerPage <= 0) throw new IllegalArgumentException("itemsPerPage must be > 0");
        this.itemsPerPage = itemsPerPage;
    }

    /**
     * Adds a single item to the pagination.
     *
     * @param item the item to add
     */
    public void addItem(T item) {
        items.add(item);
    }

    /**
     * Adds a list of items to the pagination.
     *
     * @param elements the list of items to add
     */
    public void addAll(List<T> elements) {
        items.addAll(elements);
    }

    /**
     * Retrieves a specific page of items based on the given index.
     *
     * @param index the index of the page to retrieve (0-based)
     * @return a {@link Page} object containing the items for the specified page
     * or an empty page if the index is out of bounds
     */
    public Page<T> getPage(int index) {
        int totalPages = getTotalPages();
        if (index < 0 || index >= totalPages) return new Page<>(index, totalPages, Collections.emptyList());

        int start = index * itemsPerPage;
        int end = Math.min(start + itemsPerPage, items.size());

        List<T> sublist = items.subList(start, end);
        return new Page<>(index, totalPages, new ArrayList<>(sublist));
    }

    /**
     * Calculates the total number of pages based on the number of items and items per page.
     *
     * @return the total number of pages
     */
    public int getTotalPages() {
        return (int) Math.ceil((double) items.size() / itemsPerPage);
    }

    /**
     * Returns the total number of items in the pagination.
     *
     * @return the total number of items
     */
    public int getTotalItems() {
        return items.size();
    }

    /**
     * Checks if the pagination contains no items.
     *
     * @return true if there are no items, false otherwise
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
}