package com.thoughtworks.selenium.grid.hub;

import com.thoughtworks.selenium.grid.HttpParameters;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.IDriverCommand;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.webdriver.NewBrowserSessionCommand;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.webdriver.TestCompleteCommand;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.webdriver.WebDriverCommand;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse HTTP commands targeting a Remote Control
 */
public class WebDriverHttpCommandParser {

    public static final String NEW_BROWSER_SESSION = "/wd/hub/session";
    private static final String TEST_COMPLETE = "/wd/hub/session/\\d+";
    private final HttpServletRequest request;

    public WebDriverHttpCommandParser(HttpServletRequest request) {
        this.request = request;
    }

    public IDriverCommand parse(EnvironmentManager environmentManager) {
        if (IsNewBrowserSessionRequest(request)) {
            final Environment environment;
            final String environmentName;

            environmentName = GetEnvironmentName(request);
            environment = environmentManager.environment(environmentName);
            if (null == environment) {
                throw new CommandParsingException("ERROR: Unknown environment '" + environmentName + "'");
            }
            return new NewBrowserSessionCommand(environment, request);
        } else if (IsTestCompleteRequest(request)) {
            return new TestCompleteCommand(getSessionId(), request);
        } else {
            return new WebDriverCommand(getSessionId(), request);
        }
    }

    private String GetEnvironmentName(HttpServletRequest request) {
        JSONObject jsonObject;
        JSONObject parameters;
        StringBuilder environmentName = new StringBuilder();
        try {
            jsonObject = new JSONObject(GetRequestBody(request));
            parameters = jsonObject.getJSONObject("desiredCapabilities");
            environmentName.append(parameters.getString("browserName").toLowerCase());
            environmentName.append(" on ");
            environmentName.append(parameters.getString("platform").toLowerCase());
        } catch (JSONException e) {
            throw new CommandParsingException("Error parsing JSON: " + GetRequestBody(request));
        }
        return environmentName.toString();
    }

    private String GetRequestBody(HttpServletRequest request) {
        StringBuilder body = new StringBuilder();
        String line;
        try {
            BufferedReader reader = request.getReader();
            while (null != (line = reader.readLine())) {
                body.append(line);
            }
            reader.close();
        } catch (IOException e) {
            body.append("");
        }
        return body.toString();
    }

    private boolean IsNewBrowserSessionRequest(HttpServletRequest request) {
        if (request.getRequestURI().equals(NEW_BROWSER_SESSION)) {
            return true;
        }
        return false;
    }

    private boolean IsTestCompleteRequest(HttpServletRequest request) {
        if (Pattern.compile(TEST_COMPLETE).matcher(request.getRequestURI()).matches() &&
                request.getMethod().equals("DELETE")) {
            return true;
        }
        return false;
    }

    public HttpParameters parameters() {
        return new HttpParameters(request.getParameterMap());
    }

    protected String getSessionId() {
        final String sessionId = GetSessionId(request);
        if (null == sessionId) {
            throw new CommandParsingException("ERROR: No sessionId provided. Most likely your original newBrowserSession command failed.");
        }
        return sessionId;
    }

    private String GetSessionId(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String returnString = null;
        Pattern pattern = Pattern.compile("/wd/hub/session/(\\d+).*", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(requestUri);
        if (matcher.matches()) {
            returnString = matcher.group(1);
        }
        return returnString;
    }


}
