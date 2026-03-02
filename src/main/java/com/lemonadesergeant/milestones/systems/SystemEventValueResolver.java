package com.lemonadesergeant.milestones.systems;

final class SystemEventValueResolver {

    private SystemEventValueResolver() {
    }

    static String resolveItemId(Object itemStack) {
        if (itemStack == null) {
            return null;
        }

        try {
            Object value = itemStack.getClass().getMethod("getItemId").invoke(itemStack);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ex) {
            return null;
        }
    }
}