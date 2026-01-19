package dev.kracked.voice.commands;

import dev.kracked.voice.KrackedVoicePlugin;
import dev.kracked.voice.player.VoicePlayer;
import dev.kracked.voice.player.VoicePlayerManager;

import java.util.UUID;

/**
 * Admin commands for voice chat management
 * 
 * Commands:
 * /vmute <player> - Mute a player's microphone
 * /vunmute <player> - Unmute a player
 * /vdeafen <player> - Deafen a player (they can't hear anyone)
 * /vundeafen <player> - Undeafen a player
 * /vreload - Reload voice config
 * /vstatus - Show voice chat status
 */
public class VoiceCommands {
    
    private final KrackedVoicePlugin plugin;
    private final VoicePlayerManager playerManager;
    
    public VoiceCommands(KrackedVoicePlugin plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
    }
    
    /**
     * Mute a player's voice
     * @param targetId Target player UUID
     * @param executor Command executor name
     * @return Result message
     */
    public String mutePlayer(UUID targetId, String executor) {
        VoicePlayer target = playerManager.getPlayer(targetId);
        if (target == null) {
            return "§cPlayer not found or not connected to voice";
        }
        
        target.setMuted(true);
        return "§a" + target.getPlayerName() + " has been muted by " + executor;
    }
    
    /**
     * Unmute a player's voice
     */
    public String unmutePlayer(UUID targetId, String executor) {
        VoicePlayer target = playerManager.getPlayer(targetId);
        if (target == null) {
            return "§cPlayer not found or not connected to voice";
        }
        
        target.setMuted(false);
        return "§a" + target.getPlayerName() + " has been unmuted by " + executor;
    }
    
    /**
     * Deafen a player (they can't hear anyone)
     */
    public String deafenPlayer(UUID targetId, String executor) {
        VoicePlayer target = playerManager.getPlayer(targetId);
        if (target == null) {
            return "§cPlayer not found or not connected to voice";
        }
        
        target.setDeafened(true);
        return "§a" + target.getPlayerName() + " has been deafened by " + executor;
    }
    
    /**
     * Undeafen a player
     */
    public String undeafenPlayer(UUID targetId, String executor) {
        VoicePlayer target = playerManager.getPlayer(targetId);
        if (target == null) {
            return "§cPlayer not found or not connected to voice";
        }
        
        target.setDeafened(false);
        return "§a" + target.getPlayerName() + " has been undeafened by " + executor;
    }
    
    /**
     * Reload voice configuration
     */
    public String reloadConfig() {
        plugin.getConfig().load();
        return "§aVoice configuration reloaded";
    }
    
    /**
     * Get voice chat status
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6===== HytaleVoice Status =====\n");
        sb.append("§7Voice Port: §f").append(plugin.getConfig().getVoicePort()).append("\n");
        sb.append("§7Max Distance: §f").append(plugin.getConfig().getMaxDistance()).append(" blocks\n");
        sb.append("§7Connected Players: §f").append(playerManager.getConnectedCount()).append("\n");
        sb.append("§7Status: §a").append(plugin.getVoiceServer() != null ? "Running" : "Stopped");
        return sb.toString();
    }
    
    /**
     * List all connected voice players
     */
    public String listPlayers() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6===== Voice Players =====\n");
        
        int count = 0;
        for (VoicePlayer player : playerManager.getAllPlayers()) {
            if (player.isConnected()) {
                count++;
                sb.append("§7- §f").append(player.getPlayerName());
                if (player.isMuted()) sb.append(" §c[MUTED]");
                if (player.isDeafened()) sb.append(" §c[DEAFENED]");
                if (player.isTalking()) sb.append(" §a[TALKING]");
                sb.append("\n");
            }
        }
        
        if (count == 0) {
            sb.append("§7No players connected to voice chat");
        }
        
        return sb.toString();
    }
}
