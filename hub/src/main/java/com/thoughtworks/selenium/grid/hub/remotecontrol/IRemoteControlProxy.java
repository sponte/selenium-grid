package com.thoughtworks.selenium.grid.hub.remotecontrol;

import com.thoughtworks.selenium.grid.Response;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.RemoteControlMode;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: stanw
 * Date: 14-Oct-2010
 * Time: 09:53:53
 * To change this template use File | Settings | File Templates.
 */
public interface IRemoteControlProxy {
    public String host();

    public int port();

    public String environment();

    public String remoteControlPingURL();

    public String remoteControlDriverURL();

    public String remoteControlURLFor(String path);

    public Response forward(HttpServletRequest request) throws IOException;

    public String toString();

    public boolean equals(Object other);

    public int hashCode();

    public boolean sessionInProgress();

    public void registerNewSession();

    public void unregisterSession();

    public void terminateSession(String sessionId);

    public boolean canHandleNewSession();

    public boolean unreliable();

    void setRequest(HttpServletRequest request);

    void setMode(RemoteControlMode webDriver);
}
