package gg.nextforge.ui.inventory.impl.paged;


import gg.nextforge.ui.component.UIComponent;
import gg.nextforge.ui.inventory.UI;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;

@Getter
@Setter
public class PaginatedUI implements UI {

    private final List<UIPage> pages = new ArrayList<>();
    private final Map<UUID, Integer> viewerPageMap = new HashMap<>();
    private final Set<UUID> viewers = new HashSet<>();

    private Component title;
    private int size;

    public PaginatedUI(Component title, int size) {
        this.title = title;
        this.size = size;
    }

    public UIPage getPage(int index) {
        while (pages.size() <= index) {
            pages.add(new UIPage(pages.size()));
        }
        return pages.get(index);
    }

    public void addComponent(int page, UIComponent component) {
        getPage(page).addComponent(component);
    }

    public void nextPage(Player player) {
        int current = viewerPageMap.getOrDefault(player.getUniqueId(), 0);
        if (current + 1 < pages.size()) {
            openPage(player, current + 1);
        }
    }

    public void previousPage(Player player) {
        int current = viewerPageMap.getOrDefault(player.getUniqueId(), 0);
        if (current > 0) {
            openPage(player, current - 1);
        }
    }

    public void openPage(Player player, int pageIndex) {
        UIPage page = getPage(pageIndex);
        Inventory inv = Bukkit.createInventory(null, size, title);
        for (UIComponent comp : page.getAll().values()) {
            inv.setItem(comp.getSlot(), comp.render());
        }
        viewerPageMap.put(player.getUniqueId(), pageIndex);
        player.openInventory(inv);
        viewers.add(player.getUniqueId());
    }

    @Override
    public void open(Audience... audiences) {
        for (Audience audience : audiences) {
            if (audience instanceof Player player) {
                openPage(player, 0);
            }
        }
    }

    @Override
    public void close(Audience... audiences) {
        for (Audience audience : audiences) {
            if (audience instanceof Player player) {
                player.closeInventory();
                viewers.remove(player.getUniqueId());
                viewerPageMap.remove(player.getUniqueId());
            }
        }
    }

    @Override
    public void closeAll() {
        for (UUID uuid : new HashSet<>(viewers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.closeInventory();
                viewerPageMap.remove(uuid);
            }
        }
        viewers.clear();
    }

    @Override
    public Component title(Component newTitle) {
        this.title = newTitle;
        return this.title;
    }

    @Override
    public Component title() {
        return this.title;
    }

    public int totalPages() {
        return pages.size();
    }

    public int getCurrentPage(Player player) {
        return viewerPageMap.getOrDefault(player.getUniqueId(), 0);
    }
}