package com.lemonadesergeant.milestones.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class MilestonesCommandCollection extends AbstractCommandCollection {

    public MilestonesCommandCollection() {
        super("milestones", "milestones.commands.desc");
        this.addAliases("ms");
        this.addSubCommand(new StatsListCommand());
    }
}
