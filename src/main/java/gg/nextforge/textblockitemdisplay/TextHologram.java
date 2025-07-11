package gg.nextforge.textblockitemdisplay;

import java.util.List;

/**
 * Represents a hologram consisting of text lines.
 */
public interface TextHologram extends Hologram {
    /**
     * @return immutable list of lines of text.
     */
    List<String> getLines();

    /**
     * Sets a specific line.
     *
     * @param index line index
     * @param text  new text
     */
    void setLine(int index, String text);

    /**
     * Adds a line to the end.
     *
     * @param text line to add
     */
    void addLine(String text);

    /**
     * Removes a line by index.
     *
     * @param index index of line
     */
    void removeLine(int index);
}
