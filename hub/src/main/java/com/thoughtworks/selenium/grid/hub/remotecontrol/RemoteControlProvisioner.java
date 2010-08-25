package com.thoughtworks.selenium.grid.hub.remotecontrol;

import com.thoughtworks.selenium.grid.hub.HubRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Central authority to track registered remote controls and grant exclusive
 * access to a remote control for a while.
 * <p/>
 * A client will block if it attempts to reserve a remote control and none is
 * available. The call will return as soon as a remote control becomes available
 * again.
 */
public class RemoteControlProvisioner {

    private static final Log LOGGER = LogFactory.getLog(RemoteControlProvisioner.class);
    private final List<RemoteControlProxy> remoteControls;
    private final Lock remoteControlListLock;
    private final Condition remoteControlAvailable;

    public RemoteControlProvisioner() {
        remoteControls = new LinkedList<RemoteControlProxy>();
        remoteControlListLock = new ReentrantLock();
        remoteControlAvailable = remoteControlListLock.newCondition();
    }

    public RemoteControlProxy reserve() {
        remoteControlListLock.lock();

        try {
            if (remoteControls.isEmpty()) {
                return null;
            }

            RemoteControlProxy remoteControl = blockUntilARemoteControlIsAvailableOrRequestTimesOut();
            if (null == remoteControl) {
                LOGGER.info("Timed out waiting for a remote control for environment.");
                return null;
            }

            while (remoteControl.unreliable()) {
                LOGGER.warn("Reserved RC " + remoteControl + " is detected as unreliable, unregistering it and reserving a new one...");
                tearDownExistingRemoteControl(remoteControl);
                if (remoteControls.isEmpty()) {
                    return null;
                }
                remoteControl = blockUntilARemoteControlIsAvailableOrRequestTimesOut();
                if (null == remoteControl) {
                    LOGGER.info("Timed out waiting for a remote control for environment.");
                    return null;
                }
            }
            remoteControl.registerNewSession();
            LOGGER.info("Reserved remote control" + remoteControl);
            return remoteControl;
        } finally {
            remoteControlListLock.unlock();
        }
    }

    public void release(RemoteControlProxy remoteControl) {
        remoteControlListLock.lock();

        try {
            remoteControl.unregisterSession();
            LOGGER.info("Released remote control" + remoteControl);
            signalThatARemoteControlHasBeenMadeAvailable();
        } finally {
            remoteControlListLock.unlock();
        }
    }

    public void add(RemoteControlProxy newRemoteControl) {
        remoteControlListLock.lock();

        try {
            if (remoteControls.contains(newRemoteControl)) {
                tearDownExistingRemoteControl(newRemoteControl);
            }
            remoteControls.add(newRemoteControl);
            signalThatARemoteControlHasBeenMadeAvailable();
        } finally {
            remoteControlListLock.unlock();
        }
    }

    /** Not Thread-safe */
    public boolean contains(RemoteControlProxy remoteControl) {
        return remoteControls.contains(remoteControl);
    }

    public void tearDownExistingRemoteControl(RemoteControlProxy newRemoteControl) {
        final RemoteControlProxy oldRemoteControl;

        oldRemoteControl = remoteControls.get(remoteControls.indexOf(newRemoteControl));
        remoteControls.remove(oldRemoteControl);
    }

    public boolean remove(RemoteControlProxy remoteControl) {
        remoteControlListLock.lock();

        try {
            return remoteControls.remove(remoteControl);
        } finally {
            remoteControlListLock.unlock();
        }
    }

    /**
     * Not thread safe.
     *
     * @return All available remote controls. Never null.
     */
    public List<RemoteControlProxy> availableRemoteControls() {
        final LinkedList<RemoteControlProxy> availableremoteControls;

        availableremoteControls = new LinkedList<RemoteControlProxy>();
        for (RemoteControlProxy remoteControl : remoteControls) {
            if (remoteControl.canHandleNewSession()) {
                availableremoteControls.add(remoteControl);
            }
        }
        return Arrays.asList(availableremoteControls.toArray(new RemoteControlProxy[availableremoteControls.size()]));
    }

    /**
     * Not thread safe.
     *
     * @return All reserved remote controls. Never null.
     */
    public List<RemoteControlProxy> reservedRemoteControls() {
        final LinkedList<RemoteControlProxy> reservedRemoteControls;

        reservedRemoteControls = new LinkedList<RemoteControlProxy>();
        for (RemoteControlProxy remoteControl : remoteControls) {
            if (remoteControl.sessionInProgress()) {
                reservedRemoteControls.add(remoteControl);
            }
        }
        return Arrays.asList(reservedRemoteControls.toArray(new RemoteControlProxy[reservedRemoteControls.size()]));
    }

    protected RemoteControlProxy blockUntilARemoteControlIsAvailableOrRequestTimesOut() {
        RemoteControlProxy availableRemoteControl;

        while (true) {
            try {
                availableRemoteControl = findNextAvailableRemoteControl();
                boolean timedOut = false;
                while ((null == availableRemoteControl) && !timedOut) {
                    LOGGER.info("Waiting for a remote control...");
                    timedOut = waitForARemoteControlToBeAvailable();
                    availableRemoteControl = findNextAvailableRemoteControl();
                }
                return availableRemoteControl;
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while reserving remote control", e);
            }
        }
    }

    /**
     * Non-blocking, not thread-safe
     *
     * @return Next Available remote control. Null if none is available.
     */
    protected RemoteControlProxy findNextAvailableRemoteControl() {
        for (RemoteControlProxy remoteControl : remoteControls) {
            if (remoteControl.canHandleNewSession()) {
                return remoteControl;
            }
        }
        return null;
    }

  /**
   * Wait for a remote control to be available or timeout while waiting.
   *
   * @return Indicates whether the request timed out.
   * 
   * @throws InterruptedException
   */
    protected boolean waitForARemoteControlToBeAvailable() throws InterruptedException {
        final Double maxWaitTime = HubRegistry.registry().gridConfiguration().getHub().getNewSessionMaxWaitTimeInSeconds();

        if (maxWaitTime.isInfinite()) {
            remoteControlAvailable.await();
            return false;
        } else {
            return !remoteControlAvailable.await(maxWaitTime.longValue(), TimeUnit.SECONDS);
        }
    }

    protected void signalThatARemoteControlHasBeenMadeAvailable() {
        remoteControlAvailable.signalAll();
    }


    public List<RemoteControlProxy> allRemoteControls() {
        final LinkedList<RemoteControlProxy> allRemoteControls;

        allRemoteControls = new LinkedList<RemoteControlProxy>();
        for (RemoteControlProxy remoteControl : remoteControls) {
            allRemoteControls.add(remoteControl);
        }

        return allRemoteControls;
    }
}
