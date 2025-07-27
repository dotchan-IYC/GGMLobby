package com.ggm.lobby;

import com.ggm.lobby.commands.*;
import com.ggm.lobby.listeners.*;
import com.ggm.lobby.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class GGMLobby extends JavaPlugin {

    private static GGMLobby instance;

    // 매니저들
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private AttendanceManager attendanceManager;
    private RankingManager rankingManager;
    private MarketManager marketManager;
    private LobbyShopManager lobbyShopManager;
    private ChatManager chatManager;

    @Override
    public void onEnable() {
        instance = this;

        // 설정 파일 생성
        saveDefaultConfig();

        try {
            // 매니저 초기화
            initializeManagers();

            // 명령어 등록
            registerCommands();

            // 리스너 등록
            registerListeners();

            // 스케줄러 시작
            startSchedulers();

            getLogger().info("GGMLobby 플러그인이 활성화되었습니다!");
            getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            getLogger().info("§a✓ 출석 체크 시스템 활성화");
            getLogger().info("§a✓ 랭킹 시스템 활성화");
            getLogger().info("§a✓ 거래소 시스템 활성화");
            getLogger().info("§a✓ 로비 상점 시스템 활성화");
            getLogger().info("§a✓ 채팅 강화 시스템 활성화");
            getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            getLogger().severe("플러그인 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        try {
            // 데이터베이스 연결 종료
            if (databaseManager != null) {
                databaseManager.closeConnection();
            }

            getLogger().info("GGMLobby 플러그인이 비활성화되었습니다!");

        } catch (Exception e) {
            getLogger().warning("플러그인 종료 중 오류: " + e.getMessage());
        }
    }

    private void initializeManagers() {
        try {
            // 데이터베이스 매니저
            databaseManager = new DatabaseManager(this);
            getLogger().info("✓ 데이터베이스 매니저 초기화 완료");

            // 경제 매니저 (GGMCore 연동)
            economyManager = new EconomyManager(this);
            getLogger().info("✓ 경제 매니저 초기화 완료");

            // 출석 매니저
            attendanceManager = new AttendanceManager(this);
            getLogger().info("✓ 출석 체크 매니저 초기화 완료");

            // 랭킹 매니저
            rankingManager = new RankingManager(this);
            getLogger().info("✓ 랭킹 매니저 초기화 완료");

            // 거래소 매니저
            marketManager = new MarketManager(this);
            getLogger().info("✓ 거래소 매니저 초기화 완료");

            // 로비 상점 매니저
            lobbyShopManager = new LobbyShopManager(this);
            getLogger().info("✓ 로비 상점 매니저 초기화 완료");

            // 채팅 매니저
            chatManager = new ChatManager(this);
            getLogger().info("✓ 채팅 매니저 초기화 완료");

        } catch (Exception e) {
            getLogger().severe("매니저 초기화 중 오류: " + e.getMessage());
            throw e;
        }
    }

    private void registerCommands() {
        try {
            getCommand("attendance").setExecutor(new AttendanceCommand(this));
            getCommand("ranking").setExecutor(new RankingCommand(this));
            getCommand("market").setExecutor(new MarketCommand(this));
            getCommand("shop").setExecutor(new ShopCommand(this));
            getCommand("chat").setExecutor(new ChatCommand(this));
            getCommand("emoji").setExecutor(new EmojiCommand(this));
            getCommand("lobbyadmin").setExecutor(new LobbyAdminCommand(this));

            getLogger().info("✓ 명령어 등록 완료");

        } catch (Exception e) {
            getLogger().severe("명령어 등록 중 오류: " + e.getMessage());
        }
    }

    private void registerListeners() {
        try {
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            getServer().getPluginManager().registerEvents(new ChatListener(this), this);
            getServer().getPluginManager().registerEvents(new GUIListener(this), this);

            getLogger().info("✓ 이벤트 리스너 등록 완료");

        } catch (Exception e) {
            getLogger().severe("리스너 등록 중 오류: " + e.getMessage());
        }
    }

    private void startSchedulers() {
        // 랭킹 업데이트 (5분마다)
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            rankingManager.updateAllRankings();
        }, 20L * 60 * 5, 20L * 60 * 5);

        // 만료된 거래소 아이템 정리 (1시간마다)
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            marketManager.cleanupExpiredListings();
        }, 20L * 60 * 60, 20L * 60 * 60);

        getLogger().info("✓ 스케줄러 시작 완료");
    }

    // Getter 메서드들
    public static GGMLobby getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public AttendanceManager getAttendanceManager() {
        return attendanceManager;
    }

    public RankingManager getRankingManager() {
        return rankingManager;
    }

    public MarketManager getMarketManager() {
        return marketManager;
    }

    public LobbyShopManager getLobbyShopManager() {
        return lobbyShopManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }
}
