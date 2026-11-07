package com.clientfiles.ui.kit;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import javax.swing.Timer;

/**
 * Every animation in the application runs on this single timer. Each one can be cancelled or
 * replaced mid-flight, and with motion turned off it jumps straight to its final state.
 */
public final class Anim {
    @FunctionalInterface
    public interface Ease {
        double at(double t);
    }

    public static final Ease OUT = t -> 1 - Math.pow(1 - t, 3);
    public static final Ease IN = t -> t * t * t;
    public static final Ease IN_OUT = t -> t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    /** Slight overshoot, for things that should feel like they settle into place. */
    public static final Ease SPRING = t -> {
        double s = 1.4;
        double u = t - 1;
        return 1 + (s + 1) * u * u * u + s * u * u;
    };

    public static final int FAST = 130;
    public static final int MEDIUM = 240;
    public static final int SLOW = 420;

    private static final Timer TIMER = new Timer(15, e -> tick());
    private static final List<Handle> ACTIVE = new ArrayList<>();
    private static boolean motion = !"off".equals(System.getProperty("clientfiles.motion"));

    private Anim() {
    }

    public static boolean motionEnabled() {
        return motion;
    }

    public static void setMotionEnabled(boolean enabled) {
        motion = enabled;
    }

    public static final class Handle {
        private long start;
        private final int duration;
        private final Ease ease;
        private final DoubleConsumer frame;
        private final Runnable done;
        private final Component loopHost;
        private boolean cancelled;

        private Handle(int delay, int duration, Ease ease, DoubleConsumer frame, Runnable done, Component loopHost) {
            this.start = System.nanoTime() / 1_000_000 + delay;
            this.duration = duration;
            this.ease = ease;
            this.frame = frame;
            this.done = done;
            this.loopHost = loopHost;
        }

        public void cancel() {
            cancelled = true;
        }

        private boolean step(long now) {
            if (cancelled) {
                return false;
            }
            long elapsed = now - start;
            if (loopHost != null) {
                if (!loopHost.isShowing() && elapsed > 2000) {
                    return false;
                }
                if (loopHost.isShowing()) {
                    frame.accept(elapsed);
                }
                return true;
            }
            if (elapsed < 0) {
                return true;
            }
            if (elapsed >= duration) {
                frame.accept(1);
                if (done != null) {
                    done.run();
                }
                return false;
            }
            frame.accept(ease.at(elapsed / (double) duration));
            return true;
        }
    }

    public static Handle run(int duration, Ease ease, DoubleConsumer frame) {
        return run(0, duration, ease, frame, null);
    }

    public static Handle run(int delay, int duration, Ease ease, DoubleConsumer frame, Runnable done) {
        Handle handle = new Handle(delay, duration, ease, frame, done, null);
        if (!motion) {
            frame.accept(1);
            if (done != null) {
                done.run();
            }
            handle.cancelled = true;
            return handle;
        }
        add(handle);
        return handle;
    }

    /** Runs until the host has left the screen; the consumer receives elapsed milliseconds. */
    public static Handle forever(Component host, DoubleConsumer frame) {
        Handle handle = new Handle(0, 0, OUT, frame, null, host);
        if (motion) {
            add(handle);
        }
        return handle;
    }

    private static void add(Handle handle) {
        ACTIVE.add(handle);
        if (!TIMER.isRunning()) {
            TIMER.start();
        }
    }

    private static void tick() {
        long now = System.nanoTime() / 1_000_000;
        for (Handle handle : new ArrayList<>(ACTIVE)) {
            if (!handle.step(now)) {
                ACTIVE.remove(handle);
            }
        }
        if (ACTIVE.isEmpty()) {
            TIMER.stop();
        }
    }

    /** A number that eases towards its target and repaints its host; retargeting never jumps. */
    public static final class Value {
        private final Component host;
        private double value;
        private double target;
        private Handle handle;

        public Value(Component host, double initial) {
            this.host = host;
            this.value = initial;
            this.target = initial;
        }

        public double get() {
            return value;
        }

        public void to(double next) {
            to(next, FAST, OUT);
        }

        public void to(double next, int duration, Ease ease) {
            if (next == target) {
                return;
            }
            target = next;
            if (handle != null) {
                handle.cancel();
            }
            double from = value;
            handle = run(duration, ease, t -> {
                value = from + (next - from) * t;
                host.repaint();
            });
        }

        public void set(double next) {
            if (handle != null) {
                handle.cancel();
            }
            value = next;
            target = next;
            host.repaint();
        }
    }
}
