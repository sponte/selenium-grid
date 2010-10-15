package com.thoughtworks.selenium.grid.hub;

import com.thoughtworks.selenium.grid.HttpParameters;
import com.thoughtworks.selenium.grid.MockHelper;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.IDriverCommand;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.selenium.NewBrowserSessionCommand;
import com.thoughtworks.selenium.grid.hub.remotecontrol.commands.selenium.TestCompleteCommand;
import org.jbehave.classmock.UsingClassMock;
import org.jbehave.core.mock.Mock;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;


public class HttpCommandParserTest extends UsingClassMock {

    @Test
    public void parametersReturnsTheRequestParametersProvidedToTheConstructor() {
        HttpParameters parameters = new HttpParameters();
        HttpServletRequest request = MockHelper.GetMockRequestWithParameters(parameters);
        assertEquals(parameters, new HttpCommandParser(request).parameters());
    }


    @Test
    public void returnsARemoteControlCommandWithHttpRequestQueryStringForAGenericRequest() {
        HttpParameters parameters = new HttpParameters();
        parameters.put("cmd", "generic");
        parameters.put("sessionId", "1234");

        HttpServletRequest request = MockHelper.GetMockRequestWithParameters(parameters);

        IDriverCommand command = new HttpCommandParser(request).parse(null);
        assertEquals("generic", command.parameters().get("cmd"));
        assertEquals("1234", command.parameters().get("sessionId"));
        assertEquals("1234", command.sessionId());
    }

    @Test
    public void returnsARemoteControlCommandWithMatchingHttpParametersForAGenericRequest() {
        HttpParameters parameters = new HttpParameters();
        parameters.put("cmd", "generic");
        parameters.put("sessionId", "1234");
        HttpServletRequest request = MockHelper.GetMockRequestWithParameters(parameters);

        IDriverCommand command = new HttpCommandParser(request).parse(null);
        assertEquals("generic", command.parameters().get("cmd"));
        assertEquals("1234", command.parameters().get("sessionId"));
    }

    @Test
    public void returnsARemoteControlCommandWithMatchingSessionIdForAGenericRequest() {
        HttpParameters parameters = new HttpParameters();
        parameters.put("cmd", "generic");
        parameters.put("sessionId", "1234");
        HttpServletRequest request = MockHelper.GetMockRequestWithParameters(parameters);

        IDriverCommand command = new HttpCommandParser(request).parse(null);
        assertEquals("1234", command.sessionId());
    }

    @Test
    public void returnsTestCompleteCommandForTestCompleteRequests() {
        HttpParameters parameters = new HttpParameters();
        parameters.put("cmd", "testComplete");
        parameters.put("sessionId", "1234");
        HttpServletRequest request = MockHelper.GetMockRequestWithParameters(parameters);

        IDriverCommand command = new HttpCommandParser(request).parse(null);
        assertTrue(command instanceof TestCompleteCommand);
        assertEquals("testComplete", command.parameters().get("cmd"));
        assertEquals("1234", command.parameters().get("sessionId"));
        assertEquals("1234", command.sessionId());
    }


    @SuppressWarnings({"ConstantConditions"})
    @Test
    public void returnsNewBrowserSessionCommandForNewSessionRequests() {
        final NewBrowserSessionCommand browserSessionCommand;
        final Mock environmentManager;
        final Environment expectedEnvironment;
        final HttpParameters parameters;
        final IDriverCommand command;

        parameters = new HttpParameters();
        parameters.put("cmd", "getNewBrowserSession");
        parameters.put("1", "an environment name");
        parameters.put("2", "http://seleniumhq.org");
        expectedEnvironment = new Environment("", "an environment name");
        environmentManager = mock(EnvironmentManager.class);
        environmentManager.expects("environment").with("an environment name").will(returnValue(expectedEnvironment));
        HttpServletRequest request = MockHelper.GetMockRequestWithParameters(parameters);

        command = new HttpCommandParser(request).parse((EnvironmentManager) environmentManager);
        assertEquals(true, command instanceof NewBrowserSessionCommand);
        browserSessionCommand = (NewBrowserSessionCommand) command;

        assertEquals("getNewBrowserSession", browserSessionCommand.parameters().get("cmd"));
        assertEquals("an environment name", browserSessionCommand.parameters().get("1"));
        assertEquals("http://seleniumhq.org", browserSessionCommand.parameters().get("2"));

        assertEquals(expectedEnvironment, browserSessionCommand.environment());

        verifyMocks();
    }


