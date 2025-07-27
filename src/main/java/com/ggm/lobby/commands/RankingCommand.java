package com.ggm.lobby.commands;

import com.ggm.lobby.GGMLobby;
import com.ggm.lobby.managers.RankingManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankingCommand implements CommandExecutor {

    private final GGMLobby plugin;
    private final RankingManager rankingManager;

    public RankingCommand(GGMLobby plugin) {
        this.plugin = plugin;
        this.rankingManager = plugin.getRankingManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // 기본 랭킹 GUI (G 랭킹)
            player.openInventory(rankingManager.createRankingGUI(RankingManager.RankingType.MONEY));
            return true;
        }

        String typeStr = args[0].toLowerCase();
        RankingManager.RankingType rankingType = null;

        switch (typeStr) {
            case "money":
            case "g":
            case "돈":
                rankingType = RankingManager.RankingType.MONEY;
                break;
            case "playtime":
            case "시간":
                rankingType = RankingManager.RankingType.PLAYTIME;
                break;
            case "dragon":
            case "드래곤":
                rankingType = RankingManager.RankingType.DRAGON_KILLS;
                break;
            case "market":
            case "거래소":
                rankingType = RankingManager.RankingType.MARKET_SALES;
                break;
            case "attendance":
            case "출석":
                rankingType = RankingManager.RankingType.ATTENDANCE;
                break;
            case "my":
            case "내순위":
                showMyRankings(player);
                return true;
            default:
                showRankingHelp(player);
                return true;
        }

        if (rankingType != null) {
            player.openInventory(rankingManager.createRankingGUI(rankingType));
        }

        return true;
    }

    private void showMyRankings(Player player) {
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l🏆 내 순위 정보");
        player.sendMessage("");

        for (RankingManager.RankingType type : RankingManager.RankingType.values()) {
            rankingManager.getPlayerRank(player.getUniqueId(), type).thenAccept(ranking -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (ranking != null) {
                        player.sendMessage(type.getEmoji() + " §f" + type.getDisplayName() +
                                ": §6" + ranking.getRankPosition() + "위 §7(점수: " + ranking.getScore() + ")");
                    } else {
                        player.sendMessage(type.getEmoji() + " §f" + type.getDisplayName() +
                                ": §7순위 없음");
                    }
                });
            });
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("");
            player.sendMessage("§7상세 랭킹: §e/ranking <타입>");
            player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }, 20L);
    }

    private void showRankingHelp(Player player) {
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l🏆 랭킹 시스템 도움말");
        player.sendMessage("");
        player.sendMessage("§7/ranking §f- 전체 랭킹 GUI");
        player.sendMessage("§7/ranking money §f- G 랭킹");
        player.sendMessage("§7/ranking playtime §f- 플레이 시간 랭킹");
        player.sendMessage("§7/ranking dragon §f- 드래곤 처치 랭킹");
        player.sendMessage("§7/ranking market §f- 거래소 판매 랭킹");
        player.sendMessage("§7/ranking attendance §f- 출석 랭킹");
        player.sendMessage("§7/ranking my §f- 내 순위 확인");
        player.sendMessage("");
        player.sendMessage("§a§l랭킹 보상:");
        player.sendMessage("§71위: §650,000G §7+ 특별 칭호");
        player.sendMessage("§72~3위: §630,000G §7+ 칭호");
        player.sendMessage("§74~10위: §615,000G");
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}