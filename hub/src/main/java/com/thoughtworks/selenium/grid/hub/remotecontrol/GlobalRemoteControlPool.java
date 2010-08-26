package com.thoughtworks.selenium.grid.hub.remotecontrol;

import com.thoughtworks.selenium.grid.hub.Environment;
import com.thoughtworks.selenium.grid.hub.NoSuchEnvironmentException;
import com.thoughtworks.selenium.grid.hub.NoSuchSessionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Monolithic Remote Control Pool keeping track of all environment and all sessions.
 */
public class GlobalRemoteControlPool implements DynamicRemoteControlPool {
    private static final Log LOGGER = LogFactory.getLog(GlobalRemoteControlPool.class);
    private final Map<String, RemoteControlSession> sessions = new HashMap<String, RemoteControlSession>();
    private final ConcurrentMap<String, RemoteControlProvisioner> provisioners = new ConcurrentHashMap<String, RemoteControlProvisioner>();

    public void register(RemoteControlProxy newRemoteControl) {
        provisioners.putIfAbsent(newRemoteControl.environment(), new RemoteControlProvisioner());

        final RemoteControlProvisioner provisioner = provisioners.get(newRemoteControl.environment());
        provisioner.add(newRemoteControl);
    }

    public boolean unregister(RemoteControlProxy remoteControl) {
        // First pull the remote control out of the list of RCs registered for the environment.
        final boolean unregistered = provisioners.get(remoteControl.environment()).remove(remoteControl);

        if (unregistered) {
            Set<RemoteControlSession> sessionsToRemove = new HashSet<RemoteControlSession>();

            // Now find all sessions associated with the RC.
            for (RemoteControlSession session : sessions.values()) {
                if (session.remoteControl().equals(remoteControl)) {
                    sessionsToRemove.add(session);
                }
            }

            // Remove the session separately from the loop where we found it to avoid issues with concurrent modification.
            for (RemoteControlSession session : sessionsToRemove) {
                removeFromSessionMap(session);
            }
        }

        return unregistered;
    }

    public RemoteControlProxy reserve(Environment environment) {
        final RemoteControlProvisioner provisioner = provisioners.get(environment.name());

        if (null == provisioner) {
            throw new NoSuchEnvironmentException(environment.name());
        }

        return provisioner.reserve();
    }

