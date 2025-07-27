package com.ggm.lobby.commands;

import com.ggm.lobby.GGMLobby;
import com.ggm.lobby.managers.AttendanceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AttendanceCommand implements CommandExecutor {

    private final GGMLobby plugin;
    private final AttendanceManager attendanceManager;

    public AttendanceCommand(GGMLobby plugin) {
        this.plugin = plugin;
        this.attendanceManager = plugin.getAttendanceManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // 출석 달력 GUI 열기
            player.openInventory(attendanceManager.createAttendanceGUI(player.getUniqueId()));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "check":
            case "출석":
                checkAttendance(player);
                break;
            case "calendar":
            case "달력":
                player.openInventory(attendanceManager.createAttendanceGUI(player.getUniqueId()));
                break;
            default:
                showAttendanceHelp(player);
                break;
        }

        return true;
    }

    private void checkAttendance(Player player) {
        attendanceManager.checkAttendance(player.getUniqueId(), player.getName())
                .thenAccept(result -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        player.sendMessage("§e§l📅 출석 체크 결과");
                        player.sendMessage("");

                        if (result.isSuccess()) {
                            player.sendMessage("§a✓ " + result.getMessage());

                            if (result.getRewardMoney() > 0) {
                                plugin.getEconomyManager().addMoney(player.getUniqueId(), result.getRewardMoney());
                                player.sendMessage("§7보상: §6" + plugin.getEconomyManager().formatMoney(result.getRewardMoney()) + "G");
                            }

                            if (result.getRewardItem() != null) {
                                // 아이템 보상 지급 로직 (추후 구현)
                                player.sendMessage("§7보상: §b특별 아이템");
                            }

                            // 사운드 효과
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        } else {
                            player.sendMessage("§c✗ " + result.getMessage());
                        }

                        player.sendMessage("");
                        player.sendMessage("§7달력 확인: §e/attendance calendar");
                        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    });
                });
    }

    private void showAttendanceHelp(Player player) {
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l📅 출석 체크 도움말");
        player.sendMessage("");
        player.sendMessage("§7/attendance §f- 출석 달력 열기");
        player.sendMessage("§7/attendance check §f- 출석 체크");
        player.sendMessage("§7/attendance calendar §f- 출석 달력 보기");
        player.sendMessage("");
        player.sendMessage("§a§l연속 출석 보상:");
        player.sendMessage("§71~7일: §6점점 증가하는 G 보상");
        player.sendMessage("§7특정일: §b인첸트북 추가 보상");
        player.sendMessage("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}