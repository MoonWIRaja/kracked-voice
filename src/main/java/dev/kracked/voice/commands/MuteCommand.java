package dev.kracked.voice.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import dev.kracked.voice.KrackedVoicePlugin;
import dev.kracked.voice.player.VoicePlayer;

public class MuteCommand extends AbstractPlayerCommand {
    public MuteCommand() {
        super("mute", "Toggle your microphone");
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
        
        if (player == null) {
            context.sendMessage(Message.raw("§cYou are not registered in the voice system yet. Wait a few seconds."));
            return;
        }
        
        if (player.isConnected()) {
            boolean newState = !player.isMuted();
            player.setMuted(newState);
            
            // Sync with client
            KrackedVoicePlugin.getInstance().getVoiceServer().sendMuteState(player, newState);
            
            String msg = newState ? "§aMicrophone muted." : "§aMicrophone unmuted.";
            context.sendMessage(Message.raw(msg));
        } else {
            context.sendMessage(Message.raw("§cYour voice client is not connected to the UDP server!"));
        }
    }
}
