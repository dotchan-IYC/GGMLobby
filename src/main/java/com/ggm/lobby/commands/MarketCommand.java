package com.ggm.lobby.commands;

import com.ggm.lobby.GGMLobby;
import com.ggm.lobby.managers.MarketManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MarketCommand implements CommandExecutor {

    private final GGMLobby plugin;
    private final MarketManager marketManager;

    public MarketCommand(GGMLobby plugin) {
        this.plugin = plugin;
        this.marketManager = plugin.getMarketManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // 거래소 메인 GUI
            player.openInventory(marketManager.createMarketGUI(null, 0));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "sell":
            case "판매":
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /market sell <가격>");
                    return true;
                }
                sellItem(player, args[1]);
                break;
            case "my":
            case "내목록":
                player.openInventory(marketManager.createMyListingsGUI(player.getUniqueId()));
                break;
            case "search":
            case "검색":
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /market search <검색어>");
                    return true;
                }
                searchItems(player, args[1]);
                break;
            case "help":
            case "도움말":
                showMarketHelp(player);
                break;
            default:
                showMarketHelp(player);
                break;
        }

        return true;
    }

    private void sellItem(Player player, String priceStr) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§c손에 아이템을 들고 사용하세요!");
            return;
        }

        long price;
        try {
            price = Long.parseLong(priceStr);
            if (price < 100) {
                player.sendMessage("§c최소 판매 가격은 100G입니다!");
                return;
            }
            if (price > 1000000000L) {
                player.sendMessage("§c최대 판매 가격은 10억G입니다!");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c올바른 숫자를 입력하세요!");
            return;
        }

        // 아이템 등록
        marketManager.listItem(player.getUniqueId(), player.getName(), item, price)
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            // 인벤토리에서 아이템 제거
                            player.getInventory().setItemInMainHand(null);

                            player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                            player.sendMessage("§a§l📦 아이템 등록 완료!");
                            player.sendMessage("");
                            player.sendMessage("§7아이템: §f" + getItemDisplayName(item));
                            player.sendMessage("§7판매 가격: §6" + plugin.getEconomyManager().formatMoney(price) + "G");
                            player.sendMessage("§7수수료 (5%): §c" + plugin.getEconomyManager().formatMoney(price * 5 / 100) + "G");
                            player.sendMessage("§7실제 수령액: §a" + plugin.getEconomyManager().formatMoney(price * 95 / 100) + "G");
                            player.sendMessage("");
                            player.sendMessage("§7만료일: §f7일 후");
                            player.sendMessage("§7내 목록: §e/market my");
                            player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                            // 사운드 효과
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                        } else {
                            player.sendMessage("§c아이템 등록에 실패했습니다!");
                            player.sendMessage("§7• 판매 불가능한 아이템인지 확인하세요");
                            player.sendMessage("§7• 최대 10개까지만 동시 판매 가능합니다");
                        }
                    });
                });
    }

    private void searchItems(Player player, String keyword) {
        player.sendMessage("§e검색 기능은 추후 업데이트 예정입니다!");
        player.sendMessage("§7현재는 카테고리별 검색을 이용해주세요: §e/market");
    }

    private void showMarketHelp(Player player) {
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l📦 거래소 도움말");
        player.sendMessage("");
        player.sendMessage("§7/market §f- 거래소 메인");
        player.sendMessage("§7/market sell <가격> §f- 손에 든 아이템 판매");
        player.sendMessage("§7/market my §f- 내 판매 목록");
        player.sendMessage("§7/market search <검색어> §f- 아이템 검색");
        player.sendMessage("");
        player.sendMessage("§a§l거래소 규칙:");
        player.sendMessage("§7• 최소 판매가: §6100G");
        player.sendMessage("§7• 최대 동시 판매: §610개");
        player.sendMessage("§7• 판매 수수료: §c5%");
        player.sendMessage("§7• 아이템 보관 기간: §67일");
        player.sendMessage("");
        player.sendMessage("§c주의: §7만료된 아이템은 자동 삭제됩니다!");
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name().toLowerCase().replace("_", " ");
    }
}
