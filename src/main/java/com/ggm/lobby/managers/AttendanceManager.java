package com.ggm.lobby.managers;

import com.ggm.lobby.GGMLobby;
import com.ggm.lobby.models.AttendanceData;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AttendanceManager {

    private final GGMLobby plugin;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;

    // 출석 보상 설정
    private final Map<Integer, AttendanceReward> attendanceRewards = new HashMap<>();

    public AttendanceManager(GGMLobby plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.economyManager = plugin.getEconomyManager();
        initializeRewards();
    }

    private void initializeRewards() {
        attendanceRewards.put(1, new AttendanceReward(1000L, null, "§71일차 출석"));
        attendanceRewards.put(2, new AttendanceReward(1500L, Material.BOOK, "§72일차 출석"));
        attendanceRewards.put(3, new AttendanceReward(2000L, null, "§73일차 출석"));
        attendanceRewards.put(4, new AttendanceReward(2500L, Material.ENCHANTED_BOOK, "§74일차 출석"));
        attendanceRewards.put(5, new AttendanceReward(3000L, null, "§75일차 출석"));
        attendanceRewards.put(6, new AttendanceReward(3500L, Material.ENCHANTED_BOOK, "§76일차 출석"));
        attendanceRewards.put(7, new AttendanceReward(5000L, Material.NETHER_STAR, "§6§l7일차 연속 출석!"));
    }

    /**
     * 출석 체크 실행
     */
    public CompletableFuture<AttendanceResult> checkAttendance(UUID uuid, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            LocalDate today = LocalDate.now();

            try (Connection conn = databaseManager.getConnection()) {
                // 기존 출석 데이터 조회
                AttendanceData attendanceData = getAttendanceData(conn, uuid);

                if (attendanceData == null) {
                    // 신규 플레이어
                    attendanceData = new AttendanceData(uuid, playerName, null, 0, 0, new ArrayList<>());
                }

                // 오늘 이미 출석했는지 확인
                if (attendanceData.getLastCheckIn() != null &&
                        attendanceData.getLastCheckIn().equals(today)) {
                    return new AttendanceResult(false, "이미 오늘 출석체크를 하셨습니다!", 0L, null);
                }

                // 연속 출석일 계산
                int consecutiveDays = calculateConsecutiveDays(attendanceData.getLastCheckIn(), today);
                consecutiveDays = Math.min(consecutiveDays, 7); // 최대 7일

                // 출석 보상 계산
                AttendanceReward reward = attendanceRewards.get(consecutiveDays);
                if (reward == null) {
                    reward = attendanceRewards.get(7); // 7일 이후는 7일차 보상
                }

                // 출석 데이터 업데이트
                updateAttendanceData(conn, uuid, playerName, today, consecutiveDays,
                        attendanceData.getTotalDays() + 1);

                return new AttendanceResult(true,
                        "출석체크 완료! " + consecutiveDays + "일 연속 출석",
                        reward.money, reward.item);

            } catch (SQLException e) {
                plugin.getLogger().severe("출석체크 실패: " + e.getMessage());
                return new AttendanceResult(false, "출석체크 중 오류가 발생했습니다.", 0L, null);
            }
        });
    }

    private AttendanceData getAttendanceData(Connection conn, UUID uuid) throws SQLException {
        String sql = "SELECT * FROM ggm_attendance WHERE uuid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    LocalDate lastCheckIn = null;
                    if (rs.getDate("last_check_in") != null) {
                        lastCheckIn = rs.getDate("last_check_in").toLocalDate();
                    }

                    return new AttendanceData(
                            uuid,
                            rs.getString("player_name"),
                            lastCheckIn,
                            rs.getInt("consecutive_days"),
                            rs.getInt("total_days"),
                            parseMonthlyRecord(rs.getString("monthly_record"))
                    );
                }
            }
        }
        return null;
    }

    private int calculateConsecutiveDays(LocalDate lastCheckIn, LocalDate today) {
        if (lastCheckIn == null) {
            return 1; // 첫 출석
        }

        if (lastCheckIn.equals(today.minusDays(1))) {
            return 1; // 연속 출석 계속 (기존 연속일 + 1은 updateAttendanceData에서 처리)
        } else {
            return 1; // 연속 출석 끊김, 다시 시작
        }
    }

    private void updateAttendanceData(Connection conn, UUID uuid, String playerName,
                                      LocalDate today, int consecutiveDays, int totalDays) throws SQLException {
        String sql = """
            INSERT INTO ggm_attendance (uuid, player_name, last_check_in, consecutive_days, total_days)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            player_name = VALUES(player_name),
            last_check_in = VALUES(last_check_in),
            consecutive_days = VALUES(consecutive_days),
            total_days = VALUES(total_days)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setDate(3, java.sql.Date.valueOf(today));
            stmt.setInt(4, consecutiveDays);
            stmt.setInt(5, totalDays);
            stmt.executeUpdate();
        }
    }

    /**
     * 출석 달력 GUI 생성
     */
    public Inventory createAttendanceGUI(UUID uuid) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6§l출석 체크 달력");

        // 현재 월의 출석 현황 표시
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.withDayOfMonth(1);
        int daysInMonth = today.lengthOfMonth();

        // 출석 데이터 조회
        getAttendanceData(uuid).thenAccept(attendanceData -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // 달력 생성
                for (int day = 1; day <= Math.min(daysInMonth, 45); day++) {
                    LocalDate date = firstDay.plusDays(day - 1);
                    boolean attended = attendanceData != null &&
                            attendanceData.getMonthlyRecord().contains(date);

                    ItemStack dayItem;
                    if (date.equals(today)) {
                        // 오늘
                        dayItem = new ItemStack(Material.DIAMOND);
                    } else if (attended) {
                        // 출석한 날
                        dayItem = new ItemStack(Material.EMERALD);
                    } else if (date.isBefore(today)) {
                        // 출석하지 않은 과거
                        dayItem = new ItemStack(Material.REDSTONE);
                    } else {
                        // 미래
                        dayItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
                    }

                    ItemMeta meta = dayItem.getItemMeta();
                    meta.setDisplayName("§f" + day + "일");

                    List<String> lore = new ArrayList<>();
                    if (date.equals(today)) {
                        lore.add("§e§l오늘!");
                        lore.add("§7클릭하여 출석체크");
                    } else if (attended) {
                        lore.add("§a출석 완료");
                    } else if (date.isBefore(today)) {
                        lore.add("§c출석하지 않음");
                    } else {
                        lore.add("§7미래");
                    }
                    meta.setLore(lore);
                    dayItem.setItemMeta(meta);

                    gui.setItem(day - 1, dayItem);
                }

                // 출석 보상 정보 표시
                ItemStack rewardInfo = new ItemStack(Material.CHEST);
                ItemMeta rewardMeta = rewardInfo.getItemMeta();
                rewardMeta.setDisplayName("§6§l출석 보상");
                List<String> rewardLore = Arrays.asList(
                        "§71일: §61,000G",
                        "§72일: §61,500G §7+ 인첸트북",
                        "§73일: §62,000G",
                        "§74일: §62,500G §7+ 희귀 인첸트북",
                        "§75일: §63,000G",
                        "§76일: §63,500G §7+ 에픽 인첸트북",
                        "§6§l7일: §65,000G §7+ 특별 아이템"
                );
                rewardMeta.setLore(rewardLore);
                rewardInfo.setItemMeta(rewardMeta);
                gui.setItem(53, rewardInfo);
            });
        });

        return gui;
    }

    private CompletableFuture<AttendanceData> getAttendanceData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                return getAttendanceData(conn, uuid);
            } catch (SQLException e) {
                plugin.getLogger().severe("출석 데이터 조회 실패: " + e.getMessage());
                return null;
            }
        });
    }

    private List<LocalDate> parseMonthlyRecord(String record) {
        List<LocalDate> dates = new ArrayList<>();
        if (record != null && !record.trim().isEmpty()) {
            String[] dateStrings = record.split(",");
            for (String dateString : dateStrings) {
                try {
                    dates.add(LocalDate.parse(dateString.trim(), DateTimeFormatter.ISO_LOCAL_DATE));
                } catch (Exception e) {
                    // 파싱 실패 시 무시
                }
            }
        }
        return dates;
    }

    // 출석 보상 클래스
    private static class AttendanceReward {
        final long money;
        final Material item;
        final String title;

        AttendanceReward(long money, Material item, String title) {
            this.money = money;
            this.item = item;
            this.title = title;
        }
    }

    // 출석 결과 클래스
    public static class AttendanceResult {
        private final boolean success;
        private final String message;
        private final long rewardMoney;
        private final Material rewardItem;

        public AttendanceResult(boolean success, String message, long rewardMoney, Material rewardItem) {
            this.success = success;
            this.message = message;
            this.rewardMoney = rewardMoney;
            this.rewardItem = rewardItem;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public long getRewardMoney() { return rewardMoney; }
        public Material getRewardItem() { return rewardItem; }
    }
}