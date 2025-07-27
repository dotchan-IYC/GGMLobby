package com.ggm.lobby.managers;

import com.ggm.lobby.GGMLobby;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EconomyManager {

    private final GGMLobby plugin;
    private final DatabaseManager databaseManager;

    public EconomyManager(GGMLobby plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * 플레이어의 G 잔액 조회
     */
    public CompletableFuture<Long> getMoney(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String sql = "SELECT money FROM ggm_players WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong("money");
                        }
                    }
                }
                return 0L;
            } catch (SQLException e) {
                plugin.getLogger().severe("G 조회 실패: " + e.getMessage());
                return 0L;
            }
        });
    }

    /**
     * 플레이어에게 G 지급
     */
    public CompletableFuture<Boolean> addMoney(UUID uuid, long amount) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String sql = """
                    INSERT INTO ggm_players (uuid, money) VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE money = money + VALUES(money)
                    """;
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setLong(2, amount);
                    return stmt.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("G 지급 실패: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * 플레이어의 G 차감
     */
    public CompletableFuture<Boolean> removeMoney(UUID uuid, long amount) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                // 잔액 확인
                String checkSql = "SELECT money FROM ggm_players WHERE uuid = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, uuid.toString());
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            long currentMoney = rs.getLong("money");
                            if (currentMoney < amount) {
                                return false; // 잔액 부족
                            }
                        } else {
                            return false; // 플레이어 없음
                        }
                    }
                }

                // G 차감
                String updateSql = "UPDATE ggm_players SET money = money - ? WHERE uuid = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setLong(1, amount);
                    updateStmt.setString(2, uuid.toString());
                    return updateStmt.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("G 차감 실패: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * 금액 포맷팅
     */
    public String formatMoney(long amount) {
        if (amount >= 1000000000L) {
            return String.format("%.1fB", amount / 1000000000.0);
        } else if (amount >= 1000000L) {
            return String.format("%.1fM", amount / 1000000.0);
        } else if (amount >= 1000L) {
            return String.format("%.1fK", amount / 1000.0);
        } else {
            return String.valueOf(amount);
        }
    }
}