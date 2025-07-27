package com.ggm.lobby.models;

import com.ggm.lobby.managers.LobbyShopManager.ShopCategory;
import org.bukkit.Material;
import java.util.List;

public class ShopItem {
    private final String id;
    private final String name;
    private final List<String> description;
    private final long price;
    private final ShopCategory category;
    private final Material icon;

    public ShopItem(String id, String name, List<String> description, long price,
                    ShopCategory category, Material icon) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.icon = icon;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getDescription() { return description; }
    public long getPrice() { return price; }
    public ShopCategory getCategory() { return category; }
    public Material getIcon() { return icon; }
}
