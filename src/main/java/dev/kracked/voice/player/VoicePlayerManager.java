package dev.kracked.voice.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all connected voice players
 */
public class VoicePlayerManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VoicePlayerManager.class);
    
    // Player ID -> VoicePlayer
    private final Map<UUID, VoicePlayer> players = new ConcurrentHashMap<>();
    
    // Address -> Player ID (for UDP packet routing)
    private final Map<InetSocketAddress, UUID> addressMap = new ConcurrentHashMap<>();
    
    /**
     * Register a new player
     */
    public VoicePlayer registerPlayer(UUID playerId, String playerName) {
        VoicePlayer player = new VoicePlayer(playerId, playerName);
        players.put(playerId, player);
        LOGGER.info("Registered player: {}", playerName);
        return player;
    }
    
    /**
     * Unregister a player
     */
    public void unregisterPlayer(UUID playerId) {
        VoicePlayer player = players.remove(playerId);
        if (player != null && player.getAddress() != null) {
            addressMap.remove(player.getAddress());
            LOGGER.info("Unregistered player: {}", player.getPlayerName());
        }
    }
    
    /**
     * Get player by ID
     */
    public VoicePlayer getPlayer(UUID playerId) {
        return players.get(playerId);
    }
    
    /**
     * Get player by network address
     */
    public VoicePlayer getPlayerByAddress(InetSocketAddress address) {
        UUID playerId = addressMap.get(address);
        return playerId != null ? players.get(playerId) : null;
    }
    
    /**
     * Associate address with player
     */
    public void setPlayerAddress(UUID playerId, InetSocketAddress address) {
        VoicePlayer player = players.get(playerId);
        if (player != null) {
            // Remove old address mapping
            if (player.getAddress() != null) {
                addressMap.remove(player.getAddress());
            }
            
            player.setAddress(address);
            addressMap.put(address, playerId);
        }
    }
    
    /**
     * Update player position
     */
    public void updatePosition(UUID playerId, double x, double y, double z, String worldId) {
        VoicePlayer player = players.get(playerId);
        if (player != null) {
            player.updatePosition(x, y, z, worldId);
        }
    }
    
    /**
     * Get all players in a specific world
     */
    public List<VoicePlayer> getPlayersInWorld(String worldId) {
        List<VoicePlayer> result = new ArrayList<>();
        for (VoicePlayer player : players.values()) {
            if (player.isConnected() && worldId.equals(player.getWorldId())) {
                result.add(player);
            }
        }
        return result;
    }
    
    /**
     * Get all connected players
     */
    public Collection<VoicePlayer> getAllPlayers() {
        return players.values();
    }
    
    /**
     * Get count of connected players
     */
    public int getConnectedCount() {
        int count = 0;
        for (VoicePlayer player : players.values()) {
            if (player.isConnected()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Disconnect all players
     */
    public void disconnectAll() {
        for (VoicePlayer player : players.values()) {
            player.setConnected(false);
        }
        players.clear();
        addressMap.clear();
        LOGGER.info("All players disconnected");
    }
    
    /**
     * Clean up timed out connections
     */
    public void cleanupTimeouts() {
        Iterator<Map.Entry<UUID, VoicePlayer>> it = players.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, VoicePlayer> entry = it.next();
            VoicePlayer player = entry.getValue();
            
            if (player.isConnected() && player.isTimedOut()) {
                LOGGER.info("Player timed out: {}", player.getPlayerName());
                player.setConnected(false);
                if (player.getAddress() != null) {
                    addressMap.remove(player.getAddress());
                }
            }
        }
    }
}
