package com.thoughtworks.selenium.grid.hub.remotecontrol.commands.selenium;

import com.thoughtworks.selenium.grid.HttpParameters;
import com.thoughtworks.selenium.grid.MockHelper;
import com.thoughtworks.selenium.grid.Response;
import com.thoughtworks.selenium.grid.hub.remotecontrol.RemoteControlPool;
import com.thoughtworks.selenium.grid.hub.remotecontrol.RemoteControlProxy;
import org.jbehave.classmock.UsingClassMock;
import org.jbehave.core.mock.Mock;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;

import static junit.framework.Assert.assertEquals;


public class SeleneseCommandTest extends UsingClassMock {

    @Test
    public void parametersReturnsTheParametersProvidedToConstructor() {
        HttpServletRequest theRequest = (HttpServletRequest) mock(HttpServletRequest.class);
        HttpParameters theParameters = new HttpParameters();
        assertEquals(theParameters, new SeleneseCommand("", theRequest).parameters());
    }

    @Test
    public void sessionIdReturnsTheSessionIdProvidedToConstructor() {
        assertEquals("a session id", new SeleneseCommand("a session id", null).sessionId());
    }

    @Test
    public void executeForwardsTheRequestToTheRemoteControl() throws Exception {
        final Mock remoteControl;
        final SeleneseCommand command;
        final Response expectedResponse;
        final Mock pool;
        HttpServletRequest theRequest = MockHelper.GetMockHttpServletRequest();

        command = new SeleneseCommand("a session id", theRequest);
        expectedResponse = new Response(0, "");
        remoteControl = mock(RemoteControlProxy.class);
        pool = mock(RemoteControlPool.class);
        pool.expects("retrieve").with("a session id").will(returnValue(remoteControl));
        remoteControl.expects("forward").with(command.request()).will(returnValue(expectedResponse));

        assertEquals(expectedResponse, command.execute((RemoteControlPool) pool));
        verifyMocks();
    }


    @Test
    public void executeReturnsAnErrorResponseWhenNoSessionIdIsProvided() throws IOException {
        final HttpParameters parameters;
        final Response response;

        parameters = new HttpParameters();
        parameters.put("foo", "bar");
        HttpServletRequest theRequest = GetMockRequestWithParameters(parameters);
        response = new SeleneseCommand(null, theRequest).execute(null);
        assertEquals("ERROR: Selenium Driver error: No sessionId provided for command 'foo => \"bar\"'", response.body());
    }

    @Test
    public void executeUpdatesTheSessionLastActiveAt() throws Exception {
        final Mock remoteControl;
        final SeleneseCommand command;
        final Response expectedResponse;
        final Mock pool;
        HttpServletRequest theRequest = MockHelper.GetMockHttpServletRequest();

        command = new SeleneseCommand("a session id", theRequest);
        expectedResponse = new Response(0, "");
        remoteControl = mock(RemoteControlProxy.class);
        pool = mock(RemoteControlPool.class);
        pool.stubs("retrieve").with("a session id").will(returnValue(remoteControl));
        remoteControl.stubs("forward").with(command.request()).will(returnValue(expectedResponse));
        pool.expects("updateSessionLastActiveAt").with("a session id").times(2);

        command.execute((RemoteControlPool) pool);
        verifyMocks();
    }


    private HttpServletRequest GetMockRequestWithParameters(HttpParameters parameters) {
        Mock request = mock(HttpServletRequest.class);
        HashMap params = new HashMap<String, String[]>();
        for (String name : parameters.names()) {
            params.put(name, new String[]{parameters.get(name)});
        }
        request.stubs("getParameterMap").will(returnValue(params));
        return (HttpServletRequest) request;
    }

}
