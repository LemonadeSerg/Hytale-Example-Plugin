package com.lemonadesergeant.milestones;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.commands.MilestonesCommandCollection;
import com.lemonadesergeant.milestones.data.PlayerStatsComponent;
import com.lemonadesergeant.milestones.interaction.LemStatConditionInteraction;
import com.lemonadesergeant.milestones.logging.*;
import com.lemonadesergeant.milestones.systems.*;

public class MilestonesPlugin extends JavaPlugin {

    private static MilestonesPlugin instance;
    private ComponentType<EntityStore, PlayerStatsComponent> playerStatsComponentType;

    public MilestonesPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        PluginLog.info(LogSource.PLUGIN, LogStage.LIFECYCLE, "component=MilestonesPlugin action=loaded version=%s pluginName=%s",
            this.getManifest().getVersion().toString(), 
            this.getName());
    }

    /**
     * @return Singleton instance for accessing plugin components and configuration
     */
    @Nonnull
    public static MilestonesPlugin instance() {
        return instance;
    }

    @Nonnull
    public ComponentType<EntityStore, PlayerStatsComponent> getPlayerStatsComponentType() {
        return playerStatsComponentType;
    }

    @Override
    protected void setup() {
        PluginLog.info(LogSource.PLUGIN, LogStage.SETUP, "component=MilestonesPlugin action=setup version=%s", this.getManifest().getVersion());
        
        // === Register Interactions ===
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerInteractions");
        getCodecRegistry(Interaction.CODEC).register(
            LemStatConditionInteraction.TYPE_ID,
            LemStatConditionInteraction.class,
            LemStatConditionInteraction.CODEC
        );
        PluginLog.info(
            LogSource.PLUGIN,
            LogStage.REGISTER,
            "component=MilestonesPlugin action=registerInteractions interaction=%s status=complete",
            LemStatConditionInteraction.TYPE_ID
        );

        // === Register Commands ===
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerCommands");
        getCommandRegistry().registerCommand(new MilestonesCommandCollection());
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerCommands status=complete");

        // === Register Data Components ===
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerDataComponents");
        playerStatsComponentType = getEntityStoreRegistry().registerComponent(
            PlayerStatsComponent.class,
            "MilestonesPlayerStats",
            PlayerStatsComponent.CODEC
        );
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerDataComponents component=MilestonesPlayerStats status=complete");

        // === Register Asset Schemas ===
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerAssetSchemas");

        // === Register ECS Event Systems ===
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerEntityEventSystems");

        // === Block Event Systems ===
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=start", BlockBreakSystem.class.getSimpleName());
        getEntityStoreRegistry().registerSystem(new BlockBreakSystem()); 
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=complete", BlockBreakSystem.class.getSimpleName());

        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=start", BlockPlaceSystem.class.getSimpleName());
        getEntityStoreRegistry().registerSystem(new BlockPlaceSystem());
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=complete", BlockPlaceSystem.class.getSimpleName());

        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=start", BlockUseSystem.class.getSimpleName());
        getEntityStoreRegistry().registerSystem(new BlockUseSystem());
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=complete", BlockUseSystem.class.getSimpleName());

        // === Crafting Event Systems ===
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=start", CraftRecipeSystem.class.getSimpleName());
        getEntityStoreRegistry().registerSystem(new CraftRecipeSystem());
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=complete", CraftRecipeSystem.class.getSimpleName());

        // === Combat Event Systems ===
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=start", DamageSystem.class.getSimpleName());
        getEntityStoreRegistry().registerSystem(new DamageSystem());
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=complete", DamageSystem.class.getSimpleName());

        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=start", EntityKillSystem.class.getSimpleName());
        getEntityStoreRegistry().registerSystem(new EntityKillSystem());
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=complete", EntityKillSystem.class.getSimpleName());

        // === Discovery Event Systems ===
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=start", ZoneAndBiomeDiscoverySystem.class.getSimpleName());
        getEntityStoreRegistry().registerSystem(new ZoneAndBiomeDiscoverySystem());
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=complete", ZoneAndBiomeDiscoverySystem.class.getSimpleName());

        // === Item Event Systems ===
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=start", InteractivelyPickupSystem.class.getSimpleName());
        getEntityStoreRegistry().registerSystem(new InteractivelyPickupSystem());
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=complete", InteractivelyPickupSystem.class.getSimpleName());

        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=start", ItemPickupSystem.class.getSimpleName());
        getEntityStoreRegistry().registerSystem(new ItemPickupSystem());
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerSystem system=%s phase=complete", ItemPickupSystem.class.getSimpleName());

        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerEntityEventSystems status=complete");

        // === Register Global Events ===
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerGlobalEvents");

    }

    @Override
    protected void start(){
        PluginLog.info(LogSource.PLUGIN, LogStage.LIFECYCLE, "component=MilestonesPlugin action=start version=%s", this.getManifest().getVersion());

        // === Register Asset Types ===
        // Must happen during start(); AssetEditorPlugin is not available during setup().
        PluginLog.info(LogSource.PLUGIN, LogStage.REGISTER, "component=MilestonesPlugin action=registerAssetTypes");
    }

}