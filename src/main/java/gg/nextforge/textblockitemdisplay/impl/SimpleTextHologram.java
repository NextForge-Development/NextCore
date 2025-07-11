package gg.nextforge.textblockitemdisplay.impl;

import gg.nextforge.textblockitemdisplay.TextHologram;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Basic implementation of a text hologram.
 */
public class SimpleTextHologram extends AbstractHologram implements TextHologram {
    private final List<String> lines = Collections.synchronizedList(new ArrayList<>());

    public SimpleTextHologram(String name, Location location) {
        super(name, location);
    }

    @Override
    public List<String> getLines() {
        return Collections.unmodifiableList(new ArrayList<>(lines));
    }

    @Override
    public void setLine(int index, String text) {
        lines.set(index, text);
    }

    @Override
    public void addLine(String text) {
        lines.add(text);
    }

    @Override
    public void removeLine(int index) {
        lines.remove(index);
    }
}
