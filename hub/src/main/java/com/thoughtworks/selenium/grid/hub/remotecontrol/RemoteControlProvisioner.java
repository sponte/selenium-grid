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
    private final List<IRemoteControlProxy> remoteControls;
    private final Lock remoteControlListLock;
    private final Condition remoteControlAvailable;

    public RemoteControlProvisioner() {
        remoteControls = new LinkedList<IRemoteControlProxy>();
        remoteControlListLock = new ReentrantLock();
        remoteControlAvailable = remoteControlListLock.newCondition();
    }

    public IRemoteControlProxy reserve() {
        remoteControlListLock.lock();

        try {
            if (remoteControls.isEmpty()) {
                return null;
            }

            IRemoteControlProxy remoteControl = blockUntilARemoteControlIsAvailableOrRequestTimesOut();
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

    public void release(IRemoteControlProxy remoteControl) {
        remoteControlListLock.lock();

        try {
            remoteControl.unregisterSession();
            LOGGER.info("Released remote control" + remoteControl);
            signalThatARemoteControlHasBeenMadeAvailable();
        } finally {
            remoteControlListLock.unlock();
        }
    }

    public void add(IRemoteControlProxy newRemoteControl) {
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

    /**
     * Not Thread-safe
     */
    public boolean contains(IRemoteControlProxy remoteControl) {
        return remoteControls.contains(remoteControl);
    }

    public void tearDownExistingRemoteControl(IRemoteControlProxy newRemoteControl) {
        final IRemoteControlProxy oldRemoteControl;

        oldRemoteControl = remoteControls.get(remoteControls.indexOf(newRemoteControl));
        remoteControls.remove(oldRemoteControl);
    }

    public boolean remove(IRemoteControlProxy remoteControl) {
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
    public List<IRemoteControlProxy> availableRemoteControls() {
        final LinkedList<IRemoteControlProxy> availableremoteControls;

        availableremoteControls = new LinkedList<IRemoteControlProxy>();
        for (IRemoteControlProxy remoteControl : remoteControls) {
            if (remoteControl.canHandleNewSession()) {
                availableremoteControls.add(remoteControl);
            }
        }
        return Arrays.asList(availableremoteControls.toArray(new IRemoteControlProxy[availableremoteControls.size()]));
    }

    /**
     * Not thread safe.
     *
     * @return All reserved remote controls. Never null.
     */
    public List<IRemoteControlProxy> reservedRemoteControls() {
        final LinkedList<IRemoteControlProxy> reservedRemoteControls;

        reservedRemoteControls = new LinkedList<IRemoteControlProxy>();
        for (IRemoteControlProxy remoteControl : remoteControls) {
            if (remoteControl.sessionInProgress()) {
                reservedRemoteControls.add(remoteControl);
            }
        }
        return Arrays.asList(reservedRemoteControls.toArray(new IRemoteControlProxy[reservedRemoteControls.size()]));
    }

    protected IRemoteControlProxy blockUntilARemoteControlIsAvailableOrRequestTimesOut() {
        IRemoteControlProxy availableRemoteControl;

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
    protected IRemoteControlProxy findNextAvailableRemoteControl() {
        for (IRemoteControlProxy remoteControl : remoteControls) {
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


    public List<IRemoteControlProxy> allRemoteControls() {
        final LinkedList<IRemoteControlProxy> allRemoteControls;

        allRemoteControls = new LinkedList<IRemoteControlProxy>();
        for (IRemoteControlProxy remoteControl : remoteControls) {
            allRemoteControls.add(remoteControl);
        }

        return allRemoteControls;
    }
}
