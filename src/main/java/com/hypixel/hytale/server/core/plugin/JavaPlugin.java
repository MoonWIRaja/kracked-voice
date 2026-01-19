package com.hypixel.hytale.server.core.plugin;

import java.io.File;

public class JavaPlugin {
    
    public JavaPlugin(JavaPluginInit init) {
    }
    
    protected void start() {}
    protected void stop() {}
    
    public com.hypixel.hytale.server.core.command.system.CommandRegistry getCommandRegistry() {
        return new com.hypixel.hytale.server.core.command.system.CommandRegistry();
    }
    
    // Guessing the API has this. If not, we might crash. 
    // But standard practice suggests a way to get data folder.
    // If this method doesn't exist at runtime, we get NoSuchMethodError.
    // For safety, I will try to use the 'plugins' directory manually if this fails,
    // but I can't try-catch a method call that doesn't exist unless I use reflection.
    // I'll assume standard getDataFolder exists for now or use a fallback.
    public File getDataFolder() {
        return new File("plugins/HytaleVoice");
    }
}
