package com.thoughtworks.selenium.grid.hub.remotecontrol;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RemoteControlPoller implements Runnable {
    private final long pollingIntervalInMilliseconds;
    private final double sessionMaxIdleTimeInSeconds;
    private final DynamicRemoteControlPool pool;
    private volatile boolean active;

    private static final Log LOGGER = LogFactory.getLog(RemoteControlPoller.class);

    public RemoteControlPoller(DynamicRemoteControlPool pool,
                               double pollingIntervalInSeconds,
                               double sessionMaxIdleTimeInSeconds) {
        this.pollingIntervalInMilliseconds = (long) (pollingIntervalInSeconds * 1000);
        this.pool = pool;
        this.active = true;
        this.sessionMaxIdleTimeInSeconds = sessionMaxIdleTimeInSeconds;
    }

    public boolean active() {
        return active;
    }

    public void stop() {
        active = false;
    }

    public void run() {
        while (active) {
            garbageCollectRemoteControls();
        }
    }

    public void garbageCollectRemoteControls() {
        sleepForALittleWhile();
        LOGGER.info("Garbage collecting unavailable RCs and stale sessions...");
        pool.unregisterAllUnresponsiveRemoteControls();
        pool.recycleAllSessionsIdleForTooLong(sessionMaxIdleTimeInSeconds);
    }
    
    protected void sleepForALittleWhile() {
        try {
            Thread.sleep(pollingIntervalInMilliseconds());
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted!");
        }
    }

    public long pollingIntervalInMilliseconds() {
        return pollingIntervalInMilliseconds;
    }

    public double sessionMaxIdleTimeInSeconds() {
        return sessionMaxIdleTimeInSeconds;
    }


}
