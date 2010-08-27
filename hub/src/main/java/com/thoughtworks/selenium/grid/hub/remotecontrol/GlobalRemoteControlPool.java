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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Monolithic Remote Control Pool keeping track of all environment and all sessions.
 */
public class GlobalRemoteControlPool implements DynamicRemoteControlPool {
    private static final Log LOGGER = LogFactory.getLog(GlobalRemoteControlPool.class);
    private final ConcurrentMap<String, RemoteControlSession> sessions = new ConcurrentHashMap<String, RemoteControlSession>();
    private final ConcurrentMap<String, RemoteControlProvisioner> provisioners = new ConcurrentHashMap<String, RemoteControlProvisioner>();

    private final ConcurrentMap<RemoteControlProxy, Lock> remoteControlLocks = new ConcurrentHashMap<RemoteControlProxy, Lock>();

    /**
     * Registers a remote control with the provisioner for its environment.
     *
     * If the provisioner does not exist yet (i.e., this is the first RC being registered for the environment), then
     * the provisioner is atomically created and recorded.  Once a provisioner is created it is never destroyed.
     *
     * The provisioner is thread-safe and enforces consistency on the RC list.
     *
     * There is no guarantee that this method completes execution before any other method that may wish to use the
     * registered RC.  This is considered to be an acceptable trade-off in order to avoid excessive locking, which can
     * be problematic when starting up a large number of RCs at a single time.
     *
     * @param newRemoteControl
     */
    public void register(RemoteControlProxy newRemoteControl) {
        provisioners.putIfAbsent(newRemoteControl.environment(), new RemoteControlProvisioner());

        final RemoteControlProvisioner provisioner = provisioners.get(newRemoteControl.environment());
        provisioner.add(newRemoteControl);
    }

    /**
     * Unregisters a remote control with the provisioner for its environment.
     *
     * This method locks around any logically equivalent remote control, preventing issues with multiple
     * unregister calls.  More importantly, the lock allows us to maintain invariant that an unregistered remote control
     * may not have any registered sessions.  This lock is also used by #associateWithSession.
     * 
     * @param remoteControl
     *
     * @return <code>true</code> if the remoteControl unregistered, <code>false</code> otherwise.
     */
    public boolean unregister(RemoteControlProxy remoteControl) {
        // The invariant for the provisioner map is that once a provisioner is created it is never deleted.  However, there
        // is no guarantee that a call to #register completes before any other calls, so it is possible to fetch a null provisioner.
        final RemoteControlProvisioner provisioner = provisioners.get(remoteControl.environment());
        if (null == provisioner) {
            return false;
        }

        final Lock lock = getRemoteControlLock(remoteControl);
        lock.lock();

        try {
            // First pull the remote control out of the list of RCs registered for the environment.
            final boolean successful = provisioner.remove(remoteControl);

            if (successful) {
                // Find all sessions associated with the RC and and remove them.
                // Use a real iterator to avoid issues with concurrent modification.
                for (final Iterator<Map.Entry<String, RemoteControlSession>> it = sessions.entrySet().iterator(); it.hasNext();) {
                    final Map.Entry<String, RemoteControlSession> entry = it.next();

                    if (entry.getValue().remoteControl().equals(remoteControl)) {
                        it.remove();
                    }
                }
            }

            return successful;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Records the association between a session ID and a remote control.
     *
     * This method locks around any logically equivalent remote control, allowing us to maintain invariant that
     * an unregistered remote control may not have any registered sessions.  This lock is also used by #unregister.
     *
     * @param remoteControl Reserved remote control to be associated with session. Should not be null.
     * @param sessionId     Id of the session to associate the remote control with. Should not be null.
     */
    public void associateWithSession(RemoteControlProxy remoteControl, String sessionId) {
        LOGGER.info("Associating session id='" + sessionId + "' =>" + remoteControl
                    + " for environment " + remoteControl.environment());

        final Lock lock = getRemoteControlLock(remoteControl);
        lock.lock();

        try {
            if (sessions.containsKey(sessionId)) {
                throw new IllegalStateException("Session '" + sessionId + "' is already associated with " + sessions.get(sessionId));
            }

            final RemoteControlProvisioner provisioner = provisioners.get(remoteControl.environment());
            if ((null == provisioner) || !provisioner.contains(remoteControl)) {
                throw new IllegalStateException(String.format("Remote control cannot be associated with session '%s' because it is not registered.", sessionId));
            }

            sessions.put(sessionId, new RemoteControlSession(sessionId, remoteControl));

            if (LOGGER.isDebugEnabled()) {
                logSessionMap();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Requests a remote control from the provisioner.  This is a blocking call, unless the provisioner does not
     * have any registered remote controls, in which case <code>null</code> is returned immmediately.
     *
     * @param environment  Environment that the remote control must provide. Should not be null.
     * @return
     */
    public RemoteControlProxy reserve(Environment environment) {
        final RemoteControlProvisioner provisioner = provisioners.get(environment.name());

        if (null == provisioner) {
            throw new NoSuchEnvironmentException(environment.name());
        }

        return provisioner.reserve();
    }

    public RemoteControlProxy retrieve(String sessionId) {
        final RemoteControlSession session = sessions.get(sessionId);

        if (null == session) {
            throw new NoSuchSessionException(sessionId);
        }

        return session.remoteControl();
    }

    public void release(RemoteControlProxy remoteControl) {
        provisioners.get(remoteControl.environment()).release(remoteControl);
    }

    public void releaseForSession(String sessionId) {
        LOGGER.info("Releasing pool for session id='" + sessionId + "'");

        final RemoteControlProxy remoteControl = retrieve(sessionId);

        sessions.remove(sessionId);
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
        final RemoteControlProvisioner provisioner = provisioners.get(remoteControl.environment());

        if (null == provisioner) {
            return false;
        }

        return provisioner.contains(remoteControl);
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
        for (RemoteControlSession session : sessions.values()) {
            recycleSessionIfIdleForTooLong(session, maxIdleTimeInSeconds);
        }
    }

    public void recycleSessionIfIdleForTooLong(RemoteControlSession session, double maxIdleTimeInSeconds) {
        final int maxIdleTImeInMilliseconds;
        
        maxIdleTImeInMilliseconds = (int) (maxIdleTimeInSeconds * 1000);
        if (session.innactiveForMoreThan(maxIdleTImeInMilliseconds)) {
            LOGGER.warn("Releasing session IDLE for more than " + maxIdleTimeInSeconds + " seconds: " + session);
            releaseForSession(session.sessionId());
        }
    }
    
    private Lock getRemoteControlLock(final RemoteControlProxy remoteControl) {
        remoteControlLocks.putIfAbsent(remoteControl, new ReentrantLock());

        return remoteControlLocks.get(remoteControl);
    }

    protected void logSessionMap() {
        for (Map.Entry<String, RemoteControlSession> entry : sessions.entrySet()) {
            LOGGER.debug(entry.getKey() + " => " + entry.getValue());
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
