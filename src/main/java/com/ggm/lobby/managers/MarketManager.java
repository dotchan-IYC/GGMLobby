package com.ggm.lobby.managers;

import com.ggm.lobby.GGMLobby;
import com.ggm.lobby.models.MarketListing;
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

public class MarketManager {

    private final GGMLobby plugin;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;

    public enum MarketCategory {
        WEAPONS("무기류", Material.DIAMOND_SWORD),
        ARMOR("방어구", Material.DIAMOND_CHESTPLATE),
        TOOLS("도구류", Material.DIAMOND_PICKAXE),
        BLOCKS("블록류", Material.STONE),
        FOOD("음식", Material.COOKED_BEEF),
        POTIONS("포션류", Material.POTION),
        ENCHANT_BOOKS("인첸트북", Material.ENCHANTED_BOOK),
        RARE_ITEMS("희귀 아이템", Material.NETHER_STAR),
        MISC("기타", Material.CHEST);

        private final String displayName;
        private final Material icon;

        MarketCategory(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
    }

    public MarketManager(GGMLobby plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.economyManager = plugin.getEconomyManager();
    }

    /**
     * 아이템 판매 등록
     */
    public CompletableFuture<Boolean> listItem(UUID sellerId, String sellerName, ItemStack item, long price) {
        return CompletableFuture.supplyAsync(() -> {
            // 판매 가능한 아이템인지 확인
            if (!isValidForSale(item)) {
                return false;
            }

            // 플레이어의 현재 등록 개수 확인
            if (getPlayerListingCount(sellerId) >= 10) {
                return false; // 최대 10개까지만 등록 가능
            }

            try (Connection conn = databaseManager.getConnection()) {
                String sql = """
                    INSERT INTO ggm_market_listings 
                    (listing_id, seller_uuid, seller_name, item_data, price, category, expiry_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    String listingId = UUID.randomUUID().toString();
                    LocalDateTime expiryTime = LocalDateTime.now().plusDays(7);

                    stmt.setString(1, listingId);
                    stmt.setString(2, sellerId.toString());
                    stmt.setString(3, sellerName);
                    stmt.setString(4, itemToString(item));
                    stmt.setLong(5, price);
                    stmt.setString(6, categorizeItem(item).name());
                    stmt.setObject(7, expiryTime);

                    return stmt.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("아이템 등록 실패: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * 아이템 구매
     */
    public CompletableFuture<Boolean> buyItem(UUID buyerId, String buyerName, String listingId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    // 거래소 아이템 조회
                    MarketListing listing = getListingById(conn, listingId);
                    if (listing == null || listing.isSold()) {
                        conn.rollback();
                        return false;
                    }

                    // 구매자 잔액 확인
                    long buyerMoney = economyManager.getMoney(buyerId).join();
                    if (buyerMoney < listing.getPrice()) {
                        conn.rollback();
                        return false;
                    }

                    // 거래 처리
                    // 1. 구매자 돈 차감
                    economyManager.removeMoney(buyerId, listing.getPrice()).join();

                    // 2. 판매자에게 돈 지급 (5% 수수료 차감)
                    long sellerReceive = (long) (listing.getPrice() * 0.95);
                    economyManager.addMoney(listing.getSellerId(), sellerReceive).join();

                    // 3. 거래소 아이템 판매 완료 처리
                    String updateSql = """
                        UPDATE ggm_market_listings 
                        SET sold = TRUE, buyer_uuid = ?, buyer_name = ?, sold_time = ?
                        WHERE listing_id = ?
                        """;

                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, buyerId.toString());
                        updateStmt.setString(2, buyerName);
                        updateStmt.setObject(3, LocalDateTime.now());
                        updateStmt.setString(4, listingId);
                        updateStmt.executeUpdate();
                    }

                    // 4. 구매자에게 아이템 지급 (온라인인 경우)
                    Player buyer = Bukkit.getPlayer(buyerId);
                    if (buyer != null && buyer.isOnline()) {
                        ItemStack item = stringToItem(listing.getItemData());
                        if (buyer.getInventory().firstEmpty() != -1) {
                            buyer.getInventory().addItem(item);
                        } else {
                            buyer.getWorld().dropItemNaturally(buyer.getLocation(), item);
                        }
                    }

                    conn.commit();
                    return true;

                } catch (Exception e) {
                    conn.rollback();
                    plugin.getLogger().severe("아이템 구매 실패: " + e.getMessage());
                    return false;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("거래소 구매 오류: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * 거래소 메인 GUI 생성
     */
    public Inventory createMarketGUI(MarketCategory category, int page) {
        String title = category != null ? category.getDisplayName() + " 거래소" : "플레이어 거래소";
        Inventory gui = Bukkit.createInventory(null, 54, title);

        // 카테고리 버튼 (상단)
        for (int i = 0; i < MarketCategory.values().length; i++) {
            MarketCategory cat = MarketCategory.values()[i];
            ItemStack catItem = new ItemStack(cat.getIcon());
            ItemMeta meta = catItem.getItemMeta();
            meta.setDisplayName(cat.getDisplayName());
            meta.setLore(Arrays.asList("§7클릭하여 이 카테고리 보기"));
            catItem.setItemMeta(meta);
            gui.setItem(i, catItem);
        }

        // 거래소 아이템 표시 (중간 부분)
        getMarketListings(category, page, 36).thenAccept(listings -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (int i = 0; i < Math.min(listings.size(), 36); i++) {
                    MarketListing listing = listings.get(i);
                    ItemStack displayItem = createMarketDisplayItem(listing);
                    gui.setItem(9 + i, displayItem);
                }

                // 페이지 네비게이션
                if (page > 0) {
                    ItemStack prevPage = new ItemStack(Material.ARROW);
                    ItemMeta prevMeta = prevPage.getItemMeta();
                    prevMeta.setDisplayName("§e이전 페이지");
                    prevPage.setItemMeta(prevMeta);
                    gui.setItem(45, prevPage);
                }

                if (listings.size() >= 36) {
                    ItemStack nextPage = new ItemStack(Material.ARROW);
                    ItemMeta nextMeta = nextPage.getItemMeta();
                    nextMeta.setDisplayName("§e다음 페이지");
                    nextPage.setItemMeta(nextMeta);
                    gui.setItem(53, nextPage);
                }
            });
        });

        return gui;
    }

    private ItemStack createMarketDisplayItem(MarketListing listing) {
        ItemStack item = stringToItem(listing.getItemData()).clone();
        ItemMeta meta = item.getItemMeta();

        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) {
            lore.addAll(meta.getLore());
        }

        lore.add("");
        lore.add("§6가격: §f" + economyManager.formatMoney(listing.getPrice()) + "G");
        lore.add("§7판매자: §f" + listing.getSellerName());
        lore.add("§7등록일: §f" + listing.getListedTime().toLocalDate());
        lore.add("");
        lore.add("§e클릭하여 구매하기");

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 내 판매 목록 GUI
     */
    public Inventory createMyListingsGUI(UUID playerId) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6내 판매 목록");

        getPlayerListings(playerId).thenAccept(listings -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (int i = 0; i < Math.min(listings.size(), 54); i++) {
                    MarketListing listing = listings.get(i);
                    ItemStack displayItem = createMyListingDisplayItem(listing);
                    gui.setItem(i, displayItem);
                }
            });
        });

        return gui;
    }

    private ItemStack createMyListingDisplayItem(MarketListing listing) {
        ItemStack item = stringToItem(listing.getItemData()).clone();
        ItemMeta meta = item.getItemMeta();

        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) {
            lore.addAll(meta.getLore());
        }

        lore.add("");
        lore.add("§6가격: §f" + economyManager.formatMoney(listing.getPrice()) + "G");

        if (listing.isSold()) {
            lore.add("§a판매 완료");
            lore.add("§7구매자: §f" + listing.getBuyerName());
            lore.add("§7판매일: §f" + listing.getSoldTime().toLocalDate());
        } else {
            lore.add("§e판매 중");
            lore.add("§7만료일: §f" + listing.getExpiryTime().toLocalDate());
            lore.add("");
            lore.add("§c우클릭하여 취소하기");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 만료된 아이템 정리
     */
    public void cleanupExpiredListings() {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = """
                DELETE FROM ggm_market_listings 
                WHERE expiry_time < ? AND sold = FALSE
                """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, LocalDateTime.now());
                int deleted = stmt.executeUpdate();

                if (deleted > 0) {
                    plugin.getLogger().info("만료된 거래소 아이템 " + deleted + "개 정리 완료");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("만료된 아이템 정리 실패: " + e.getMessage());
        }
    }

    // 유틸리티 메서드들
    private boolean isValidForSale(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        // 판매 금지 아이템 목록
        Set<Material> bannedItems = Set.of(
                Material.BEDROCK,
                Material.COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK,
                Material.BARRIER
        );

        return !bannedItems.contains(item.getType());
    }

    private MarketCategory categorizeItem(ItemStack item) {
        Material type = item.getType();
        String name = type.name();

        if (name.contains("SWORD") || name.contains("BOW") || name.contains("CROSSBOW")) {
            return MarketCategory.WEAPONS;
        } else if (name.contains("HELMET") || name.contains("CHESTPLATE") ||
                name.contains("LEGGINGS") || name.contains("BOOTS")) {
            return MarketCategory.ARMOR;
        } else if (name.contains("PICKAXE") || name.contains("AXE") ||
                name.contains("SHOVEL") || name.contains("HOE")) {
            return MarketCategory.TOOLS;
        } else if (type == Material.ENCHANTED_BOOK) {
            return MarketCategory.ENCHANT_BOOKS;
        } else if (type.isEdible()) {
            return MarketCategory.FOOD;
        } else if (name.contains("POTION")) {
            return MarketCategory.POTIONS;
        } else if (type.isBlock()) {
            return MarketCategory.BLOCKS;
        } else if (isRareItem(type)) {
            return MarketCategory.RARE_ITEMS;
        } else {
            return MarketCategory.MISC;
        }
    }

    private boolean isRareItem(Material type) {
        Set<Material> rareItems = Set.of(
                Material.NETHER_STAR,
                Material.DRAGON_EGG,
                Material.ELYTRA,
                Material.NETHERITE_INGOT,
                Material.HEART_OF_THE_SEA
        );
        return rareItems.contains(type);
    }

    private int getPlayerListingCount(UUID playerId) {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT COUNT(*) FROM ggm_market_listings WHERE seller_uuid = ? AND sold = FALSE";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("판매 목록 개수 조회 실패: " + e.getMessage());
        }
        return 0;
    }

    private CompletableFuture<List<MarketListing>> getMarketListings(MarketCategory category, int page, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<MarketListing> listings = new ArrayList<>();

            try (Connection conn = databaseManager.getConnection()) {
                String sql = """
                    SELECT * FROM ggm_market_listings 
                    WHERE sold = FALSE AND expiry_time > ?
                    """ + (category != null ? " AND category = ?" : "") + """
                    ORDER BY listed_time DESC
                    LIMIT ? OFFSET ?
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    int paramIndex = 1;
                    stmt.setObject(paramIndex++, LocalDateTime.now());

                    if (category != null) {
                        stmt.setString(paramIndex++, category.name());
                    }

                    stmt.setInt(paramIndex++, limit);
                    stmt.setInt(paramIndex, page * limit);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            listings.add(createMarketListingFromResultSet(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("거래소 목록 조회 실패: " + e.getMessage());
            }

            return listings;
        });
    }

    private CompletableFuture<List<MarketListing>> getPlayerListings(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            List<MarketListing> listings = new ArrayList<>();

            try (Connection conn = databaseManager.getConnection()) {
                String sql = """
                    SELECT * FROM ggm_market_listings 
                    WHERE seller_uuid = ?
                    ORDER BY listed_time DESC
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, playerId.toString());

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            listings.add(createMarketListingFromResultSet(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("플레이어 판매 목록 조회 실패: " + e.getMessage());
            }

            return listings;
        });
    }

    private MarketListing getListingById(Connection conn, String listingId) throws SQLException {
        String sql = "SELECT * FROM ggm_market_listings WHERE listing_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, listingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return createMarketListingFromResultSet(rs);
                }
            }
        }
        return null;
    }

    private MarketListing createMarketListingFromResultSet(ResultSet rs) throws SQLException {
        return new MarketListing(
                rs.getString("listing_id"),
                UUID.fromString(rs.getString("seller_uuid")),
                rs.getString("seller_name"),
                rs.getString("item_data"),
                rs.getLong("price"),
                MarketCategory.valueOf(rs.getString("category")),
                rs.getTimestamp("listed_time").toLocalDateTime(),
                rs.getTimestamp("expiry_time").toLocalDateTime(),
                rs.getBoolean("sold"),
                rs.getString("buyer_uuid") != null ? UUID.fromString(rs.getString("buyer_uuid")) : null,
                rs.getString("buyer_name"),
                rs.getTimestamp("sold_time") != null ? rs.getTimestamp("sold_time").toLocalDateTime() : null
        );
    }

    // 아이템 직렬화/역직렬화 (간단한 버전)
    private String itemToString(ItemStack item) {
        return item.getType().name() + ":" + item.getAmount();
    }

    private ItemStack stringToItem(String data) {
        String[] parts = data.split(":");
        Material material = Material.valueOf(parts[0]);
        int amount = Integer.parseInt(parts[1]);
        return new ItemStack(material, amount);
    }
}