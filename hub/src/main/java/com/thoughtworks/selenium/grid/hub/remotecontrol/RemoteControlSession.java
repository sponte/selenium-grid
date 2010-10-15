package com.thoughtworks.selenium.grid.hub.remotecontrol;

import java.util.Date;

/**
 * Selenium Session in progress
 */
public class RemoteControlSession {
    private final IRemoteControlProxy remoteControl;
    private long lastActiveAt;
    private final String sessionId;

    public RemoteControlSession(String sessionId, IRemoteControlProxy remoteControl) {
        this.sessionId = sessionId;
        this.remoteControl = remoteControl;
        updateLastActiveAt();
    }

    public String sessionId() {
        return sessionId;
    }

    public IRemoteControlProxy remoteControl() {
        return remoteControl;
    }

    public long lastActiveAt() {
        return lastActiveAt;
    }

    public void updateLastActiveAt() {
        updateLastActiveAt(now());
    }

    public boolean innactiveSince(long millisecondsSinceEpoch) {
        return lastActiveAt() <= millisecondsSinceEpoch;
    }

    protected void updateLastActiveAt(long newLastActiveAt) {
        this.lastActiveAt = newLastActiveAt;
    }

    protected long now() {
        return new Date().getTime();
    }

    public boolean innactiveForMoreThan(int milliseconds) {
        return innactiveSince(now() - milliseconds);
    }
}
