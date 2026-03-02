package com.lemonadesergeant.milestones.systems;

import java.lang.reflect.Method;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

final class SystemEntityResolver {

    private static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

    private SystemEntityResolver() {
    }

    static Player resolvePlayer(Ref<EntityStore> ref) {
        if (ref == null || !ref.isValid()) {
            return null;
        }

        try {
            return ref.getStore().getComponent(ref, Player.getComponentType());
        } catch (Exception ex) {
            return null;
        }
    }

    static String resolvePlayerUuid(Player player) {
        if (player == null) {
            return null;
        }

        try {
            Object value = player.getClass().getMethod("getUuid").invoke(player);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ex) {
            return null;
        }
    }

    static String resolveEntityId(Ref<EntityStore> ref, Player player) {
        if (player != null) {
            String uuid = resolvePlayerUuid(player);
            if (uuid != null) {
                return uuid;
            }

            return player.getDisplayName();
        }

        if (ref == null) {
            return "unknown";
        }

        String npcTypeId = resolveNpcTypeId(ref);
        if (npcTypeId != null && !npcTypeId.isBlank()) {
            return npcTypeId;
        }

        return "entity:" + ref.getIndex();
    }

    static String resolveEntityName(Ref<EntityStore> ref, Player player) {
        if (player != null) {
            String displayName = player.getDisplayName();
            if (displayName != null && !displayName.isBlank()) {
                return displayName;
            }

            String uuid = resolvePlayerUuid(player);
            if (uuid != null) {
                return uuid;
            }
        }

        if (ref == null) {
            return "unknown";
        }

        String npcTypeId = resolveNpcTypeId(ref);
        if (npcTypeId != null && !npcTypeId.isBlank()) {
            return npcTypeId;
        }

        String reflectedName = resolveNameFromReference(ref);
        if (reflectedName != null) {
            return reflectedName;
        }

        return "entity:" + ref.getIndex();
    }

    static String resolveEntityStatKey(Ref<EntityStore> ref, Player player) {
        if (player != null) {
            String uuid = resolvePlayerUuid(player);
            if (uuid != null && !uuid.isBlank()) {
                return uuid;
            }

            String displayName = player.getDisplayName();
            return displayName == null || displayName.isBlank() ? "unknown" : displayName;
        }

        if (ref == null) {
            return "unknown";
        }

        String npcTypeId = resolveNpcTypeId(ref);
        if (npcTypeId != null && !npcTypeId.isBlank()) {
            return npcTypeId.trim().toLowerCase().replace(' ', '_').replace('-', '_');
        }

        String resolved = resolveNameFromReference(ref);
        if (resolved == null || resolved.isBlank()) {
            return "entity:" + ref.getIndex();
        }

        if (resolved.matches(UUID_PATTERN)) {
            return resolved;
        }

        String compact = resolved.trim();
        if (compact.startsWith("entity:")) {
            String type = resolveEntityTypeNameFromReference(ref);
            if (type != null && !type.isBlank()) {
                compact = type;
            }
        }

        compact = compact.replace(' ', '_');
        compact = compact.replace('-', '_');

        if (compact.contains(":")) {
            compact = compact.substring(compact.lastIndexOf(':') + 1);
        }

        if (compact.contains(".")) {
            compact = compact.substring(compact.lastIndexOf('.') + 1);
        }

        String[] tokens = compact.split("_");
        if (tokens.length > 1) {
            String first = tokens[0].toLowerCase();
            if ("entity".equals(first) || "npc".equals(first) || "mob".equals(first) || "creature".equals(first)) {
                compact = tokens[tokens.length - 1];
            }
        }

        return compact.isBlank() ? "entity:" + ref.getIndex() : compact.toLowerCase();
    }

    static String resolvePlayerOrEntityId(Player player, int entityIndex) {
        if (player == null) {
            return "entity:" + entityIndex;
        }

        String uuid = resolvePlayerUuid(player);
        if (uuid != null) {
            return uuid;
        }

        return player.getDisplayName();
    }

    static String resolvePlayerOrEntityName(Player player, int entityIndex) {
        if (player == null) {
            return "entity:" + entityIndex;
        }

        String displayName = player.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }

        String uuid = resolvePlayerUuid(player);
        if (uuid != null) {
            return uuid;
        }

        return "entity:" + entityIndex;
    }

    private static String resolveNameFromReference(Ref<EntityStore> ref) {
        String directRefName = tryExtractNamedValue(ref);
        if (directRefName != null) {
            return directRefName;
        }

        String[] storeGetterCandidates = {
            "getEntity",
            "getEntityOrNull",
            "getEntityType",
            "getType"
        };

        for (String getterName : storeGetterCandidates) {
            Object value = invokeStoreGetter(ref, getterName);
            String extracted = tryExtractNamedValue(value);
            if (extracted != null) {
                return extracted;
            }
        }

        return null;
    }

    private static String resolveNpcTypeId(Ref<EntityStore> ref) {
        if (ref == null || ref.getStore() == null || !ref.isValid()) {
            return null;
        }

        try {
            NPCEntity npc = ref.getStore().getComponent(ref, NPCEntity.getComponentType());
            if (npc == null) {
                return null;
            }

            String npcTypeId = npc.getNPCTypeId();
            return npcTypeId == null || npcTypeId.isBlank() ? null : npcTypeId;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String resolveEntityTypeNameFromReference(Ref<EntityStore> ref) {
        String[] storeGetterCandidates = {
            "getEntity",
            "getEntityOrNull",
            "getEntityType",
            "getType"
        };

        for (String getterName : storeGetterCandidates) {
            Object value = invokeStoreGetter(ref, getterName);
            if (value == null) {
                continue;
            }

            Object[] nested = {
                invokeObjectGetter(value, "getType"),
                invokeObjectGetter(value, "getEntityType"),
                invokeObjectGetter(value, "getDefinition"),
                invokeObjectGetter(value, "getAsset")
            };

            for (Object nestedValue : nested) {
                String extracted = tryExtractNamedValue(nestedValue);
                if (extracted != null && !extracted.isBlank()) {
                    return extracted;
                }
            }

            String direct = tryExtractNamedValue(value);
            if (direct != null && !direct.isBlank()) {
                return direct;
            }
        }

        return null;
    }

    private static Object invokeObjectGetter(Object target, String methodName) {
        if (target == null) {
            return null;
        }

        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Object invokeStoreGetter(Ref<EntityStore> ref, String methodName) {
        if (ref == null || ref.getStore() == null) {
            return null;
        }

        Object store = ref.getStore();
        Method[] methods = store.getClass().getMethods();
        for (Method method : methods) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }

            Class<?> parameterType = method.getParameterTypes()[0];
            if (!parameterType.isAssignableFrom(ref.getClass())) {
                continue;
            }

            try {
                return method.invoke(store, ref);
            } catch (Exception ex) {
                return null;
            }
        }

        return null;
    }

    private static String tryExtractNamedValue(Object target) {
        if (target == null) {
            return null;
        }

        if (target instanceof CharSequence) {
            String text = String.valueOf(target);
            return text.isBlank() ? null : text;
        }

        String[] getters = {
            "getDisplayName",
            "getName",
            "name",
            "getIdentifier",
            "getId",
            "id"
        };

        for (String getter : getters) {
            String value = invokeStringGetter(target, getter);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        String fallback = String.valueOf(target);
        if (fallback.contains("@") || fallback.isBlank()) {
            return null;
        }

        return fallback;
    }

    private static String invokeStringGetter(Object target, String methodName) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ex) {
            return null;
        }
    }
}