package com.lemonadesergeant.milestones.api;

@FunctionalInterface
public interface ListenerRegistration {

    void unregister();
}