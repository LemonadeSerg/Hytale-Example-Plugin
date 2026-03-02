package com.lemonadesergeant.milestones.logging;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class EventLogFormatter {

    private static final int MAX_PROPERTIES = 160;
    private static final int MAX_ITEMS = 40;

    private EventLogFormatter() {
    }

    @Nonnull
    public static String describe(@Nullable Object event) {
        if (event == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        appendObject(builder, event, visited);
        return builder.toString();
    }

    @Nonnull
    public static String describeCompact(@Nullable Object value) {
        return toCompactValue(value);
    }

    private static void appendObject(StringBuilder builder, Object value, Set<Object> visited) {
        if (isSimpleType(value)) {
            builder.append(value);
            return;
        }

        if (visited.contains(value)) {
            builder.append(simpleIdentity(value)).append("{...cycle...}");
            return;
        }

        visited.add(value);

        if (value.getClass().isArray()) {
            appendArray(builder, value);
            return;
        }

        if (value instanceof Collection) {
            appendCollection(builder, (Collection<?>) value);
            return;
        }

        if (value instanceof Map) {
            appendMap(builder, (Map<?, ?>) value);
            return;
        }

        appendByGetters(builder, value);
    }

    private static void appendArray(StringBuilder builder, Object arrayValue) {
        int length = Array.getLength(arrayValue);
        builder.append(arrayValue.getClass().getComponentType().getSimpleName())
            .append("[")
            .append(length)
            .append("]");

        if (length == 0) {
            return;
        }

        builder.append("=");
        builder.append("[");

        int count = Math.min(length, MAX_ITEMS);
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                builder.append(", ");
            }

            Object item = Array.get(arrayValue, index);
            builder.append(toCompactValue(item));
        }

        if (length > MAX_ITEMS) {
            builder.append(", ...");
        }

        builder.append("]");
    }

    private static void appendCollection(StringBuilder builder, Collection<?> collection) {
        builder.append(collection.getClass().getSimpleName())
            .append("(size=")
            .append(collection.size())
            .append(")");

        if (collection.isEmpty()) {
            return;
        }

        builder.append("=");
        builder.append("[");
        int index = 0;
        for (Object item : collection) {
            if (index >= MAX_ITEMS) {
                builder.append("...");
                break;
            }

            if (index > 0) {
                builder.append(", ");
            }

            builder.append(toCompactValue(item));
            index++;
        }
        builder.append("]");
    }

    private static void appendMap(StringBuilder builder, Map<?, ?> map) {
        builder.append(map.getClass().getSimpleName())
            .append("(size=")
            .append(map.size())
            .append(")");

        if (map.isEmpty()) {
            return;
        }

        builder.append("={");
        int index = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (index >= MAX_ITEMS) {
                builder.append("...");
                break;
            }

            if (index > 0) {
                builder.append(", ");
            }

            builder.append(toCompactValue(entry.getKey()))
                .append("=")
                .append(toCompactValue(entry.getValue()));
            index++;
        }
        builder.append("}");
    }

    private static void appendByGetters(StringBuilder builder, Object value) {
        builder.append(simpleIdentity(value)).append(" {");

        List<Method> getters = new ArrayList<>();
        for (Method method : value.getClass().getMethods()) {
            if (!isGetter(method)) {
                continue;
            }
            getters.add(method);
        }

        getters.sort(Comparator.comparing(Method::getName));

        int count = 0;
        for (Method getter : getters) {
            if (count >= MAX_PROPERTIES) {
                builder.append("...");
                break;
            }

            Object propertyValue;
            try {
                propertyValue = getter.invoke(value);
            } catch (Exception ex) {
                continue;
            }

            String propertyName = toPropertyName(getter.getName());
            if ("hCode".equals(propertyName)) {
                continue;
            }

            if (count > 0) {
                builder.append(", ");
            }

            builder.append(propertyName)
                .append("=")
                .append(toCompactValue(propertyValue));
            count++;
        }

        if (count == 0) {
            builder.append("value=").append(value);
        }

        builder.append(" }");
    }

    private static boolean isGetter(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            return false;
        }

        if (method.getParameterCount() != 0) {
            return false;
        }

        if (method.getReturnType() == Void.TYPE) {
            return false;
        }

        String name = method.getName();
        if (name.equals("getClass")) {
            return false;
        }

        return name.startsWith("get") || name.startsWith("is") || name.startsWith("has");
    }

    @Nonnull
    private static String toPropertyName(String methodName) {
        String baseName;
        if (methodName.startsWith("get") && methodName.length() > 3) {
            baseName = methodName.substring(3);
        } else if (methodName.startsWith("is") && methodName.length() > 2) {
            baseName = methodName.substring(2);
        } else if (methodName.startsWith("has") && methodName.length() > 3) {
            baseName = methodName.substring(3);
        } else {
            baseName = methodName;
        }

        if (baseName.isEmpty()) {
            return methodName;
        }

        return Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1);
    }

    @Nonnull
    private static String toCompactValue(@Nullable Object value) {
        if (value == null) {
            return "null";
        }

        if (isSimpleType(value)) {
            return String.valueOf(value);
        }

        if (value.getClass().isArray()) {
            return value.getClass().getComponentType().getSimpleName() + "[" + Array.getLength(value) + "]";
        }

        if (value instanceof Collection) {
            return value.getClass().getSimpleName() + "(size=" + ((Collection<?>) value).size() + ")";
        }

        if (value instanceof Map) {
            return value.getClass().getSimpleName() + "(size=" + ((Map<?, ?>) value).size() + ")";
        }

        String summarized = summarizeByPreferredGetters(value);
        if (summarized != null) {
            return summarized;
        }

        return simpleIdentity(value);
    }

    @Nullable
    private static String summarizeByPreferredGetters(@Nonnull Object value) {
        String[] candidates = new String[]{
            "getId",
            "getItemId",
            "getItemType",
            "getName",
            "getDisplayName",
            "getUuid",
            "getGameMode",
            "getNetworkId",
            "getType",
            "getKey",
            "getIndex",
            "isValid",
            "getX",
            "getY",
            "getZ",
            "getHeldItemSlot",
            "getHeldItemSectionId"
        };

        StringBuilder properties = new StringBuilder();
        int matched = 0;

        for (String getterName : candidates) {
            Object getterValue = invokeSimpleGetterIfExists(value, getterName);
            if (getterValue == null) {
                continue;
            }

            if (matched > 0) {
                properties.append(", ");
            }

            properties.append(toPropertyName(getterName)).append("=").append(getterValue);
            matched++;
        }

        if (matched == 0) {
            return null;
        }

        return value.getClass().getSimpleName() + "{" + properties + "}";
    }

    @Nullable
    private static Object invokeSimpleGetterIfExists(@Nonnull Object value, @Nonnull String getterName) {
        try {
            Method method = value.getClass().getMethod(getterName);
            if (method.getParameterCount() != 0 || method.getReturnType() == Void.TYPE) {
                return null;
            }

            Object result = method.invoke(value);
            if (result == null || !isSimpleType(result)) {
                return null;
            }

            return result;
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean isSimpleType(Object value) {
        return value instanceof String
            || value instanceof Number
            || value instanceof Boolean
            || value instanceof Character
            || value instanceof Enum
            || value instanceof UUID
            || value instanceof Temporal
            || value.getClass().isPrimitive();
    }

    @Nonnull
    private static String simpleIdentity(Object value) {
        return value.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(value));
    }
}