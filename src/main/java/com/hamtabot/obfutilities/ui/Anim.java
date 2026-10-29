package com.hamtabot.obfutilities.ui;

public final class Anim {

    private float value;
    private long last = System.nanoTime();

    public Anim(float initial) { value = initial; }

    public float update(float target, float speed) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
        last = now;
        value += (target - value) * Math.min(1f, dt * speed);
        if (Math.abs(target - value) < 0.002f) value = target;
        return value;
    }

    public void set(float v) { value = v; }

    public float value() { return value; }
}
