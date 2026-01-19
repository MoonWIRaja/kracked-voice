package dev.kracked.voice.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import dev.kracked.voice.KrackedVoicePlugin;
import dev.kracked.voice.player.VoicePlayer;

import java.util.Collection;

public class VoiceDebugCommand extends AbstractPlayerCommand {
    public VoiceDebugCommand() {
        super("voicedebug", "Show voice chat connection status");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(CommandContext context, Store<?> store, Ref<?> ref, PlayerRef playerRef, World world) {
        if (playerRef == null) return;

        VoicePlayer player = KrackedVoicePlugin.getInstance().getPlayerManager()
                .getPlayer(playerRef.getUuid());

        context.sendMessage(Message.raw("§d§l========== [KrackedVoice] Debug =========="));
        context.sendMessage(Message.raw("§eServer Port: §f" + KrackedVoicePlugin.getInstance().getConfig().getVoicePort()));
        context.sendMessage(Message.raw("§eEncryption: §f" + (KrackedVoicePlugin.getInstance().getConfig().isEncryptionEnabled() ? "Enabled" : "Disabled")));
        context.sendMessage(Message.raw("§eTotal Players Registered: §f" + KrackedVoicePlugin.getInstance().getPlayerManager().getAllPlayers().size()));
        context.sendMessage(Message.raw("§eConnected Players: §f" + KrackedVoicePlugin.getInstance().getPlayerManager().getConnectedCount()));

        if (player == null) {
            context.sendMessage(Message.raw("§cYou are NOT registered in voice system!"));
            context.sendMessage(Message.raw("§7Waiting for sync packet..."));
        } else {
            context.sendMessage(Message.raw("§aYou are registered!"));
            context.sendMessage(Message.raw("§eConnected: §" + (player.isConnected() ? "aYES" : "cNO")));
            context.sendMessage(Message.raw("§eAddress: §f" + (player.getAddress() != null ? player.getAddress() : "null")));
            context.sendMessage(Message.raw("§eMuted: §" + (player.isMuted() ? "cYES" : "aNO")));

            // Resend sync packet if not connected
            if (!player.isConnected()) {
                String syncPacket = "[KRACKED_SYNC]|||" + KrackedVoicePlugin.getInstance().getConfig().getVoicePort() + "|||" + KrackedVoicePlugin.getInstance().getConfig().getSecretKey();
                playerRef.sendMessage(Message.raw(syncPacket));
                context.sendMessage(Message.raw("§e[DEBUG] Sync packet resent! Check your console."));
            }
        }
        context.sendMessage(Message.raw("§d§l======================================="));
    }
}
