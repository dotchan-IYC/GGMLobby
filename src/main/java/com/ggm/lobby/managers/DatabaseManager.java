package com.ggm.lobby.managers;

import com.ggm.lobby.GGMLobby;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    private final GGMLobby plugin;
    private Connection connection;

    public DatabaseManager(GGMLobby plugin) {
        this.plugin = plugin;
        initializeDatabase();
        createTables();
    }

    // HikariCP 대신 기본 JDBC 연결 사용 (임시)
    private void initializeDatabase() {
        try {
            String host = plugin.getConfig().getString("database.host", "localhost");
            int port = plugin.getConfig().getInt("database.port", 3306);
            String database = plugin.getConfig().getString("database.name", "ggm_server");
            String username = plugin.getConfig().getString("database.username", "root");
            String password = plugin.getConfig().getString("database.password", "1224");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                    "?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true";

            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, username, password);

            plugin.getLogger().info("데이터베이스 연결 성공!");

        } catch (Exception e) {
            plugin.getLogger().severe("데이터베이스 연결 실패: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void createTables() {
        String[] tables = {
                // 출석 체크 테이블
                """
            CREATE TABLE IF NOT EXISTS ggm_attendance (
                uuid VARCHAR(36) PRIMARY KEY,
                player_name VARCHAR(16) NOT NULL,
                last_check_in DATE,
                consecutive_days INT DEFAULT 0,
                total_days INT DEFAULT 0,
                monthly_record TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """,

                // 랭킹 캐시 테이블
                """
            CREATE TABLE IF NOT EXISTS ggm_rankings (
                uuid VARCHAR(36) NOT NULL,
                ranking_type VARCHAR(20) NOT NULL,
                score BIGINT NOT NULL,
                rank_position INT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (uuid, ranking_type)
            )
            """,

                // 거래소 테이블
                """
            CREATE TABLE IF NOT EXISTS ggm_market_listings (
                listing_id VARCHAR(36) PRIMARY KEY,
                seller_uuid VARCHAR(36) NOT NULL,
                seller_name VARCHAR(16) NOT NULL,
                item_data TEXT NOT NULL,
                price BIGINT NOT NULL,
                category VARCHAR(20) NOT NULL,
                listed_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expiry_time TIMESTAMP NOT NULL,
                sold BOOLEAN DEFAULT FALSE,
                buyer_uuid VARCHAR(36),
                buyer_name VARCHAR(16),
                sold_time TIMESTAMP NULL
            )
            """,

                // 상점 구매 기록
                """
            CREATE TABLE IF NOT EXISTS ggm_shop_purchases (
                id INT AUTO_INCREMENT PRIMARY KEY,
                uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                item_id VARCHAR(50) NOT NULL,
                price BIGINT NOT NULL,
                expiry_time TIMESTAMP NULL,
                purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,

                // 채팅 설정
                """
            CREATE TABLE IF NOT EXISTS ggm_chat_settings (
                uuid VARCHAR(36) PRIMARY KEY,
                player_name VARCHAR(16) NOT NULL,
                chat_color VARCHAR(10) DEFAULT 'white',
                emoji_pack TEXT,
                vip_level INT DEFAULT 0,
                muted_until TIMESTAMP NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """
        };

        try {
            for (String sql : tables) {
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.executeUpdate();
                }
            }
            plugin.getLogger().info("데이터베이스 테이블 생성 완료");

        } catch (SQLException e) {
            plugin.getLogger().severe("테이블 생성 실패: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            initializeDatabase();
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("데이터베이스 연결 종료 실패: " + e.getMessage());
        }
    }
}