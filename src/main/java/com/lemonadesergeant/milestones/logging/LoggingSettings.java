package com.lemonadesergeant.milestones.logging;

public final class LoggingSettings {

    private LoggingSettings() {
    }

    public static boolean ENABLE_PLUGIN_LOGGING = true;
    public static boolean ENABLE_FILE_LOGGING = true;
    public static String LOG_FILE_PATH = "run/logs/milestones-plugin.txt";

    public static boolean ENABLE_BLOCK_BREAK_LOGGING = true;
    public static boolean ENABLE_BLOCK_PLACE_LOGGING = true;
    public static boolean ENABLE_BLOCK_USE_LOGGING = true;
    public static boolean ENABLE_CRAFT_RECIPE_LOGGING = true;
    public static boolean ENABLE_DAMAGE_LOGGING = true;
    public static boolean ENABLE_ENTITY_KILL_LOGGING = true;
    public static boolean ENABLE_INTERACTIVELY_PICKUP_LOGGING = true;
    public static boolean ENABLE_ITEM_PICKUP_LOGGING = true;
    public static boolean ENABLE_ZONE_AND_BIOME_DISCOVERY_LOGGING = true;

    public static boolean ENABLE_MILESTONE_STATE_LOGGING = true;
    public static boolean ENABLE_MILESTONE_UPDATE_LOGGING = true;

    public static boolean ENABLE_STATS_MANAGER_LOGGING = true;
    public static boolean ENABLE_STATS_HELPER_LOGGING = true;

    public static boolean ENABLE_MILESTONE_MANAGER_LOGGING = true;
    public static boolean ENABLE_PROGRESS_HELPER_LOGGING = true;

    public static boolean ENABLE_DATA_STORAGE_LOGGING = true;
    public static boolean ENABLE_PLAYER_INIT_LOGGING = true;
}