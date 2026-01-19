package com.hypixel.hytale.server.core.command.system.basecommands;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

public abstract class CommandBase extends AbstractCommand {
    public CommandBase(String name, String description) {}
    protected abstract void executeSync(CommandContext context);
    protected boolean canGeneratePermission() { return true; }
}
