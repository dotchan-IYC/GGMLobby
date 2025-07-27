package com.ggm.lobby.models;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class AttendanceData {
    private final UUID playerId;
    private final String playerName;
    private final LocalDate lastCheckIn;
    private final int consecutiveDays;
    private final int totalDays;
    private final List<LocalDate> monthlyRecord;

    public AttendanceData(UUID playerId, String playerName, LocalDate lastCheckIn,
                          int consecutiveDays, int totalDays, List<LocalDate> monthlyRecord) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.lastCheckIn = lastCheckIn;
        this.consecutiveDays = consecutiveDays;
        this.totalDays = totalDays;
        this.monthlyRecord = monthlyRecord;
    }

    // Getters
    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public LocalDate getLastCheckIn() { return lastCheckIn; }
    public int getConsecutiveDays() { return consecutiveDays; }
    public int getTotalDays() { return totalDays; }
    public List<LocalDate> getMonthlyRecord() { return monthlyRecord; }
}