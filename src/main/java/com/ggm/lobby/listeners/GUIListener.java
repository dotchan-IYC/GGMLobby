package com.ggm.lobby.listeners;

import com.ggm.lobby.GGMLobby;
import com.ggm.lobby.managers.*;
import com.ggm.lobby.models.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final GGMLobby plugin;

    public GUIListener(GGMLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // 로비 관련 GUI 확인
        if (title.contains("출석") || title.contains("랭킹") ||
                title.contains("거래소") || title.contains("상점")) {

            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null) return;

            // GUI 타입별 처리
            if (title.contains("출석")) {
                handleAttendanceGUI(player, clickedItem, event.getSlot());
            } else if (title.contains("랭킹")) {
                handleRankingGUI(player, clickedItem, title);
            } else if (title.contains("거래소")) {
                handleMarketGUI(player, clickedItem, title);
            } else if (title.contains("상점")) {
                handleShopGUI(player, clickedItem, title);
            }
        }
    }

    private void handleAttendanceGUI(Player player, ItemStack item, int slot) {
        // 오늘 날짜 슬롯인지 확인하고 출석 체크 실행
        if (item.getType().name().contains("DIAMOND")) {
            plugin.getAttendanceManager().checkAttendance(player.getUniqueId(), player.getName())
                    .thenAccept(result -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            player.closeInventory();

                            if (result.isSuccess()) {
                                player.sendMessage("§a✓ " + result.getMessage());

                                if (result.getRewardMoney() > 0) {
                                    plugin.getEconomyManager().addMoney(player.getUniqueId(), result.getRewardMoney());
                                    player.sendMessage("§7보상: §6" + plugin.getEconomyManager().formatMoney(result.getRewardMoney()) + "G");
                                }

                                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                            } else {
                                player.sendMessage("§c" + result.getMessage());
                            }
                        });
                    });
        }
    }

    private void handleRankingGUI(Player player, ItemStack item, String title) {
        // 랭킹 타입 변경 처리
        String itemName = item.getItemMeta().getDisplayName();

        for (RankingManager.RankingType type : RankingManager.RankingType.values()) {
            if (itemName.contains(type.getDisplayName())) {
                player.openInventory(plugin.getRankingManager().createRankingGUI(type));
                break;
            }
        }
    }

    private void handleMarketGUI(Player player, ItemStack item, String title) {
        // 거래소 카테고리 변경 또는 아이템 구매 처리
        String itemName = item.getItemMeta().getDisplayName();

        // 카테고리 버튼 클릭 확인
        for (MarketManager.MarketCategory category : MarketManager.MarketCategory.values()) {
            if (itemName.contains(category.getDisplayName().replaceAll("§.", ""))) {
                player.openInventory(plugin.getMarketManager().createMarketGUI(category, 0));
                return;
            }
        }

        // 아이템 구매 처리 (구현 필요)
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            // 거래소 아이템인 경우 구매 처리
            player.sendMessage("§e구매 기능은 개발 중입니다!");
        }
    }

    private void handleShopGUI(Player player, ItemStack item, String title) {
        // 상점 카테고리 변경 또는 아이템 구매 처리
        String itemName = item.getItemMeta().getDisplayName();

        // 카테고리 버튼 클릭 확인
        for (LobbyShopManager.ShopCategory category : LobbyShopManager.ShopCategory.values()) {
            if (itemName.contains(category.getDisplayName().replaceAll("§.", ""))) {
                player.openInventory(plugin.getLobbyShopManager().createShopGUI(category));
                return;
            }
        }

        // 아이템 구매 처리
        for (ShopItem shopItem : plugin.getLobbyShopManager().getAllItems()) {
            if (itemName.contains(shopItem.getName().replaceAll("§.", ""))) {
                purchaseShopItem(player, shopItem);
                break;
            }
        }
    }

    private void purchaseShopItem(Player player, ShopItem item) {
        // 잔액 확인
        plugin.getEconomyManager().getMoney(player.getUniqueId()).thenAccept(money -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (money < item.getPrice()) {
                    player.sendMessage("§c잔액이 부족합니다! (필요: " +
                            plugin.getEconomyManager().formatMoney(item.getPrice()) + "G)");
                    return;
                }

                // 구매 처리
                plugin.getLobbyShopManager().purchaseItem(player.getUniqueId(), player.getName(), item.getId())
                        .thenAccept(success -> {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                player.closeInventory();

                                if (success) {
                                    player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                    player.sendMessage("§a§l구매 완료!");
                                    player.sendMessage("");
                                    player.sendMessage("§7상품: §f" + item.getName());
                                    player.sendMessage("§7가격: §6" + plugin.getEconomyManager().formatMoney(item.getPrice()) + "G");
                                    player.sendMessage("");
                                    player.sendMessage("§a구매해주셔서 감사합니다!");
                                    player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                                } else {
                                    player.sendMessage("§c구매에 실패했습니다!");
                                }
                            });
                        });
            });
        });
    }
}