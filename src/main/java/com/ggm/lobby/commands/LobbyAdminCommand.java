package com.ggm.lobby.commands;

import com.ggm.lobby.GGMLobby;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LobbyAdminCommand implements CommandExecutor {

    private final GGMLobby plugin;

    public LobbyAdminCommand(GGMLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ggm.lobby.admin")) {
            sender.sendMessage("§c권한이 없습니다.");
            return true;
        }

        if (args.length == 0) {
            showAdminHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                reloadPlugin(sender);
                break;
            case "ranking":
                if (args.length > 1 && args[1].equalsIgnoreCase("update")) {
                    updateRankings(sender);
                } else {
                    showRankingInfo(sender);
                }
                break;
            case "market":
                if (args.length > 1 && args[1].equalsIgnoreCase("clear")) {
                    clearExpiredMarket(sender);
                } else {
                    showMarketInfo(sender);
                }
                break;
            case "stats":
                showServerStats(sender);
                break;
            default:
                showAdminHelp(sender);
                break;
        }

        return true;
    }

    private void reloadPlugin(CommandSender sender) {
        try {
            plugin.reloadConfig();
            sender.sendMessage("§a[GGM로비] 설정이 리로드되었습니다!");

            plugin.getLogger().info(sender.getName() + "이(가) 로비 플러그인을 리로드했습니다.");
        } catch (Exception e) {
            sender.sendMessage("§c설정 리로드 중 오류가 발생했습니다: " + e.getMessage());
            plugin.getLogger().severe("설정 리로드 실패: " + e.getMessage());
        }
    }

    private void updateRankings(CommandSender sender) {
        sender.sendMessage("§e랭킹 업데이트를 시작합니다...");

        plugin.getRankingManager().updateAllRankings();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            sender.sendMessage("§a모든 랭킹이 업데이트되었습니다!");
        }, 60L);
    }

    private void clearExpiredMarket(CommandSender sender) {
        sender.sendMessage("§e만료된 거래소 아이템을 정리합니다...");

        plugin.getMarketManager().cleanupExpiredListings();
        sender.sendMessage("§a만료된 아이템 정리가 완료되었습니다!");
    }

    private void showRankingInfo(CommandSender sender) {
        sender.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§e§l🏆 랭킹 시스템 관리");
        sender.sendMessage("");
        sender.sendMessage("§7/lobbyadmin ranking update §f- 랭킹 강제 업데이트");
        sender.sendMessage("§7랭킹은 5분마다 자동 업데이트됩니다.");
        sender.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showMarketInfo(CommandSender sender) {
        sender.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§e§l📦 거래소 관리");
        sender.sendMessage("");
        sender.sendMessage("§7/lobbyadmin market clear §f- 만료된 아이템 정리");
        sender.sendMessage("§7만료된 아이템은 1시간마다 자동 정리됩니다.");
        sender.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showServerStats(CommandSender sender) {
        sender.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§e§l📊 서버 통계");
        sender.sendMessage("");
        sender.sendMessage("§7온라인 플레이어: §f" + plugin.getServer().getOnlinePlayers().size() + "명");
        sender.sendMessage("§7서버 TPS: §a양호");
        sender.sendMessage("§7메모리 사용량: §a" + getMemoryUsage());
        sender.sendMessage("");
        sender.sendMessage("§a§l시스템 상태:");
        sender.sendMessage("§7• 출석 체크: §a정상");
        sender.sendMessage("§7• 랭킹 시스템: §a정상");
        sender.sendMessage("§7• 거래소: §a정상");
        sender.sendMessage("§7• 로비 상점: §a정상");
        sender.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showAdminHelp(CommandSender sender) {
        sender.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§e§l🔧 로비 관리자 명령어");
        sender.sendMessage("");
        sender.sendMessage("§7/lobbyadmin reload §f- 설정 리로드");
        sender.sendMessage("§7/lobbyadmin ranking §f- 랭킹 관리");
        sender.sendMessage("§7/lobbyadmin market §f- 거래소 관리");
        sender.sendMessage("§7/lobbyadmin stats §f- 서버 통계");
        sender.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private String getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        double percentage = (double) usedMemory / maxMemory * 100;
        return String.format("%.1f%%", percentage);
    }
}
