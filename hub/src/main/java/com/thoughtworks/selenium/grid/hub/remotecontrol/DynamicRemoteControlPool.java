package com.thoughtworks.selenium.grid.hub.remotecontrol;

import java.util.List;

/**
 * Remote control pool that grows/shrinks when remote control
 * register/unregister themselves.
 */
public interface DynamicRemoteControlPool extends RemoteControlPool {

    void register(IRemoteControlProxy newRemoteControl);

    boolean unregister(IRemoteControlProxy remoteControl);

    boolean isRegistered(IRemoteControlProxy remoteControl);

    List<IRemoteControlProxy> allRegisteredRemoteControls();

    List<IRemoteControlProxy> availableRemoteControls();

    List<IRemoteControlProxy> reservedRemoteControls();

    void unregisterAllUnresponsiveRemoteControls();

    void recycleAllSessionsIdleForTooLong(double maxIdleTimeInSeconds);

}
