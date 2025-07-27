package com.ggm.lobby.listeners;

import com.ggm.lobby.GGMLobby;
import com.ggm.lobby.managers.ChatManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final GGMLobby plugin;
    private final ChatManager chatManager;

    public ChatListener(GGMLobby plugin) {
        this.plugin = plugin;
        this.chatManager = plugin.getChatManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // 채팅 메시지 처리
        String processedMessage = chatManager.processMessage(player, message);

        // 플레이어 VIP 레벨에 따른 채팅 색상 적용
        chatManager.getVipLevel(player.getUniqueId()).thenAccept(vipLevel -> {
            String chatColor = getChatColorByVipLevel(vipLevel);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // 채팅 포맷 설정
                String format = getFormattedMessage(player, processedMessage, chatColor, vipLevel);
                event.setFormat(format);
            });
        });
    }

    private String getChatColorByVipLevel(int vipLevel) {
        return switch (vipLevel) {
            case 1 -> "§6"; // 금색
            case 2 -> "§b"; // 하늘색
            case 3 -> "§d"; // 분홍색
            default -> "§f"; // 하얀색
        };
    }

    private String getFormattedMessage(Player player, String message, String chatColor, int vipLevel) {
        StringBuilder format = new StringBuilder();

        // VIP 표시
        if (vipLevel > 0) {
            format.append("§6[VIP").append(vipLevel).append("]§r ");
        }

        // 플레이어 이름
        format.append("§7").append(player.getName()).append("§7: ");

        // 메시지 색상 적용
        format.append(chatColor).append(message);

        return format.toString();
    }
}
