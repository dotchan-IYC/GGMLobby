package com.ggm.lobby.commands;

import com.ggm.lobby.GGMLobby;
import com.ggm.lobby.managers.LobbyShopManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final GGMLobby plugin;
    private final LobbyShopManager shopManager;

    public ShopCommand(GGMLobby plugin) {
        this.plugin = plugin;
        this.shopManager = plugin.getLobbyShopManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // 상점 메인 GUI
            player.openInventory(shopManager.createShopGUI(null));
            return true;
        }

        String categoryStr = args[0].toLowerCase();
        LobbyShopManager.ShopCategory category = null;

        switch (categoryStr) {
            case "cosmetics":
            case "코스메틱":
                category = LobbyShopManager.ShopCategory.COSMETICS;
                break;
            case "chat":
            case "채팅":
                category = LobbyShopManager.ShopCategory.CHAT;
                break;
            case "utilities":
            case "편의":
                category = LobbyShopManager.ShopCategory.UTILITIES;
                break;
            case "boosts":
            case "부스터":
                category = LobbyShopManager.ShopCategory.BOOSTS;
                break;
            case "special":
            case "특별":
                category = LobbyShopManager.ShopCategory.SPECIAL;
                break;
            case "my":
            case "내구매":
                showMyPurchases(player);
                return true;
            case "help":
            case "도움말":
                showShopHelp(player);
                return true;
            default:
                showShopHelp(player);
                return true;
        }

        if (category != null) {
            player.openInventory(shopManager.createShopGUI(category));
        }

        return true;
    }

    private void showMyPurchases(Player player) {
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l🛍️ 내 구매 목록");
        player.sendMessage("");

        // 주요 아이템들의 소유 여부 확인
        String[] items = {"heart_particle", "rich_title", "chat_color", "emoji_pack",
                "storage_expand", "money_boost", "dragon_boost"};

        for (String itemId : items) {
            shopManager.hasItem(player.getUniqueId(), itemId).thenAccept(hasItem -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (hasItem) {
                        var item = shopManager.getShopItem(itemId);
                        if (item != null) {
                            player.sendMessage("§a✓ " + item.getName());
                        }
                    }
                });
            });
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("");
            player.sendMessage("§7상점 이용: §e/shop");
            player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }, 40L);
    }

    private void showShopHelp(Player player) {
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l🏪 로비 상점 도움말");
        player.sendMessage("");
        player.sendMessage("§7/shop §f- 상점 메인");
        player.sendMessage("§7/shop cosmetics §f- 코스메틱 상품");
        player.sendMessage("§7/shop chat §f- 채팅 강화 상품");
        player.sendMessage("§7/shop utilities §f- 편의 기능 상품");
        player.sendMessage("§7/shop boosts §f- 부스터 상품");
        player.sendMessage("§7/shop my §f- 내 구매 목록");
        player.sendMessage("");
        player.sendMessage("§a§l인기 상품:");
        player.sendMessage("§7• §6💰 G 부스터 §7- 24시간 2배 획득");
        player.sendMessage("§7• §e😀 이모티콘 팩 §7- 20개 추가 이모티콘");
        player.sendMessage("§7• §d❤️ 하트 파티클 §7- 30일간 특수 효과");
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}