package com.hamtabot.obfutilities.stats;

public class StatCounter {

    private static final float MAX_RATE = 20f;
    private static final long  IDLE_MS  = 4000;

    private int   session  = 0;
    private int   recent   = 0;
    private float rate     = 0f;
    private long  lastTime = 0;

    public void add(int amount) {
        session += amount;
        recent  += amount;
        lastTime = System.currentTimeMillis();
    }

    void closeRateWindow(float seconds) {
        rate   = Math.min(recent / seconds, MAX_RATE);
        recent = 0;
    }

    void decayIfIdle(long now) {
        if (now - lastTime > IDLE_MS) rate = 0f;
    }

    void reset() {
        session = 0;
        recent  = 0;
        rate    = 0f;
    }

    void setSession(int value) { session = value; }

    public int   getSession() { return session; }
    public float getRate()    { return rate; }
}
