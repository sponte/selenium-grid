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
    private final ConcurrentMap<String, RemoteControlSession> remoteControlsBySessionIds = new ConcurrentHashMap<String, RemoteControlSession>();
    private final ConcurrentMap<String, RemoteControlProvisioner> provisionersByEnvironment = new ConcurrentHashMap<String, RemoteControlProvisioner>();

    public void register(RemoteControlProxy newRemoteControl) {
        final RemoteControlProvisioner provisioner;

        synchronized(provisionersByEnvironment) {
            if (null == getProvisioner(newRemoteControl.environment())) {
                createNewProvisionerForEnvironment(newRemoteControl.environment());
            }
            provisioner = getProvisioner(newRemoteControl.environment());
            provisioner.add(newRemoteControl);
        }
    }

    public boolean unregister(RemoteControlProxy remoteControl) {
        final boolean status;

        synchronized(provisionersByEnvironment) {
            synchronized (remoteControlsBySessionIds) {
                Set<RemoteControlSession> sessionsToRemove = new HashSet<RemoteControlSession>();

                status = getProvisioner(remoteControl.environment()).remove(remoteControl);
                for (RemoteControlSession session : remoteControlsBySessionIds.values()) {
                    if (session.remoteControl().equals(remoteControl)) {
                        sessionsToRemove.add(session);
                    }
                }

                // Remove the session separately from the loop where we found it to avoid issues with concurrent modification.
                for (RemoteControlSession session : sessionsToRemove) {
                    removeFromSessionMap(session);
                }
            }
        }
        return status;
    }

    public RemoteControlProxy reserve(Environment environment) {
        final RemoteControlProvisioner provisioner;
        
        provisioner = getProvisioner(environment.name());
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
        synchronized (remoteControlsBySessionIds) {
            if (remoteControlsBySessionIds.containsKey(sessionId)) {
                throw new IllegalStateException(
                        "Session '" + sessionId + "' is already asssociated with " + remoteControlsBySessionIds.get(sessionId));
            }

            remoteControlsBySessionIds.put(sessionId, new RemoteControlSession(sessionId, remoteControl));
        }
        if (LOGGER.isDebugEnabled()) {
            logSessionMap();
        }
    }

    public RemoteControlProxy retrieve(String sessionId) {
        return getRemoteControlForSession(sessionId);
    }

    public void release(RemoteControlProxy remoteControl) {
        getProvisioner(remoteControl.environment()).release(remoteControl);
    }

    public void releaseForSession(String sessionId) {
        LOGGER.info("Releasing pool for session id='" + sessionId + "'");

        final RemoteControlProxy remoteControl;
        remoteControl = getRemoteControlForSession(sessionId);

        synchronized (remoteControlsBySessionIds) {
            remoteControlsBySessionIds.remove(sessionId);
        }
        remoteControl.terminateSession(sessionId);
        getProvisioner(remoteControl.environment()).release(remoteControl);
    }

    public List<RemoteControlProxy> availableRemoteControls() {
        final List<RemoteControlProxy> availableRemoteControls;

        availableRemoteControls = new LinkedList<RemoteControlProxy>();
        for (RemoteControlProvisioner provisioner : provisionersByEnvironment.values()) {
            availableRemoteControls.addAll(provisioner.availableRemoteControls());
        }

        return availableRemoteControls;
    }

    public List<RemoteControlProxy> reservedRemoteControls() {
        final List<RemoteControlProxy> reservedRemoteControls;

        reservedRemoteControls = new LinkedList<RemoteControlProxy>();
        for (RemoteControlProvisioner provisioner : provisionersByEnvironment.values()) {
            reservedRemoteControls.addAll(provisioner.reservedRemoteControls());
        }

        return reservedRemoteControls;
    }

    public List<RemoteControlProxy> allRegisteredRemoteControls() {
        final List<RemoteControlProxy> allRemoteControls;

        allRemoteControls = new LinkedList<RemoteControlProxy>();
        synchronized(provisionersByEnvironment) {
            for (RemoteControlProvisioner provisioner : provisionersByEnvironment.values()) {
                allRemoteControls.addAll(provisioner.allRemoteControls());
            }
        }

        return allRemoteControls;
    }

    public boolean isRegistered(RemoteControlProxy remoteControl) {
        for (RemoteControlProvisioner provisioner : provisionersByEnvironment.values()) {
            if (provisioner.contains(remoteControl)) {
                return true;
            }
        }
        return false;
    }

    public RemoteControlProvisioner getProvisioner(String environment) {
        return provisionersByEnvironment.get(environment);
    }

    protected RemoteControlProxy getRemoteControlForSession(String sessionId) {
        final RemoteControlSession session;

        session = getRemoteControlSession(sessionId);
        if (null == session) {
            throw new NoSuchSessionException(sessionId);
        }

        return session.remoteControl();
    }

    protected RemoteControlSession getRemoteControlSession(String sessionId) {
        return remoteControlsBySessionIds.get(sessionId);
    }

    protected void removeFromSessionMap(RemoteControlSession session) {
        // Use a real iterator to avoid issues with concurrent modification.
        for (final Iterator<Map.Entry<String, RemoteControlSession>> it = remoteControlsBySessionIds.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<String, RemoteControlSession> entry = it.next();

            if (entry.getValue().equals(session)) {
                it.remove();
            }
        }
    }

    protected void logSessionMap() {
        for (Map.Entry<String, RemoteControlSession> entry : remoteControlsBySessionIds.entrySet()) {
            LOGGER.debug(entry.getKey() + " => " + entry.getValue());
        }
    }

    protected void createNewProvisionerForEnvironment(String environemntName) {
        provisionersByEnvironment.put(environemntName, new RemoteControlProvisioner());
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
        getRemoteControlSession(sessionId).updateLastActiveAt();
    }

    public void recycleAllSessionsIdleForTooLong(double maxIdleTimeInSeconds) {
        for (RemoteControlSession session : iteratorSafeRemoteControlSessions()) {
            recycleSessionIfIdleForTooLong(session, maxIdleTimeInSeconds);
        }
    }

    public Set<RemoteControlSession> iteratorSafeRemoteControlSessions() {
        final Set<RemoteControlSession> iteratorSafeCopy;

        iteratorSafeCopy = new HashSet<RemoteControlSession>();
        synchronized (remoteControlsBySessionIds) {
            for (Map.Entry<String, RemoteControlSession> entry : remoteControlsBySessionIds.entrySet()) {
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

}
