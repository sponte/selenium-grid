package com.thoughtworks.selenium.grid.hub.remotecontrol.commands.selenium;

import com.thoughtworks.selenium.grid.Response;
import com.thoughtworks.selenium.grid.hub.Environment;
import com.thoughtworks.selenium.grid.hub.remotecontrol.IRemoteControlProxy;
import com.thoughtworks.selenium.grid.hub.remotecontrol.RemoteControlPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Selenese command requesting a new session for a specific browser/environment.
 * Marks the start of a new Selenese session.
 */
public class NewBrowserSessionCommand extends SeleneseCommand {

    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("OK,([^,]*)");
    private static final Log logger = LogFactory.getLog(NewBrowserSessionCommand.class);
    private final Environment environment;

    public NewBrowserSessionCommand(Environment environment, HttpServletRequest request) {
        super(null, request);
        this.environment = environment;
    }

    public Response execute(RemoteControlPool pool) throws IOException {
        IRemoteControlProxy remoteControl;
        final String sessionId;
        final Response response;

        remoteControl = pool.reserve(environment);
        if (null == remoteControl) {
            final String message = "No available remote control for environment '" + environment.name() + "'";
            logger.warn(message);
            return new Response(message);
        }
        try {
            response = remoteControl.forward(request());
            sessionId = parseSessionId(response.body());
            if (null == sessionId) {
                pool.release(remoteControl);
                return new Response("Could not retrieve a new session");
            }
            pool.associateWithSession(remoteControl, sessionId);
            pool.updateSessionLastActiveAt(sessionId);

            return response;
        } catch (Exception e) {
            logger.error("Problem while requesting new browser session", e);
            pool.release(remoteControl);
            return new Response(e.getMessage());
        }
    }

    public String parseSessionId(String responseBody) {
        final Matcher matcher = SESSION_ID_PATTERN.matcher(responseBody);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    public Environment environment() {
        return environment;
    }

}
