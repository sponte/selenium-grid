package com.thoughtworks.selenium.grid.hub.remotecontrol.commands;

import com.thoughtworks.selenium.grid.HttpParameters;
import com.thoughtworks.selenium.grid.Response;
import com.thoughtworks.selenium.grid.hub.remotecontrol.RemoteControlPool;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: stanw
 * Date: 13-Oct-2010
 * Time: 09:57:22
 * To change this template use File | Settings | File Templates.
 */
public interface IDriverCommand {
    public String sessionId();

    public HttpParameters parameters();

    public HttpServletRequest request();

    public Response execute(RemoteControlPool pool) throws IOException;

    public String parseSessionId(String string);
}
