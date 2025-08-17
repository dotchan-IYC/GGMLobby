package com.ggm.lobby.managers;

import com.ggm.lobby.GGMLobby;
import com.ggm.lobby.models.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class LobbyShopManager {

    private final GGMLobby plugin;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;

    // 상점 아이템 카테고리
    public enum ShopCategory {
        COSMETICS("코스메틱", Material.NETHER_STAR),
        CHAT("채팅 강화", Material.PAPER),
        UTILITIES("편의 기능", Material.ENDER_CHEST),
        BOOSTS("부스터", Material.EXPERIENCE_BOTTLE),
        SPECIAL("특별 아이템", Material.CHEST); // GIFT → CHEST

        private final String displayName;
        private final Material icon;

        ShopCategory(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
    }

    private final Map<String, ShopItem> shopItems = new HashMap<>();

    public LobbyShopManager(GGMLobby plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.economyManager = plugin.getEconomyManager();
        initializeShopItems();
    }

    private void initializeShopItems() {
        // 코스메틱
        shopItems.put("heart_particle", new ShopItem("heart_particle", "하트 파티클",
                Arrays.asList("§7걸을 때 하트가 떨어져요!", "§7지속시간: 30일"),
                10000L, ShopCategory.COSMETICS, Material.REDSTONE));

        shopItems.put("rich_title", new ShopItem("rich_title", "부자 칭호",
                Arrays.asList("§7닉네임 앞에 §6[부자]§7 표시", "§7영구적으로 사용 가능"),
                15000L, ShopCategory.COSMETICS, Material.GOLD_INGOT));

        // 채팅 강화
        shopItems.put("chat_color", new ShopItem("chat_color", "채팅 색상",
                Arrays.asList("§7채팅에 색상을 입혀보세요!", "§7지속시간: 30일"),
                5000L, ShopCategory.CHAT, Material.RED_DYE));

        shopItems.put("emoji_pack", new ShopItem("emoji_pack", "😀 이모티콘 팩",
                Arrays.asList("§7추가 이모티콘 20개 해금!", "§7영구적으로 사용 가능"),
                8000L, ShopCategory.CHAT, Material.PAINTING));

        // 편의 기능
        shopItems.put("storage_expand", new ShopItem("storage_expand", "보관함 확장",
                Arrays.asList("§7개인 보관함 슬롯 +27개", "§7영구적으로 확장됨"),
                25000L, ShopCategory.UTILITIES, Material.ENDER_CHEST));

        shopItems.put("quick_teleport", new ShopItem("quick_teleport", "빠른 이동권",
                Arrays.asList("§7서버 이동 시 로딩 시간 단축", "§7사용 횟수: 10회"),
                3000L, ShopCategory.UTILITIES, Material.ENDER_PEARL));

        // 부스터
        shopItems.put("money_boost", new ShopItem("money_boost", "G 부스터",
                Arrays.asList("§7G 획득량 2배 증가!", "§7지속시간: 24시간"),
                15000L, ShopCategory.BOOSTS, Material.GOLD_BLOCK));

        shopItems.put("dragon_boost", new ShopItem("dragon_boost", "드래곤 부스터",
                Arrays.asList("§7드래곤 보상 1.2배 증가!", "§7지속시간: 3일"),
                30000L, ShopCategory.BOOSTS, Material.DRAGON_HEAD));
    }

    /**
     * 상점 아이템 구매
     */
    public CompletableFuture<Boolean> purchaseItem(UUID playerId, String playerName, String itemId) {
        return CompletableFuture.supplyAsync(() -> {
            ShopItem item = shopItems.get(itemId);
            if (item == null) {
                return false;
            }

            // 잔액 확인
            long playerMoney = economyManager.getMoney(playerId).join();
            if (playerMoney < item.getPrice()) {
                return false;
            }

            try (Connection conn = databaseManager.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    // G 차감
                    if (!economyManager.removeMoney(playerId, item.getPrice()).join()) {
                        conn.rollback();
                        return false;
                    }

                    // 구매 기록 저장
                    String sql = """
                        INSERT INTO ggm_shop_purchases 
                        (uuid, player_name, item_id, price, expiry_time)
                        VALUES (?, ?, ?, ?, ?)
                        """;

                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, playerId.toString());
                        stmt.setString(2, playerName);
                        stmt.setString(3, itemId);
                        stmt.setLong(4, item.getPrice());

                        // 만료 시간 계산 (아이템에 따라 다름)
                        LocalDateTime expiryTime = calculateExpiryTime(itemId);
                        if (expiryTime != null) {
                            stmt.setObject(5, expiryTime);
                        } else {
                            stmt.setNull(5, java.sql.Types.TIMESTAMP);
                        }

                        stmt.executeUpdate();
                    }

                    conn.commit();
                    return true;

                } catch (Exception e) {
                    conn.rollback();
                    plugin.getLogger().severe("상점 구매 실패: " + e.getMessage());
                    return false;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("상점 구매 오류: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * 상점 메인 GUI 생성
     */
    public Inventory createShopGUI(ShopCategory category) {
        String title = category != null ? category.getDisplayName() + " 상점" : "🏪 로비 상점";
        Inventory gui = Bukkit.createInventory(null, 54, title);

        // 카테고리 선택 버튼 (상단)
        for (int i = 0; i < ShopCategory.values().length; i++) {
            ShopCategory cat = ShopCategory.values()[i];
            ItemStack catItem = new ItemStack(cat.getIcon());
            ItemMeta meta = catItem.getItemMeta();
            meta.setDisplayName(cat.getDisplayName());
            meta.setLore(Arrays.asList("§7클릭하여 이 카테고리 보기"));
            catItem.setItemMeta(meta);
            gui.setItem(i, catItem);
        }

        // 상품 표시
        List<ShopItem> categoryItems = shopItems.values().stream()
                .filter(item -> category == null || item.getCategory() == category)
                .toList();

        for (int i = 0; i < Math.min(categoryItems.size(), 45); i++) {
            ShopItem item = categoryItems.get(i);
            ItemStack displayItem = createShopDisplayItem(item);
            gui.setItem(9 + i, displayItem);
        }

        return gui;
    }

    private ItemStack createShopDisplayItem(ShopItem item) {
        ItemStack displayItem = new ItemStack(item.getIcon());
        ItemMeta meta = displayItem.getItemMeta();

        meta.setDisplayName("§f" + item.getName());

        List<String> lore = new ArrayList<>(item.getDescription());
        lore.add("");
        lore.add("§6💰 가격: §f" + economyManager.formatMoney(item.getPrice()) + "G");
        lore.add("");
        lore.add("§e클릭하여 구매하기");

        meta.setLore(lore);
        displayItem.setItemMeta(meta);

        return displayItem;
    }

    /**
     * 플레이어가 아이템을 소유하고 있는지 확인
     */
    public CompletableFuture<Boolean> hasItem(UUID playerId, String itemId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String sql = """
                    SELECT COUNT(*) FROM ggm_shop_purchases 
                    WHERE uuid = ? AND item_id = ? 
                    AND (expiry_time IS NULL OR expiry_time > ?)
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, playerId.toString());
                    stmt.setString(2, itemId);
                    stmt.setObject(3, LocalDateTime.now());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1) > 0;
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("아이템 소유 확인 실패: " + e.getMessage());
            }
            return false;
        });
    }

    private LocalDateTime calculateExpiryTime(String itemId) {
        return switch (itemId) {
            case "heart_particle", "chat_color" -> LocalDateTime.now().plusDays(30);
            case "money_boost" -> LocalDateTime.now().plusDays(1);
            case "dragon_boost" -> LocalDateTime.now().plusDays(3);
            case "quick_teleport" -> LocalDateTime.now().plusDays(7);
            default -> null; // 영구 아이템
        };
    }

    public ShopItem getShopItem(String itemId) {
        return shopItems.get(itemId);
    }

    public Collection<ShopItem> getAllItems() {
        return shopItems.values();
    }
}
