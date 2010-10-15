package com.thoughtworks.selenium.grid.hub;

import com.thoughtworks.selenium.grid.HttpParameters;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.IDriverCommand;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.selenium.NewBrowserSessionCommand;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.selenium.SeleneseCommand;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.selenium.TestCompleteCommand;

import javax.servlet.http.HttpServletRequest;

/**
 * Parse HTTP commands targeting a Remote Control
 */
public class HttpCommandParser {

    public static final String NEW_BROWSER_SESSION = "getNewBrowserSession";
    private static final String TEST_COMPLETE = "testComplete";
    private final HttpServletRequest request;
    private final HttpParameters parameters;

    public HttpCommandParser(HttpServletRequest request) {
        this.request = request;
        if (null != request) {
            this.parameters = new HttpParameters(request.getParameterMap());
        } else {
            this.parameters = null;
        }
    }

    public IDriverCommand parse(EnvironmentManager environmentManager) {
        final String command = parameters().get("cmd");
        if (command.equals(NEW_BROWSER_SESSION)) {
            final Environment environment;
            final String environmentName;

            environmentName = parameters().get("1");
            environment = environmentManager.environment(environmentName);
            if (null == environment) {
                throw new CommandParsingException("ERROR: Unknown environment '" + environmentName + "'");
            }
            parameters.put("1", environment.browser());
            return new NewBrowserSessionCommand(environment, request);
        } else if (command.equals(TEST_COMPLETE)) {
            return new TestCompleteCommand(retrieveSessionId(), request);
        } else {
            return new SeleneseCommand(retrieveSessionId(), request);
        }
    }

    public HttpParameters parameters() {
        return parameters;
    }

    protected String retrieveSessionId() {
        final String sessionId = parameters.get("sessionId");
        if (null == sessionId) {
            throw new CommandParsingException("ERROR: No sessionId provided. Most likely your original newBrowserSession command failed.");
        }
        return sessionId;
    }
}
