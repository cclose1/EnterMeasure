package com.cbc.android;

import android.os.Handler;

public class Timer extends Handler {
    public interface Action {
        public void start();
        public void update(long lapsedMilliSeconds);
        public void reset();
        public void stop();
    }
    private Action  action  = null;
    private boolean running = false;
    private long    startTime;
    private long    loopDelay = 500;

    private Runnable tloop = new Runnable() {
        @Override
        public void run() {
            if (action != null) update();
        }
    };
    public Timer(Action action) {
        this.action = action;
    }
    public void setAction(Action actor) {
        action = actor;
    }
    public void start() {
        if (running) return;

        startTime = System.currentTimeMillis();
        running   = true;
        postDelayed(tloop, 0);

        if (action != null) action.start();
    }
    public void update() {
        if (!running) start();

        if (action != null) action.update(System.currentTimeMillis() - startTime);

        postDelayed(tloop, loopDelay);
    }
    public void stop() {
        running = false;
        removeCallbacks(tloop);

        if (action != null) action.stop();
    }
    public void reset() {
        startTime = System.currentTimeMillis();

        if (action != null) action.reset();

        start();
    }
    public void setLoopDelay(long loopDelay) {
        this.loopDelay = loopDelay;
    }
    public Action getAction() {
        return action;
    }
}

