package dev.kracked.voice;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.kracked.voice.audio.OpusCodec;
import dev.kracked.voice.commands.*;
import dev.kracked.voice.config.VoiceConfig;
import dev.kracked.voice.network.UdpVoiceServer;
import dev.kracked.voice.player.VoicePlayer;
import dev.kracked.voice.player.VoicePlayerManager;
import dev.kracked.voice.proximity.ProximityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * KrackedVoice - Premium Proximity Voice Chat for Hytale
 * Developed by MoonWiRaja KRACKEDDEVS
 * 
 * @author MoonWiRaja KRACKEDDEVS
 * @version 1.0.0
 */
public class KrackedVoicePlugin extends JavaPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(KrackedVoicePlugin.class);
    private static KrackedVoicePlugin instance;
    
    private File dataFolder;
    private VoiceConfig config;
    private UdpVoiceServer voiceServer;
    private VoicePlayerManager playerManager;
    private ProximityEngine proximityEngine;
    private OpusCodec opusCodec;
    
    public KrackedVoicePlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }
    
    @Override
    protected void start() {
        super.start();
        LOGGER.info("========================================");
        LOGGER.info("  KrackedVoice - Proximity Voice Chat");
        LOGGER.info("  By: MoonWiRaja KRACKEDDEVS");
        LOGGER.info("  Version: 1.0.0");
        LOGGER.info("========================================");

        this.dataFolder = new File("plugins/KrackedVoice");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        this.config = new VoiceConfig(dataFolder);
        config.load();

        this.opusCodec = new OpusCodec();
        this.playerManager = new VoicePlayerManager();
        this.proximityEngine = new ProximityEngine(config.getMaxDistance());

        this.voiceServer = new UdpVoiceServer(
            config.getVoiceHost(),
            config.getVoicePort(),
            playerManager,
            proximityEngine,
            config.getSecretKey(),
            config.isProximityEnabled(),
            config.isEncryptionEnabled()
        );

        try {
            voiceServer.start();
            LOGGER.info("Voice server started on {}:{}", config.getVoiceHost(), config.getVoicePort());
            LOGGER.info("[KrackedVoice] Secret Key generated and synced for client.");
            LOGGER.info("[KrackedVoice] Players can now connect using /voice command");
        } catch (Exception e) {
            LOGGER.error("Failed to start KrackedVoice server!", e);
            LOGGER.error("[KrackedVoice] Voice chat will be unavailable until server restart");
            return;
        }

        getCommandRegistry().registerCommand(new VoiceGuiCommand());
        getCommandRegistry().registerCommand(new MuteCommand());
        getCommandRegistry().registerCommand(new VoicePartyCommand());
        getCommandRegistry().registerCommand(new VoiceDebugCommand());

        // Robust Multi-Stage Sync Task
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                syncPlayers();
            }
        }, 1000, 3000); // Sync every 3 seconds

        LOGGER.info("KrackedVoice activated successfully!");
        LOGGER.info("[KrackedVoice] Client assets should auto-load when players join");
    }
    
    private void syncPlayers() {
        try {
            Universe universe = Universe.get();
            if (universe == null || universe.getWorlds() == null) return;

            for (World world : universe.getWorlds().values()) {
                if (world == null || world.getPlayerRefs() == null) continue;

                for (PlayerRef playerRef : world.getPlayerRefs()) {
                    VoicePlayer vp = playerManager.getPlayer(playerRef.getUuid());

                    // Register new players
                    if (vp == null) {
                        vp = playerManager.registerPlayer(playerRef.getUuid(), playerRef.getUsername());
                        LOGGER.info("[KrackedVoice] Registered player: {}", playerRef.getUsername());

                        // Send connection info to player ONCE when they first register
                        playerRef.sendMessage(Message.raw("§d§l========== [KrackedVoice] =========="));
                        playerRef.sendMessage(Message.raw("§eVoice Chat is available on this server!"));
                        playerRef.sendMessage(Message.raw("§aPress V to talk, /voice for settings"));
                        playerRef.sendMessage(Message.raw("§7Connecting to UDP server on port " + config.getVoicePort() + "..."));
                        playerRef.sendMessage(Message.raw("§d§l==================================="));

                        // Try multiple sync packet formats for maximum compatibility
                        sendSyncPackets(playerRef);
                    }

                    // Send sync packet ONLY to non-connected players
                    if (vp != null && !vp.isConnected()) {
                        sendSyncPackets(playerRef);
                        LOGGER.debug("[KrackedVoice] Syncing {}: port={}", playerRef.getUsername(), config.getVoicePort());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[KrackedVoice] Sync error: {}", e.getMessage());
        }
    }

    /**
     * Send sync packets in multiple formats for compatibility
     */
    private void sendSyncPackets(PlayerRef playerRef) {
        // Format 1: With ||| delimiter (new)
        playerRef.sendMessage(Message.raw("[KRACKED_SYNC]|||" + config.getVoicePort() + "|||" + config.getSecretKey()));

        // Format 2: JSON format (for potential future parsing)
        String jsonSync = "{\"mod\":\"kracked_voice\",\"port\":" + config.getVoicePort() + ",\"key\":\"" + config.getSecretKey() + "\"}";
        playerRef.sendMessage(Message.raw("[KRACKED_JSON]" + jsonSync));

        // Format 3: Simple format (fallback)
        playerRef.sendMessage(Message.raw("[KRACKED] " + config.getVoicePort()));
    }
    
    @Override
    protected void stop() {
        if (voiceServer != null) voiceServer.stop();
        if (playerManager != null) playerManager.disconnectAll();
        LOGGER.info("KrackedVoice deactivated.");
    }
    
    public static KrackedVoicePlugin getInstance() { return instance; }
    public VoiceConfig getConfig() { return config; }
    public UdpVoiceServer getVoiceServer() { return voiceServer; }
    public VoicePlayerManager getPlayerManager() { return playerManager; }
    public ProximityEngine getProximityEngine() { return proximityEngine; }
    public OpusCodec getOpusCodec() { return opusCodec; }
}
