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

public class VoiceGuiCommand extends AbstractPlayerCommand {
    public VoiceGuiCommand() {
        super("voice", "Open voice chat settings");
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
            context.sendMessage(Message.raw("§cYou are not registered in the voice system yet. Wait a few seconds and try again."));
            return;
        }
        
        if (player.isConnected()) {
            KrackedVoicePlugin.getInstance().getVoiceServer().sendToggleGui(player);
            context.sendMessage(Message.raw("§d§l[KrackedVoice]§r §aOpening UI..."));
        } else {
            context.sendMessage(Message.raw("§d§l[KrackedVoice]§r §cNot connected to UDP Bridge!"));
            context.sendMessage(Message.raw("§eWaiting for Multi-Stage Sync..."));
            context.sendMessage(Message.raw("§7(Port: §b" + KrackedVoicePlugin.getInstance().getConfig().getVoicePort() + "§7)"));
        }
    }
}
