package dev.kracked.voice.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Configuration for voice chat
 */
public class VoiceConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final File configFile;
    private ConfigData data;
    
    public VoiceConfig(File dataFolder) {
        this.configFile = new File(dataFolder, "config.json");
    }
    
    public void load() {
        if (!configFile.exists()) {
            // Create default config
            this.data = new ConfigData();
            save();
            LOGGER.info("Created default configuration");
        } else {
            try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                this.data = GSON.fromJson(reader, ConfigData.class);
                LOGGER.info("Loaded configuration from file");
            } catch (Exception e) {
                LOGGER.error("Failed to load config, using defaults", e);
                this.data = new ConfigData();
            }
        }
        
        // Generate secret key if not set
        if (data.secretKey == null || data.secretKey.isEmpty()) {
            data.secretKey = generateSecretKey();
            save();
            LOGGER.info("Generated new secret key");
        }
    }
    
    public void save() {
        configFile.getParentFile().mkdirs();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }
    
    private String generateSecretKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
    
    // ==================== Getters ====================
    
    public boolean isEnabled() {
        return data.enabled;
    }
    
    public String getVoiceHost() {
        return data.voiceHost;
    }
    
    public int getVoicePort() {
        return data.voicePort;
    }
    
    public boolean isProximityEnabled() {
        return data.proximityEnabled;
    }
    
    public boolean isEncryptionEnabled() {
        return data.encryptionEnabled;
    }
    
    public double getMaxDistance() {
        return data.maxDistance;
    }
    
    public String getFalloffType() {
        return data.falloffType;
    }
    
    public int getBitrate() {
        return data.bitrate;
    }
    
    public String getPushToTalkKey() {
        return data.pushToTalkKey;
    }
    
    public String getMuteToggleKey() {
        return data.muteToggleKey;
    }
    
    public String getSecretKey() {
        return data.secretKey;
    }
    
    /**
     * Internal config data structure
     */
    private static class ConfigData {
        boolean enabled = true;
        String voiceHost = "0.0.0.0"; // 0.0.0.0 = bind to all interfaces
        int voicePort = 3012; // FIXED: Match common UDP port that works

        // Feature Toggles
        boolean proximityEnabled = true; // Distance-based volume
        boolean encryptionEnabled = false; // FIXED: Disable encryption initially for debugging

        double maxDistance = 20.0;
        String falloffType = "linear"; // linear, exponential
        int bitrate = 64000;
        String pushToTalkKey = "V";
        String muteToggleKey = "M";
        String secretKey = "";
    }
}
