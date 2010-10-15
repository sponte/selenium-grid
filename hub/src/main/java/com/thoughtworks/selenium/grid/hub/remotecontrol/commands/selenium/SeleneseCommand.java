package com.thoughtworks.selenium.grid.hub.remotecontrol.commands.selenium;

import com.thoughtworks.selenium.grid.HttpParameters;
import com.thoughtworks.selenium.grid.Response;
import com.thoughtworks.selenium.grid.hub.remotecontrol.IRemoteControlProxy;
import com.thoughtworks.selenium.grid.hub.remotecontrol.RemoteControlPool;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.IDriverCommand;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Generic Selenese command
 */
public class SeleneseCommand implements IDriverCommand {

    private final String sessionId;
    private HttpServletRequest request;
    private final HttpParameters parameters;

    public SeleneseCommand(String sessionId, HttpServletRequest request) {
        this.sessionId = sessionId;
        this.request = request;
        if (null != request) {
            this.parameters = new HttpParameters(request.getParameterMap());
        } else {
            this.parameters = null;
        }
    }

    public String sessionId() {
        return sessionId;
    }

    public HttpParameters parameters() {
        return parameters;
    }

    public HttpServletRequest request() {
        return request;
    }

    public Response execute(RemoteControlPool pool) throws IOException {
        final IRemoteControlProxy remoteControl;
        final Response response;

        if (null == sessionId) {
            return new Response("Selenium Driver error: No sessionId provided for command '" + parameters().toString() + "'");
        }
        remoteControl = pool.retrieve(sessionId());
        pool.updateSessionLastActiveAt(sessionId);
        response = remoteControl.forward(request());
        pool.updateSessionLastActiveAt(sessionId);

        return response;
    }

    public String parseSessionId(String string) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
