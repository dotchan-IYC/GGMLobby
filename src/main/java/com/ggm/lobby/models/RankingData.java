package com.ggm.lobby.models;

import com.ggm.lobby.managers.RankingManager.RankingType;
import java.util.UUID;

public class RankingData {
    private final UUID playerId;
    private final String playerName;
    private final long score;
    private final int rankPosition;
    private final RankingType type;

    public RankingData(UUID playerId, String playerName, long score, int rankPosition, RankingType type) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.score = score;
        this.rankPosition = rankPosition;
        this.type = type;
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public long getScore() { return score; }
    public int getRankPosition() { return rankPosition; }
    public RankingType getType() { return type; }
}