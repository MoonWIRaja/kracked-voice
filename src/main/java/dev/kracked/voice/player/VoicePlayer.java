package dev.kracked.voice.player;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Represents a player connected to voice chat
 */
public class VoicePlayer {
    
    private final UUID playerId;
    private final String playerName;
    private InetSocketAddress address;
    private String worldId;
    
    // Position
    private double x, y, z;
    
    // State
    private boolean connected;
    private boolean muted;
    private boolean deafened;
    private boolean talking;
    
    // Timing
    private long lastPacketTime;
    private long connectTime;
    
    public VoicePlayer(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.connected = false;
        this.muted = false;
        this.deafened = false;
        this.talking = false;
        this.connectTime = System.currentTimeMillis();
    }
    
    // ==================== Position ====================
    
    public void updatePosition(double x, double y, double z, String worldId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldId = worldId;
    }
    
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public String getWorldId() { return worldId; }
    
    // ==================== Identity ====================
    
    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    
    // ==================== Network ====================
    
    public InetSocketAddress getAddress() { return address; }
    public void setAddress(InetSocketAddress address) { this.address = address; }
    
    // ==================== State ====================
    
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { 
        this.connected = connected; 
        if (connected) {
            this.connectTime = System.currentTimeMillis();
        }
    }
    
    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
    
    public boolean isDeafened() { return deafened; }
    public void setDeafened(boolean deafened) { this.deafened = deafened; }
    
    public boolean isTalking() { return talking; }
    public void setTalking(boolean talking) { this.talking = talking; }
    
    // ==================== Timing ====================
    
    public long getLastPacketTime() { return lastPacketTime; }
    public void setLastPacketTime(long time) { this.lastPacketTime = time; }
    
    public long getConnectTime() { return connectTime; }
    
    /**
     * Check if connection has timed out (no packets for 30 seconds)
     */
    public boolean isTimedOut() {
        return System.currentTimeMillis() - lastPacketTime > 30000;
    }
    
    @Override
    public String toString() {
        return "VoicePlayer{" + playerName + ", " + playerId + ", connected=" + connected + "}";
    }
}
