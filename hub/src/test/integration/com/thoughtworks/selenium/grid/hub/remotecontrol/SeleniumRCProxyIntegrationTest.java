package com.thoughtworks.selenium.grid.hub.remotecontrol;

import com.thoughtworks.selenium.grid.HttpClient;
import com.thoughtworks.selenium.grid.HttpParameters;
import com.thoughtworks.selenium.grid.MockHelper;
import com.thoughtworks.selenium.grid.Response;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class SeleniumRCProxyIntegrationTest {

    private static final int VALID_PORT = 5555;

    @Test
    public void forwardGetNewBrowserSessionToRemoteControl() throws IOException {
        HttpClient client = new HttpClient();
        RemoteControlProxy proxy = new RemoteControlProxy("localhost", VALID_PORT, "env", client);
        HttpParameters parameters = new HttpParameters();
        parameters.put("cmd", "getNewBrowserSession");
        parameters.put("1", "*firefox");
        parameters.put("2", "http://www.google.com");
        HttpServletRequest request = MockHelper.GetMockRequestWithParameters(parameters);
        Response response = proxy.forward(request);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().startsWith("OK,"));
    }

}