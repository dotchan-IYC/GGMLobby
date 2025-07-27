package com.ggm.lobby.commands;

import com.ggm.lobby.GGMLobby;
import com.ggm.lobby.managers.ChatManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatCommand implements CommandExecutor {

    private final GGMLobby plugin;
    private final ChatManager chatManager;

    public ChatCommand(GGMLobby plugin) {
        this.plugin = plugin;
        this.chatManager = plugin.getChatManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showChatHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "global":
            case "전체":
                player.sendMessage("§a전체 채팅으로 변경했습니다!");
                break;
            case "lobby":
            case "로비":
                player.sendMessage("§a로비 채팅으로 변경했습니다!");
                break;
            case "trade":
            case "거래":
                player.sendMessage("§a거래 채팅으로 변경했습니다!");
                break;
            case "help":
            case "도움말":
                showChatHelp(player);
                break;
            case "color":
            case "색상":
                showChatColors(player);
                break;
            default:
                showChatHelp(player);
                break;
        }

        return true;
    }

    private void showChatHelp(Player player) {
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l💬 채팅 시스템 도움말");
        player.sendMessage("");
        player.sendMessage("§7/chat global §f- 전체 서버 채팅");
        player.sendMessage("§7/chat lobby §f- 로비 전용 채팅");
        player.sendMessage("§7/chat trade §f- 거래 채팅");
        player.sendMessage("§7/chat color §f- 채팅 색상 정보");
        player.sendMessage("");
        player.sendMessage("§a§l채팅 기능:");
        player.sendMessage("§7• §e이모티콘: §f:smile: :heart: :money:");
        player.sendMessage("§7• §e멘션: §f@플레이어명");
        player.sendMessage("§7• §e채팅 색상: §f상점에서 구매 가능");
        player.sendMessage("");
        player.sendMessage("§c주의: §7욕설, 광고, 스팸은 자동 차단됩니다!");
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void showChatColors(Player player) {
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l🎨 채팅 색상 정보");
        player.sendMessage("");
        player.sendMessage("§f일반: §f하얀색 (기본)");
        player.sendMessage("§6VIP1: §6금색 (상점 구매)");
        player.sendMessage("§bVIP2: §b하늘색 (상점 구매)");
        player.sendMessage("§dVIP3: §d분홍색 (상점 구매)");
        player.sendMessage("");
        player.sendMessage("§7구매 방법: §e/shop chat");
        player.sendMessage("§7가격: §65,000G §7(30일)");
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}