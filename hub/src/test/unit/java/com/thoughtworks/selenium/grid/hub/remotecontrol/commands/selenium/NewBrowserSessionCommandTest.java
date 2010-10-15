package com.thoughtworks.selenium.grid.hub.remotecontrol.commands.selenium;

import com.thoughtworks.selenium.grid.HttpParameters;
import com.thoughtworks.selenium.grid.Response;
import com.thoughtworks.selenium.grid.hub.Environment;
import com.thoughtworks.selenium.grid.hub.remotecontrol.DynamicRemoteControlPool;
import com.thoughtworks.selenium.grid.hub.remotecontrol.RemoteControlPool;
import com.thoughtworks.selenium.grid.hub.remotecontrol.RemoteControlProxy;
import org.jbehave.classmock.UsingClassMock;
import org.jbehave.core.mock.Mock;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;

import static junit.framework.Assert.assertEquals;


public class NewBrowserSessionCommandTest extends UsingClassMock {

    @Test
    public void environmentReturnsTheEnvironmentProvidedInTheConstructor() {
        final Environment anEnvironment;

        anEnvironment = new Environment("", null);
        assertEquals(anEnvironment, new NewBrowserSessionCommand(anEnvironment, null).environment());
    }

    @Test
    public void sessionIdIsAlwaysNull() {
        assertEquals(null, new NewBrowserSessionCommand(null, null).sessionId());
    }

    @Test
    public void parseSessionIdReturnsTheSessionIdWhenResponseIsSuccessful() {
        assertEquals("22207", new NewBrowserSessionCommand(null, null).parseSessionId("OK,22207"));
    }

    @Test
    public void parseSessionIdReturnsTheSessionIdWhenSessionIdIsAGUID() {
        assertEquals("36d3cc124c8b4b4f8c53c5b600052da3", new NewBrowserSessionCommand(null, null).parseSessionId("OK,36d3cc124c8b4b4f8c53c5b600052da3"));
    }

    @Test
    public void parseSessionIdReturnsNullWhenResponseIsNotSuccessful() {
        assertEquals(null, new NewBrowserSessionCommand(null, null).parseSessionId(""));
    }

    @Test
    public void executeReserveAndThenAssociateARemoteControlWithTheSession() throws IOException {
        final NewBrowserSessionCommand command;
        final Mock remoteControl;
        final Mock pool;
        final Response expectedResponse;
        final Environment environment;
        final HttpParameters parameters;
        HttpServletRequest theRequest = getMockHttpServletRequest();

        expectedResponse = new Response(0, "OK,1234");
        pool = mock(DynamicRemoteControlPool.class);
        remoteControl = mock(RemoteControlProxy.class);
        environment = new Environment("an environment", "*browser");
        command = new NewBrowserSessionCommand(environment, theRequest);
        remoteControl.expects("forward").with(command.request()).will(returnValue(expectedResponse));
        pool.expects("reserve").with(environment).will(returnValue(remoteControl));
        pool.expects("associateWithSession").with(anything(), eq("1234"));  // TODO with(remoteControl, ...)

        assertEquals(expectedResponse, command.execute((RemoteControlPool) pool));
        verifyMocks();
    }

    @Test
    public void executeReturnsAnErrorResponseWhenSessionCannotBeCreated() throws IOException {
        final NewBrowserSessionCommand command;
        final Mock remoteControl;
        final Environment environment;
        final Mock pool;
        final Response response;
        HttpServletRequest theRequest = getMockHttpServletRequest();

        pool = mock(DynamicRemoteControlPool.class);
        remoteControl = mock(RemoteControlProxy.class);
        environment = new Environment("an environment", "*browser");
        command = new NewBrowserSessionCommand(environment, theRequest);
        pool.expects("reserve").with(environment).will(returnValue(remoteControl));
        remoteControl.expects("forward").with(command.request()).will(returnValue(new Response(500, "")));

        response = command.execute((RemoteControlPool) pool);
        assertEquals(200, response.statusCode());
        assertEquals("ERROR: Could not retrieve a new session", response.body());
        verifyMocks();
    }

