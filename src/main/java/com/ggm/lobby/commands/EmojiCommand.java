package com.ggm.lobby.commands;

import com.ggm.lobby.GGMLobby;
import com.ggm.lobby.managers.ChatManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class EmojiCommand implements CommandExecutor {

    private final GGMLobby plugin;
    private final ChatManager chatManager;

    public EmojiCommand(GGMLobby plugin) {
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
        showEmojiList(player);
        return true;
    }

    private void showEmojiList(Player player) {
        Map<String, String> availableEmojis = chatManager.getAvailableEmojis(player);

        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l사용 가능한 이모티콘");
        player.sendMessage("");

        if (availableEmojis.isEmpty()) {
            player.sendMessage("§c사용 가능한 이모티콘이 없습니다!");
            player.sendMessage("§7상점에서 이모티콘 팩을 구매해보세요: §e/shop chat");
        } else {
            player.sendMessage("§a기본 이모티콘:");
            for (Map.Entry<String, String> emoji : availableEmojis.entrySet()) {
                if (isBasicEmoji(emoji.getKey())) {
                    player.sendMessage("§f" + emoji.getKey() + " §7→ " + emoji.getValue());
                }
            }

            // VIP 이모티콘이 있다면 표시
            boolean hasVipEmojis = availableEmojis.entrySet().stream()
                    .anyMatch(entry -> !isBasicEmoji(entry.getKey()));

            if (hasVipEmojis) {
                player.sendMessage("");
                player.sendMessage("§6VIP 이모티콘:");
                for (Map.Entry<String, String> emoji : availableEmojis.entrySet()) {
                    if (!isBasicEmoji(emoji.getKey())) {
                        player.sendMessage("§f" + emoji.getKey() + " §7→ " + emoji.getValue());
                    }
                }
            }
        }

        player.sendMessage("");
        player.sendMessage("§7사용법: §f채팅에서 :smile: 같은 코드 입력");
        player.sendMessage("§7추가 이모티콘: §e/shop chat");
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private boolean isBasicEmoji(String emojiCode) {
        return !emojiCode.equals(":rainbow:") && !emojiCode.equals(":unicorn:") &&
                !emojiCode.equals(":dragon:") && !emojiCode.equals(":magic:");
    }
}