package com.thoughtworks.selenium.grid.hub.remotecontrol.commands.webdriver;

import com.thoughtworks.selenium.grid.HttpParameters;
import com.thoughtworks.selenium.grid.Response;
import com.thoughtworks.selenium.grid.hub.remotecontrol.IRemoteControlProxy;
import com.thoughtworks.selenium.grid.hub.remotecontrol.RemoteControlPool;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.IDriverCommand;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.RemoteControlMode;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Generic Selenese command
 */
public class WebDriverCommand implements IDriverCommand {

    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("/wd/hub/session/(\\d+)");

    private final String sessionId;
    private final HttpServletRequest request;
    private final HttpParameters parameters;

    public WebDriverCommand(String sessionId, HttpServletRequest request) {
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

    public HttpServletRequest request() {
        return request;
    }

    public HttpParameters parameters() {
        return parameters;
    }

    public Response execute(RemoteControlPool pool) throws IOException {
        final IRemoteControlProxy remoteControl;
        final Response response;

        if (null == sessionId) {
            return new Response("Selenium Driver error: No sessionId provided for command '" + parameters().toString() + "'");
        }
        remoteControl = pool.retrieve(sessionId());
        remoteControl.setMode(RemoteControlMode.WebDriver);
        remoteControl.setRequest(request());
        pool.updateSessionLastActiveAt(sessionId);
        response = remoteControl.forward(request());
        pool.updateSessionLastActiveAt(sessionId);

        return response;
    }

    public String parseSessionId(String string) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(string);
            return jsonObject.getString("sessionId");
        } catch (JSONException e) {
            return null;
        }
    }

    public String parseSessionId(Response response) {
        return parseSessionId(response.body());
    }
}