    @Test
    public void executeReleaseRemoteControlWhenSessionCannotBeCreated() throws IOException {
        final NewBrowserSessionCommand command;
        final Environment environment;
        final Mock remoteControl;
        final Response response;
        final Mock pool;
        HttpServletRequest theRequest = getMockHttpServletRequest();

        pool = mock(DynamicRemoteControlPool.class);
        remoteControl = mock(RemoteControlProxy.class);
        environment = new Environment("an environment", "*browser");
        command = new NewBrowserSessionCommand(environment, theRequest);
        pool.stubs("reserve").will(returnValue(remoteControl));
        pool.expects("release").with(remoteControl);
        remoteControl.stubs("forward").will(returnValue(new Response(500, "")));

        response = command.execute((RemoteControlPool) pool);
        assertEquals(200, response.statusCode());
        assertEquals("ERROR: Could not retrieve a new session", response.body());
        verifyMocks();
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void executeReturnsAnErrorResponseWhenThereIsANetworkError() throws IOException {
        String helloWorld = "Hello World";
        final NewBrowserSessionCommand command;
        final Environment environment;
        final Mock remoteControl;
        final Response response;
        final Mock pool;
        HttpServletRequest theRequest = getMockHttpServletRequest();

        pool = mock(DynamicRemoteControlPool.class);
        remoteControl = mock(RemoteControlProxy.class);
        environment = new Environment("an environment", "*browser");
        command = new NewBrowserSessionCommand(environment, theRequest);
        remoteControl.expects("forward").with(command.request()).will(throwException(new IOException("an error message")));
        pool.expects("reserve").with(environment).will(returnValue(remoteControl));
        pool.stubs("release").with(remoteControl);

        response = command.execute((RemoteControlPool) pool);
        assertEquals(200, response.statusCode());
        assertEquals("ERROR: an error message", response.body());
        verifyMocks();
    }

    private HttpServletRequest getMockHttpServletRequest() {
        Mock servletRequest = mock(HttpServletRequest.class);
        servletRequest.stubs("getParameterMap").will(returnValue(new HashMap()));
        return (HttpServletRequest) servletRequest;
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void executeReleasesReservedRemoteControlAndReturnsAnErrorResponseWhenThereIsANetworkError() throws IOException {
        final NewBrowserSessionCommand command;
        final Mock remoteControl;
        final Environment environment;
        final Mock pool;
        HttpServletRequest theRequest = getMockHttpServletRequest();

        pool = mock(DynamicRemoteControlPool.class);
        remoteControl = mock(RemoteControlProxy.class);
        environment = new Environment("an environment", "*browser");
        command = new NewBrowserSessionCommand(environment, theRequest);
        remoteControl.stubs("forward").will(throwException(new IOException("an error message")));
        pool.stubs("reserve").will(returnValue(remoteControl));
        pool.expects("release").with(remoteControl);

        command.execute((RemoteControlPool) pool);
        verifyMocks();
    }

    @Test
    public void executeReturnsAnErrorResponseWhenNoRemoteControlCanBeReserved() throws IOException {
        final NewBrowserSessionCommand command;
        final Environment environment;
        final Response response;
        final Mock pool;

        pool = mock(DynamicRemoteControlPool.class);
        environment = new Environment("an environment", "*browser");
        command = new NewBrowserSessionCommand(environment, null);
        pool.expects("reserve").with(environment).will(returnValue(null));

        response = command.execute((RemoteControlPool) pool);
        assertEquals(200, response.statusCode());
        assertEquals("ERROR: No available remote control for environment 'an environment'", response.body());
        verifyMocks();
    }

    @Test
    public void executeCallsUpdateSessionLastActiveAtWithTheSession() throws IOException {
        final NewBrowserSessionCommand command;
        final Mock remoteControl;
        final Mock pool;
        final Environment environment;
        HttpServletRequest theRequest = getMockHttpServletRequest();

        pool = mock(DynamicRemoteControlPool.class);
        remoteControl = mock(RemoteControlProxy.class);
        environment = new Environment("an environment", "*browser");
        command = new NewBrowserSessionCommand(environment, theRequest);
        remoteControl.stubs("forward").will(returnValue(new Response(0, "OK,1234")));
        pool.stubs("reserve").will(returnValue(remoteControl));
        pool.stubs("associateWithSession");
        pool.expects("updateSessionLastActiveAt").with(eq("1234"));

        command.execute((RemoteControlPool) pool);
        verifyMocks();
    }

}
