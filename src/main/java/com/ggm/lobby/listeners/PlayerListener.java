package com.ggm.lobby.listeners;

import com.ggm.lobby.GGMLobby;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final GGMLobby plugin;

    public PlayerListener(GGMLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 환영 메시지
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§e§l🎮 GGM 로비에 오신 것을 환영합니다!");
            player.sendMessage("");
            player.sendMessage("§a§l이용 가능한 기능:");
            player.sendMessage("§7• §e📅 출석 체크 §7- 매일 보상 획득");
            player.sendMessage("§7• §e🏆 랭킹 시스템 §7- 다양한 순위 경쟁");
            player.sendMessage("§7• §e📦 플레이어 거래소 §7- 자유로운 아이템 거래");
            player.sendMessage("§7• §e🏪 로비 상점 §7- 특별한 아이템 구매");
            player.sendMessage("");
            player.sendMessage("§b§l주요 명령어:");
            player.sendMessage("§7/attendance §f- 출석체크  §7/ranking §f- 랭킹확인");
            player.sendMessage("§7/market §f- 거래소  §7/shop §f- 상점");
            player.sendMessage("");
            player.sendMessage("§7즐거운 시간 되세요! §e❤️");
            player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }, 20L);

        // 출석 체크 알림
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("§e💡 §f오늘 출석체크를 하셨나요? §e/attendance §f로 확인해보세요!");
        }, 100L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 플레이어 퇴장 시 필요한 정리 작업
        Player player = event.getPlayer();
        plugin.getLogger().info(player.getName() + "이(가) 로비를 떠났습니다.");
    }
}
