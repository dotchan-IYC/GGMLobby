package com.ggm.lobby.managers;

import com.ggm.lobby.GGMLobby;
import com.ggm.lobby.models.RankingData;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RankingManager {

    private final GGMLobby plugin;
    private final DatabaseManager databaseManager;

    public enum RankingType {
        MONEY("G 보유량", ""),
        PLAYTIME("플레이 시간", ""),
        DRAGON_KILLS("드래곤 처치", ""),
        MARKET_SALES("거래소 판매", ""),
        ATTENDANCE("연속 출석", "");

        private final String displayName;
        private final String emoji;

        RankingType(String displayName, String emoji) {
            this.displayName = displayName;
            this.emoji = emoji;
        }

        public String getDisplayName() { return displayName; }
        public String getEmoji() { return emoji; }
    }

    public RankingManager(GGMLobby plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * 모든 랭킹 업데이트
     */
    public void updateAllRankings() {
        plugin.getLogger().info("랭킹 업데이트 시작...");

        for (RankingType type : RankingType.values()) {
            updateRanking(type).thenRun(() -> {
                plugin.getLogger().info(type.getDisplayName() + " 랭킹 업데이트 완료");
            });
        }
    }

    /**
     * 특정 타입의 랭킹 업데이트
     */
    public CompletableFuture<Void> updateRanking(RankingType type) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String dataQuery = getDataQuery(type);

                // 기존 랭킹 데이터 삭제
                String deleteSql = "DELETE FROM ggm_rankings WHERE ranking_type = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, type.name());
                    deleteStmt.executeUpdate();
                }

                // 새 랭킹 데이터 삽입
                String insertSql = """
                    INSERT INTO ggm_rankings (uuid, ranking_type, score, rank_position)
                    SELECT uuid, ?, score, ROW_NUMBER() OVER (ORDER BY score DESC) as rank_position
                    FROM (%s) as ranking_data
                    """.formatted(dataQuery);

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, type.name());
                    insertStmt.executeUpdate();
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("랭킹 업데이트 실패: " + e.getMessage());
            }
        });
    }

    private String getDataQuery(RankingType type) {
        return switch (type) {
            case MONEY -> """
                SELECT uuid, COALESCE(money, 0) as score
                FROM ggm_players
                WHERE money > 0
                """;
            case PLAYTIME -> """
                SELECT uuid, COALESCE(playtime, 0) as score
                FROM ggm_players
                WHERE playtime > 0
                """;
            case DRAGON_KILLS -> """
                SELECT uuid, COUNT(*) as score
                FROM ggm_dragon_rewards
                GROUP BY uuid
                """;
            case MARKET_SALES -> """
                SELECT seller_uuid as uuid, COUNT(*) as score
                FROM ggm_market_listings
                WHERE sold = TRUE
                GROUP BY seller_uuid
                """;
            case ATTENDANCE -> """
                SELECT uuid, COALESCE(consecutive_days, 0) as score
                FROM ggm_attendance
                WHERE consecutive_days > 0
                """;
        };
    }

    /**
     * 랭킹 조회
     */
    public CompletableFuture<List<RankingData>> getRanking(RankingType type, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<RankingData> rankings = new ArrayList<>();

            try (Connection conn = databaseManager.getConnection()) {
                String sql = """
                    SELECT r.uuid, p.player_name, r.score, r.rank_position
                    FROM ggm_rankings r
                    LEFT JOIN ggm_players p ON r.uuid = p.uuid
                    WHERE r.ranking_type = ?
                    ORDER BY r.rank_position
                    LIMIT ?
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, type.name());
                    stmt.setInt(2, limit);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            rankings.add(new RankingData(
                                    UUID.fromString(rs.getString("uuid")),
                                    rs.getString("player_name"),
                                    rs.getLong("score"),
                                    rs.getInt("rank_position"),
                                    type
                            ));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("랭킹 조회 실패: " + e.getMessage());
            }

            return rankings;
        });
    }

    /**
     * 플레이어의 순위 조회
     */
    public CompletableFuture<RankingData> getPlayerRank(UUID uuid, RankingType type) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String sql = """
                    SELECT r.uuid, p.player_name, r.score, r.rank_position
                    FROM ggm_rankings r
                    LEFT JOIN ggm_players p ON r.uuid = p.uuid
                    WHERE r.ranking_type = ? AND r.uuid = ?
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, type.name());
                    stmt.setString(2, uuid.toString());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return new RankingData(
                                    uuid,
                                    rs.getString("player_name"),
                                    rs.getLong("score"),
                                    rs.getInt("rank_position"),
                                    type
                            );
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("플레이어 순위 조회 실패: " + e.getMessage());
            }

            return null;
        });
    }

    /**
     * 랭킹 GUI 생성
     */
    public Inventory createRankingGUI(RankingType type) {
        Inventory gui = Bukkit.createInventory(null, 54,
                type.getEmoji() + " §6§l" + type.getDisplayName() + " 랭킹");

        // 랭킹 데이터 조회 및 GUI 업데이트
        getRanking(type, 45).thenAccept(rankings -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (int i = 0; i < Math.min(rankings.size(), 45); i++) {
                    RankingData ranking = rankings.get(i);
                    ItemStack playerItem = createPlayerRankItem(ranking, i + 1);
                    gui.setItem(i, playerItem);
                }

                // 랭킹 타입 선택 버튼
                for (int i = 0; i < RankingType.values().length; i++) {
                    RankingType rankType = RankingType.values()[i];
                    ItemStack typeItem = new ItemStack(Material.PAPER);
                    ItemMeta meta = typeItem.getItemMeta();
                    meta.setDisplayName(rankType.getEmoji() + " §f" + rankType.getDisplayName());
                    meta.setLore(Arrays.asList("§7클릭하여 이 랭킹 보기"));
                    typeItem.setItemMeta(meta);
                    gui.setItem(45 + i, typeItem);
                }
            });
        });

        return gui;
    }

    private ItemStack createPlayerRankItem(RankingData ranking, int position) {
        Material material = switch (position) {
            case 1 -> Material.GOLD_INGOT;
            case 2 -> Material.IRON_INGOT;      // SILVER_INGOT 대신 IRON_INGOT
            case 3 -> Material.COPPER_INGOT;    // 1.17+ 버전에서만 사용 가능
            default -> Material.STONE;          // 안전한 기본값
        };

        // 1.16 이하 버전 호환성을 위한 대안
        if (position == 3) {
            try {
                material = Material.valueOf("COPPER_INGOT");
            } catch (IllegalArgumentException e) {
                material = Material.BRICK; // COPPER_INGOT이 없으면 BRICK 사용
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String rankColor = switch (position) {
            case 1 -> "§6§l";
            case 2 -> "§7§l";
            case 3 -> "§c§l";
            default -> "§f";
        };

        meta.setDisplayName(rankColor + position + "위 " + ranking.getPlayerName());
        meta.setLore(Arrays.asList(
                "§7점수: §f" + formatScore(ranking.getScore(), ranking.getType()),
                "§7순위: §f" + ranking.getRankPosition() + "위"
        ));
        item.setItemMeta(meta);

        return item;
    }


    private String formatScore(long score, RankingType type) {
        return switch (type) {
            case MONEY -> plugin.getEconomyManager().formatMoney(score) + "G";
            case PLAYTIME -> formatPlaytime(score);
            case DRAGON_KILLS -> score + "마리";
            case MARKET_SALES -> score + "건";
            case ATTENDANCE -> score + "일";
        };
    }

    private String formatPlaytime(long minutes) {
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return hours + "시간 " + remainingMinutes + "분";
    }
}
