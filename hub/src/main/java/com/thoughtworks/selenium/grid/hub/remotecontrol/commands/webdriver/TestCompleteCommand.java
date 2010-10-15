package com.thoughtworks.selenium.grid.hub.remotecontrol.commands.webdriver;

import com.thoughtworks.selenium.grid.Response;
import com.thoughtworks.selenium.grid.hub.remotecontrol.RemoteControlPool;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Selenese command marking the end of a Selenese session.
 */
public class TestCompleteCommand extends WebDriverCommand {

    public TestCompleteCommand(String sessionId, HttpServletRequest request) {
        super(sessionId, request);
    }

    public Response execute(RemoteControlPool pool) throws IOException {
        try {
            return super.execute(pool);
        } finally {
            pool.releaseForSession(sessionId());
        }
    }

}
