package com.thoughtworks.selenium.grid.hub;

import com.thoughtworks.selenium.grid.GridRequestWrapper;
import com.thoughtworks.selenium.grid.HttpParameters;
import com.thoughtworks.selenium.grid.Response;
import com.thoughtworks.selenium.grid.hub.remotecontrol.DynamicRemoteControlPool;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.IDriverCommand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Main entry point for the Hub and the Selenium Farm.
 * Load balance selense requests accross a farm of remote control.
 */
public class WebDriverServlet extends HttpServlet {

    private final static Log LOGGER = LogFactory.getLog(HubServer.class);

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        process(request, response);
    }

    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        process(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        process(request, response);
    }

    protected void process(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final Response remoteControlResponse;

        final HubRegistry registry;
        final HttpParameters parameters;

        request = new GridRequestWrapper(request);

        registry = HubRegistry.registry();
        remoteControlResponse = forward(request, registry.remoteControlPool(), registry.environmentManager());
        reply(response, remoteControlResponse);
    }

    protected Response forward(HttpServletRequest request, DynamicRemoteControlPool pool, EnvironmentManager environmentManager) throws IOException {
        final IDriverCommand command;
        final Response response;

        LOGGER.info("Processing '" + request.toString() + "'");
        try {
            command = new WebDriverHttpCommandParser(request).parse(environmentManager);
            response = command.execute(pool);
        } catch (CommandParsingException e) {
            LOGGER.error("Failed to parse '" + request.toString() + "' : " + e.getMessage());
            return new Response(e.getMessage());
        } catch (NoSuchEnvironmentException e) {
            LOGGER.error("Could not find any remote control providing the '" + e.environment() +
                    "' environment. Please make sure you started some remote controls which registered as offering this environment.");
            return new Response(e.getMessage());
        } catch (NoSuchSessionException e) {
            LOGGER.error(e.getMessage());
            return new Response(e.getMessage());
        }

        final String responseBody = response.body();
        if (responseBody.length() > 256) {
            final int truncated = responseBody.length() - 256;
            LOGGER.info(String.format("Responding with %d / %s...[%d characters truncated]", response.statusCode(), responseBody.substring(0, 256), truncated));
        } else {
            LOGGER.info(String.format("Responding with %d / %s", response.statusCode(), responseBody));
        }

        return response;
    }

    protected void reply(HttpServletResponse response, Response remoteControlResponse) throws IOException {
        if (remoteControlResponse.statusCode() != 204) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().print(remoteControlResponse.body());
        }
        response.setStatus(remoteControlResponse.statusCode());
    }

    @SuppressWarnings({"unchecked"})
    protected HttpParameters requestParameters(HttpServletRequest request) {
        final HttpParameters parameters;
        parameters = new HttpParameters(request.getParameterMap());
        return parameters;
    }

}
