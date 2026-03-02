package com.lemonadesergeant.milestones.commands;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.lemonadesergeant.milestones.data.GameEventType;

public class StatsTypesCommand extends CommandBase {

    public StatsTypesCommand() {
        super("types", "milestones.commands.stats.types.desc");
        this.addAliases("type");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String types = Arrays.stream(GameEventType.values())
            .map(type -> type.name().toLowerCase(Locale.ROOT))
            .collect(Collectors.joining(", "));

        context.sendMessage(Message.raw("Available stat types: " + types));
        context.sendMessage(Message.raw("Usage: /milestones stats --statType <type> [--id <id>] [--player <player>]."));
    }
}