    @SuppressWarnings({"ConstantConditions"})
    @Test
    @Ignore
    public void returnsNewBrowserSessionCommandForNewSessionRequestsAndOverwritesEnvironmentName() {
        final NewBrowserSessionCommand browserSessionCommand;
        final Mock environmentManager;
        final Environment expectedEnvironment;
        final IDriverCommand command;


        Mock parameters = mock(HttpParameters.class);

        HttpServletRequest request = MockHelper.GetMockRequestWithParameters((HttpParameters) parameters);
        parameters.stubs("HttpParameters").with(request).will(returnValue(parameters));
        parameters.stubs("get").with("cmd").will(returnValue("getNewBrowserSession"));
        parameters.stubs("get").with("1").will(returnValue("an environment name"));
        parameters.stubs("get").with("2").will(returnValue("http://seleniumhq.org"));
        HashMap hm = new HashMap<String, String[]>();
        hm.put("cmd", new String[]{"getNewBrowserSession"});
        hm.put("1", new String[]{"an environment name"});
        hm.put("2", new String[]{"http://seleniumhq.org"});
        parameters.stubs("names").will(returnValue(hm.keySet()));

        parameters.expects("put").with("1", "aBrowser");

        expectedEnvironment = new Environment("", "aBrowser");
        environmentManager = mock(EnvironmentManager.class);
        environmentManager.expects("environment").with("an environment name").will(returnValue(expectedEnvironment));

        command = new HttpCommandParser(request).parse((EnvironmentManager) environmentManager);
        assertEquals(true, command instanceof NewBrowserSessionCommand);
        browserSessionCommand = (NewBrowserSessionCommand) command;

        assertEquals("getNewBrowserSession", browserSessionCommand.parameters().get("cmd"));
        assertEquals("an environment name", browserSessionCommand.parameters().get("1"));
        assertEquals("http://seleniumhq.org", browserSessionCommand.parameters().get("2"));

        assertEquals(expectedEnvironment, browserSessionCommand.environment());

        verifyMocks();
    }

    @Test(expected = CommandParsingException.class)
    public void executeThrowsCommandParsingExceptionForNewBrowserSessionWhenEnvironmentIsNotKnown() {
        final Mock environmentManager;
        final HttpParameters parameters;

        parameters = new HttpParameters();
        parameters.put("cmd", "getNewBrowserSession");
        parameters.put("1", "an unknown environment name");
        parameters.put("2", "http://seleniumhq.org");
        environmentManager = mock(EnvironmentManager.class);
        environmentManager.expects("environment").with("an unknown environment name").will(returnValue(null));
        HttpServletRequest request = MockHelper.GetMockRequestWithParameters(parameters);

        new HttpCommandParser(request).parse((EnvironmentManager) environmentManager);
        verifyMocks();
    }

    @Test(expected = CommandParsingException.class)
    public void executeThrowsCommandParsingExceptionForAGenericSeleneseCommandWhenSessionIdIsNull() {
        final Mock environmentManager;
        final HttpParameters parameters;

        parameters = new HttpParameters();
        parameters.put("cmd", "genericCommand");
        environmentManager = mock(EnvironmentManager.class);

        HttpServletRequest request = MockHelper.GetMockRequestWithParameters(parameters);

        new HttpCommandParser(request).parse((EnvironmentManager) environmentManager);
        verifyMocks();
    }

    @Test(expected = CommandParsingException.class)
    public void executeThrowsCommandParsingExceptionForTestCompleteCommandWhenSessionIdIsNull() {
        final Mock environmentManager;
        final HttpParameters parameters;

        parameters = new HttpParameters();
        parameters.put("cmd", "testComplete");
        environmentManager = mock(EnvironmentManager.class);

        HttpServletRequest request = MockHelper.GetMockRequestWithParameters(parameters);

        new HttpCommandParser(request).parse((EnvironmentManager) environmentManager);
        verifyMocks();
    }


}
