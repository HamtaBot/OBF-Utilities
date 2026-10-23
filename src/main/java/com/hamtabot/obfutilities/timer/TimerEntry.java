package com.hamtabot.obfutilities.timer;

public class TimerEntry {

    public enum Type {
        COUNTDOWN("Compte à rebours"), STOPWATCH("Chronomètre");

        public final String label;
        Type(String label) { this.label = label; }
    }

    private static final float MIN_SCALE = 0.5f, MAX_SCALE = 3f;

    public String  name          = "Timer";
    public Type    type          = Type.COUNTDOWN;
    public long    durationMs    = 30 * 60_000L;
    public boolean running       = false;
    public long    startedAt     = 0;
    public long    accumulatedMs = 0;
    public boolean finished      = false;
    public boolean showOnScreen  = false;
    public int     x = 10, y = 60;
    public float   scale = 1f;

    public TimerEntry() {}

    public TimerEntry(String name, Type type, long durationMs) {
        this.name = name;
        this.type = type;
        this.durationMs = durationMs;
    }

    public Type type() { return type != null ? type : Type.COUNTDOWN; }

    public boolean isCountdown() { return type() == Type.COUNTDOWN; }

    public long elapsed() { return accumulatedMs + (running ? System.currentTimeMillis() - startedAt : 0); }

    public long remaining() { return Math.max(0, durationMs - elapsed()); }

    public long displayMs() { return isCountdown() ? remaining() : elapsed(); }

    private void start() {
        if (running) return;
        if (finished) { restart(); return; }
        running = true;
        startedAt = System.currentTimeMillis();
    }

    private void pause() {
        if (!running) return;
        accumulatedMs = elapsed();
        running = false;
    }

    public void toggle() { if (running) pause(); else start(); }

    public void restart() {
        accumulatedMs = 0;
        finished = false;
        running = true;
        startedAt = System.currentTimeMillis();
    }

    public void setType(Type t) {
        if (type() == t) return;
        type = t;
        running = false;
        accumulatedMs = 0;
        finished = false;
    }

    public void setDuration(long ms) {
        if (durationMs == ms) return;
        durationMs = ms;
        finished = false;
    }

    void finish() {
        accumulatedMs = durationMs;
        running = false;
        finished = true;
    }

    public void setScale(float s) { scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, s)); }
}
