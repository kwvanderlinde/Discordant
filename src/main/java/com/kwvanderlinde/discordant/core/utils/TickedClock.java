package com.kwvanderlinde.discordant.core.utils;

public final class TickedClock implements Clock {
    private long currentTime;

    public TickedClock() {
        tick();
    }

    public void tick() {
        currentTime = System.currentTimeMillis();
    }

    public long getCurrentTime() {
        return currentTime;
    }
}
