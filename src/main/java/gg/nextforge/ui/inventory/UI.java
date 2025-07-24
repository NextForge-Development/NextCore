package gg.nextforge.ui.inventory;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

import java.awt.*;

public interface UI {

    Component title(Component newTitle);

    Component title();

    void open(Audience... audiences);

    void close(Audience... audiences);

    void closeAll();

}
