package com.thoughtworks.selenium.grid.hub;

import com.thoughtworks.selenium.grid.hub.remotecontrol.DynamicRemoteControlPool;
import com.thoughtworks.selenium.grid.hub.remotecontrol.RemoteControlProxy;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Provides feedback that the Hub is still up and running
 * with minimal performance impact on other Hub operations.
 */
public class HeartbeatServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        reply(request.getParameter("host"), request.getParameter("port"), response);
    }

    protected void reply(String host, String port, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        if (registeredRemoteControl(host, port)) {
            response.getWriter().write("Hub : OK");
        } else {
            response.getWriter().write("Hub : Not Registered");
        }
    }

    protected boolean registeredRemoteControl(String host, String port) {
        final RemoteControlProxy remoteControl;

        try {
            remoteControl = new RemoteControlProxy(host, Integer.parseInt(port), "dummy", null);
        } catch (NumberFormatException e) {
            return false;
        }
        return remoteControlPool().isRegistered(remoteControl);
    }

    protected DynamicRemoteControlPool remoteControlPool() {
        return HubRegistry.registry().remoteControlPool();
    }

}
