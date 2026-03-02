package com.lemonadesergeant.milestones.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.IllegalFormatException;
import java.util.Locale;

import javax.annotation.Nonnull;

import com.hypixel.hytale.logger.HytaleLogger;

public final class PluginLog {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Object FILE_LOCK = new Object();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PluginLog() {
    }

    public static void info(@Nonnull LogSource source, @Nonnull String message, Object... args) {
        logInfo(source, LogStage.GENERAL, message, args);
    }

    public static void info(@Nonnull LogSource source, @Nonnull LogStage stage, @Nonnull String message, Object... args) {
        logInfo(source, stage, message, args);
    }

    public static void info(@Nonnull String source, @Nonnull String message, Object... args) {
        LogSource resolvedSource = resolveSource(source);
        logInfo(resolvedSource, LogStage.GENERAL, message, args);
    }

    public static void info(@Nonnull String source, @Nonnull LogStage stage, @Nonnull String message, Object... args) {
        LogSource resolvedSource = resolveSource(source);
        logInfo(resolvedSource, stage, message, args);
    }

    public static void warn(@Nonnull LogSource source, @Nonnull String message, Object... args) {
        logWarn(source, LogStage.GENERAL, message, args);
    }

    public static void warn(@Nonnull LogSource source, @Nonnull LogStage stage, @Nonnull String message, Object... args) {
        logWarn(source, stage, message, args);
    }

    public static void warn(@Nonnull String source, @Nonnull String message, Object... args) {
        LogSource resolvedSource = resolveSource(source);
        logWarn(resolvedSource, LogStage.GENERAL, message, args);
    }

    public static void warn(@Nonnull String source, @Nonnull LogStage stage, @Nonnull String message, Object... args) {
        LogSource resolvedSource = resolveSource(source);
        logWarn(resolvedSource, stage, message, args);
    }

    public static void error(@Nonnull LogSource source, @Nonnull String message, Object... args) {
        logError(source, LogStage.GENERAL, message, args);
    }

    public static void error(@Nonnull LogSource source, @Nonnull LogStage stage, @Nonnull String message, Object... args) {
        logError(source, stage, message, args);
    }

    public static void error(@Nonnull String source, @Nonnull String message, Object... args) {
        LogSource resolvedSource = resolveSource(source);
        logError(resolvedSource, LogStage.GENERAL, message, args);
    }

    public static void error(@Nonnull String source, @Nonnull LogStage stage, @Nonnull String message, Object... args) {
        LogSource resolvedSource = resolveSource(source);
        logError(resolvedSource, stage, message, args);
    }

    private static void logInfo(@Nonnull LogSource source, @Nonnull LogStage stage, @Nonnull String message, Object... args) {
        if (!isEnabled(source)) {
            return;
        }

        String formattedMessage = formatMessage(message, args);
        LOGGER.atInfo().log("[" + source.name() + "] [" + stage.name() + "] " + formattedMessage);
        writeToFile("INFO", source, stage, formattedMessage);
    }

    private static void logWarn(@Nonnull LogSource source, @Nonnull LogStage stage, @Nonnull String message, Object... args) {
        if (!isEnabled(source)) {
            return;
        }

        String formattedMessage = formatMessage(message, args);
        LOGGER.atWarning().log("[" + source.name() + "] [" + stage.name() + "] " + formattedMessage);
        writeToFile("WARN", source, stage, formattedMessage);
    }

    private static void logError(@Nonnull LogSource source, @Nonnull LogStage stage, @Nonnull String message, Object... args) {
        if (!isEnabled(source)) {
            return;
        }

        String formattedMessage = formatMessage(message, args);
        LOGGER.atSevere().log("[" + source.name() + "] [" + stage.name() + "] " + formattedMessage);
        writeToFile("ERROR", source, stage, formattedMessage);
    }

    public static boolean isEnabled(@Nonnull LogSource source) {
        switch (source) {
            case PLUGIN:
                return LoggingSettings.ENABLE_PLUGIN_LOGGING;
            case BLOCK_BREAK:
                return LoggingSettings.ENABLE_BLOCK_BREAK_LOGGING;
            case BLOCK_PLACE:
                return LoggingSettings.ENABLE_BLOCK_PLACE_LOGGING;
            case BLOCK_USE:
                return LoggingSettings.ENABLE_BLOCK_USE_LOGGING;
            case CRAFT_RECIPE:
                return LoggingSettings.ENABLE_CRAFT_RECIPE_LOGGING;
            case DAMAGE:
                return LoggingSettings.ENABLE_DAMAGE_LOGGING;
            case ENTITY_KILL:
                return LoggingSettings.ENABLE_ENTITY_KILL_LOGGING;
            case INTERACTIVELY_PICKUP:
                return LoggingSettings.ENABLE_INTERACTIVELY_PICKUP_LOGGING;
            case ITEM_PICKUP:
                return LoggingSettings.ENABLE_ITEM_PICKUP_LOGGING;
            case ZONE_AND_BIOME_DISCOVERY:
                return LoggingSettings.ENABLE_ZONE_AND_BIOME_DISCOVERY_LOGGING;
            case MILESTONE_STATE:
                return LoggingSettings.ENABLE_MILESTONE_STATE_LOGGING;
            case MILESTONE_UPDATE:
                return LoggingSettings.ENABLE_MILESTONE_UPDATE_LOGGING;
            case STATS_MANAGER:
                return LoggingSettings.ENABLE_STATS_MANAGER_LOGGING;
            case STATS_HELPER:
                return LoggingSettings.ENABLE_STATS_HELPER_LOGGING;
            case MILESTONE_MANAGER:
                return LoggingSettings.ENABLE_MILESTONE_MANAGER_LOGGING;
            case PROGRESS_HELPER:
                return LoggingSettings.ENABLE_PROGRESS_HELPER_LOGGING;
            case DATA_STORAGE:
                return LoggingSettings.ENABLE_DATA_STORAGE_LOGGING;
            case PLAYER_INIT:
                return LoggingSettings.ENABLE_PLAYER_INIT_LOGGING;
            default:
                return true;
        }
    }

    @Nonnull
    private static LogSource resolveSource(@Nonnull String source) {
        String normalized = source.trim().toUpperCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');

        try {
            return LogSource.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return LogSource.PLUGIN;
        }
    }

    private static void writeToFile(@Nonnull String level, @Nonnull LogSource source, @Nonnull LogStage stage, @Nonnull String formattedMessage) {
        if (!LoggingSettings.ENABLE_FILE_LOGGING) {
            return;
        }

        Path logPath = Paths.get(LoggingSettings.LOG_FILE_PATH);
        Path parent = logPath.getParent();
        String line = String.format(
            Locale.ROOT,
            "[%s] [%s] [%s] [%s] %s%n",
            LocalDateTime.now().format(TIMESTAMP_FORMAT),
            level,
            source.name(),
            stage.name(),
            formattedMessage
        );

        synchronized (FILE_LOCK) {
            try {
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                Files.writeString(
                    logPath,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                );
            } catch (IOException ex) {
                LOGGER.atWarning().log("[PLUGIN] Failed writing log file: %s", ex.getMessage());
            }
        }
    }

    @Nonnull
    private static String formatMessage(@Nonnull String message, Object... args) {
        try {
            return String.format(Locale.ROOT, message, args);
        } catch (IllegalFormatException ex) {
            return message + " [formatting error: " + ex.getMessage() + "]";
        }
    }
}