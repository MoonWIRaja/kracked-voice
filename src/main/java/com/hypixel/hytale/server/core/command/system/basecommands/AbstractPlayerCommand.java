package com.hypixel.hytale.server.core.command.system.basecommands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;

public abstract class AbstractPlayerCommand extends CommandBase {
    public AbstractPlayerCommand(String name, String description) {
        super(name, description);
    }

    protected abstract void execute(CommandContext context, Store<?> store, Ref<?> ref, PlayerRef player, World world);

    @Override
    protected void executeSync(CommandContext context) {
        // This is likely called by the server, which then calls execute() 
        // after extracting the player data.
    }
}
