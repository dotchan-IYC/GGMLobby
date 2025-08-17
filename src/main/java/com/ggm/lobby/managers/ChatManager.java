package com.ggm.lobby.managers;

import com.ggm.lobby.GGMLobby;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class ChatManager {

    private final GGMLobby plugin;
    private final DatabaseManager databaseManager;

    // 이모티콘 매핑
    private final Map<String, String> emojis = new HashMap<>();

    // 금지어 패턴
    private final List<Pattern> bannedPatterns = new ArrayList<>();

    public ChatManager(GGMLobby plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        initializeEmojis();
        initializeBannedWords();
    }

    private void initializeEmojis() {
        // 기본 이모티콘
        emojis.put(":smile:", "😀");
        emojis.put(":heart:", "❤️");
        emojis.put(":money:", "💰");
        emojis.put(":sword:", "⚔️");
        emojis.put(":star:", "⭐");
        emojis.put(":fire:", "🔥");
        emojis.put(":diamond:", "💎");
        emojis.put(":crown:", "👑");
        emojis.put(":rocket:", "🚀");
        emojis.put(":thumbsup:", "👍");

        // VIP 이모티콘 (별도 구매 필요)
        emojis.put(":rainbow:", "🌈");
        emojis.put(":unicorn:", "🦄");
        emojis.put(":dragon:", "🐉");
        emojis.put(":magic:", "✨");
    }

    private void initializeBannedWords() {
        // 기본 금지어 패턴
        bannedPatterns.add(Pattern.compile("\\b(바보|멍청이|바카|시발|좆)\\b", Pattern.CASE_INSENSITIVE));
        bannedPatterns.add(Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")); // IP 주소
        bannedPatterns.add(Pattern.compile("\\w+\\.(com|net|org|co\\.kr)")); // 도메인
    }

    /**
     * 채팅 메시지 처리
     */
    public String processMessage(Player player, String message) {
        // 1. 금지어 필터링
        String filteredMessage = filterBannedWords(message);

        // 2. 이모티콘 변환
        String emojiMessage = convertEmojis(player, filteredMessage);

        // 3. 멘션 처리
        String mentionMessage = processMentions(emojiMessage);

        return mentionMessage;
    }

    private String filterBannedWords(String message) {
        String filtered = message;
        for (Pattern pattern : bannedPatterns) {
            filtered = pattern.matcher(filtered).replaceAll("***");
        }
        return filtered;
    }

    private String convertEmojis(Player player, String message) {
        String converted = message;

        for (Map.Entry<String, String> emoji : emojis.entrySet()) {
            String emojiCode = emoji.getKey();
            String emojiChar = emoji.getValue();

            // VIP 이모티콘 확인
            if (isVipEmoji(emojiCode)) {
                // 플레이어가 이모티콘 팩을 구매했는지 확인
                boolean hasEmojiPack = plugin.getLobbyShopManager()
                        .hasItem(player.getUniqueId(), "emoji_pack").join();

                if (!hasEmojiPack) {
                    continue; // VIP 이모티콘 사용 불가
                }
            }

            converted = converted.replace(emojiCode, emojiChar);
        }

        return converted;
    }

    private String processMentions(String message) {
        // @플레이어명 패턴 찾기
        Pattern mentionPattern = Pattern.compile("@(\\w+)");
        return mentionPattern.matcher(message).replaceAll("§e@$1§r");
    }

    private boolean isVipEmoji(String emojiCode) {
        Set<String> vipEmojis = Set.of(":rainbow:", ":unicorn:", ":dragon:", ":magic:");
        return vipEmojis.contains(emojiCode);
    }

    /**
     * 플레이어 채팅 색상 조회
     */
    public CompletableFuture<String> getChatColor(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String sql = "SELECT chat_color FROM ggm_chat_settings WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, playerId.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("chat_color");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("채팅 색상 조회 실패: " + e.getMessage());
            }
            return "white";
        });
    }

    /**
     * 플레이어 VIP 레벨 조회
     */
    public CompletableFuture<Integer> getVipLevel(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String sql = "SELECT vip_level FROM ggm_chat_settings WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, playerId.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt("vip_level");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("VIP 레벨 조회 실패: " + e.getMessage());
            }
            return 0;
        });
    }

    /**
     * 이모티콘 목록 반환
     */
    public Map<String, String> getAvailableEmojis(Player player) {
        Map<String, String> available = new HashMap<>();

        for (Map.Entry<String, String> emoji : emojis.entrySet()) {
            if (isVipEmoji(emoji.getKey())) {
                // VIP 이모티콘은 구매 확인
                boolean hasEmojiPack = plugin.getLobbyShopManager()
                        .hasItem(player.getUniqueId(), "emoji_pack").join();
                if (hasEmojiPack) {
                    available.put(emoji.getKey(), emoji.getValue());
                }
            } else {
                // 기본 이모티콘
                available.put(emoji.getKey(), emoji.getValue());
            }
        }

        return available;
    }
}