    public void associateWithSession(RemoteControlProxy remoteControl, String sessionId) {
        LOGGER.info("Associating session id='" + sessionId + "' =>" + remoteControl
                    + " for environment " + remoteControl.environment());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Asssociating " + sessionId + " => " + remoteControl);
        }
        synchronized (sessions) {
            if (sessions.containsKey(sessionId)) {
                throw new IllegalStateException(
                        "Session '" + sessionId + "' is already asssociated with " + sessions.get(sessionId));
            }

            sessions.put(sessionId, new RemoteControlSession(sessionId, remoteControl));
        }
        if (LOGGER.isDebugEnabled()) {
            logSessionMap();
        }
    }

    public RemoteControlProxy retrieve(String sessionId) {
        return getRemoteControlForSession(sessionId);
    }

    public void release(RemoteControlProxy remoteControl) {
        provisioners.get(remoteControl.environment()).release(remoteControl);
    }

    public void releaseForSession(String sessionId) {
        LOGGER.info("Releasing pool for session id='" + sessionId + "'");

        final RemoteControlProxy remoteControl;
        remoteControl = getRemoteControlForSession(sessionId);

        synchronized (sessions) {
            sessions.remove(sessionId);
        }
        remoteControl.terminateSession(sessionId);
        provisioners.get(remoteControl.environment()).release(remoteControl);
    }

    public List<RemoteControlProxy> availableRemoteControls() {
        final List<RemoteControlProxy> availableRemoteControls;

        availableRemoteControls = new LinkedList<RemoteControlProxy>();
        for (RemoteControlProvisioner provisioner : provisioners.values()) {
            availableRemoteControls.addAll(provisioner.availableRemoteControls());
        }

        return availableRemoteControls;
    }

    public List<RemoteControlProxy> reservedRemoteControls() {
        final List<RemoteControlProxy> reservedRemoteControls;

        reservedRemoteControls = new LinkedList<RemoteControlProxy>();
        for (RemoteControlProvisioner provisioner : provisioners.values()) {
            reservedRemoteControls.addAll(provisioner.reservedRemoteControls());
        }

        return reservedRemoteControls;
    }

    public List<RemoteControlProxy> allRegisteredRemoteControls() {
        final List<RemoteControlProxy> allRemoteControls = new LinkedList<RemoteControlProxy>();
        
        for (RemoteControlProvisioner provisioner : provisioners.values()) {
            allRemoteControls.addAll(provisioner.allRemoteControls());
        }

        return allRemoteControls;
    }

    public boolean isRegistered(RemoteControlProxy remoteControl) {
        for (RemoteControlProvisioner provisioner : provisioners.values()) {
            if (provisioner.contains(remoteControl)) {
                return true;
            }
        }
        return false;
    }

    protected RemoteControlProxy getRemoteControlForSession(String sessionId) {
        final RemoteControlSession session;

        session = sessions.get(sessionId);
        if (null == session) {
            throw new NoSuchSessionException(sessionId);
        }

        return session.remoteControl();
    }

    protected void removeFromSessionMap(RemoteControlSession session) {
        // Use a real iterator to avoid issues with concurrent modification.
        for (final Iterator<Map.Entry<String, RemoteControlSession>> it = sessions.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<String, RemoteControlSession> entry = it.next();

            if (entry.getValue().equals(session)) {
                it.remove();
            }
        }
    }

    protected void logSessionMap() {
        for (Map.Entry<String, RemoteControlSession> entry : sessions.entrySet()) {
            LOGGER.debug(entry.getKey() + " => " + entry.getValue());
        }
    }

    public void unregisterAllUnresponsiveRemoteControls() {
        for (RemoteControlProxy rc : allRegisteredRemoteControls()) {
            unregisterRemoteControlIfUnreliable(rc);
        }
    }

    protected void unregisterRemoteControlIfUnreliable(RemoteControlProxy rc) {
        if (rc.unreliable()) {
            LOGGER.warn("Unregistering unreliable RC " + rc);
            unregister(rc);
        }
    }

    public void updateSessionLastActiveAt(String sessionId) {
        sessions.get(sessionId).updateLastActiveAt();
    }

    public void recycleAllSessionsIdleForTooLong(double maxIdleTimeInSeconds) {
        for (RemoteControlSession session : iteratorSafeRemoteControlSessions()) {
            recycleSessionIfIdleForTooLong(session, maxIdleTimeInSeconds);
        }
    }

    public Set<RemoteControlSession> iteratorSafeRemoteControlSessions() {
        final Set<RemoteControlSession> iteratorSafeCopy;

        iteratorSafeCopy = new HashSet<RemoteControlSession>();
        synchronized (sessions) {
            for (Map.Entry<String, RemoteControlSession> entry : sessions.entrySet()) {
                iteratorSafeCopy.add(entry.getValue());
            }
        }
        return iteratorSafeCopy;
    }

    public void recycleSessionIfIdleForTooLong(RemoteControlSession session, double maxIdleTimeInSeconds) {
        final int maxIdleTImeInMilliseconds;
        
        maxIdleTImeInMilliseconds = (int) (maxIdleTimeInSeconds * 1000);
        if (session.innactiveForMoreThan(maxIdleTImeInMilliseconds)) {
            LOGGER.warn("Releasing session IDLE for more than " + maxIdleTimeInSeconds + " seconds: " + session);
            releaseForSession(session.sessionId());
        }
    }

    // This should only be used by tests.  It's a hack that we even need it, but there are some benefits in being
    // able to inject mocked objects into the map.
    protected final ConcurrentMap<String, RemoteControlProvisioner> getProvisioners() {
        return provisioners;
    }

    // This should only be used by tests.  It's a hack that we even need it, but there are some benefits in being
    // able to inject mocked objects into the map.
    protected final Map<String, RemoteControlSession> getSessions() {
        return sessions;
    }
}
