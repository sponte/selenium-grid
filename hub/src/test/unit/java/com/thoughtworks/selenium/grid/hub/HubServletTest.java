package com.thoughtworks.selenium.grid.hub;

import com.thoughtworks.selenium.grid.HttpParameters;
import com.thoughtworks.selenium.grid.MockHelper;
import com.thoughtworks.selenium.grid.Response;
import com.thoughtworks.selenium.grid.hub.remotecontrol.DynamicRemoteControlPool;
import com.thoughtworks.selenium.grid.hub.remotecontrol.RemoteControlProxy;
import org.apache.commons.httpclient.Header;
import org.jbehave.classmock.UsingClassMock;
import org.jbehave.core.mock.Mock;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;


public class HubServletTest extends UsingClassMock {

    @Test
    public void replySetContentTypeAsPlainText() throws IOException {
        final Response remoteControlResponse;
        final Mock servletResponse;

        remoteControlResponse = new Response(123, "", new Header[]{});
        servletResponse = mock(HttpServletResponse.class);
        servletResponse.expects("setContentType").with("text/plain");
        servletResponse.expects("getWriter").will(returnValue(new PrintWriter(new StringWriter(100))));

        new HubServlet().reply((HttpServletResponse) servletResponse, remoteControlResponse);
        verifyMocks();
    }

    @Test
    public void replySetCharacterEncodingToUTF8() throws IOException {
        final Response remoteControlResponse;
        final Mock servletResponse;

        remoteControlResponse = new Response(123, "", new Header[]{});
        servletResponse = mock(HttpServletResponse.class);
        servletResponse.expects("setCharacterEncoding").with("UTF-8");
        servletResponse.expects("getWriter").will(returnValue(new PrintWriter(new StringWriter(100))));

        new HubServlet().reply((HttpServletResponse) servletResponse, remoteControlResponse);
        verifyMocks();
    }

    @Test
    public void replySetStatusFromRemoteControlOnTheServletResponse() throws IOException {
        final Response remoteControlResponse;
        final Mock servletResponse;

        remoteControlResponse = new Response(123, "", new Header[]{});
        servletResponse = mock(HttpServletResponse.class);
        servletResponse.expects("setStatus").with(123);
        servletResponse.expects("getWriter").will(returnValue(new PrintWriter(new StringWriter(100))));

        new HubServlet().reply((HttpServletResponse) servletResponse, remoteControlResponse);
        verifyMocks();
    }

    @Test
    public void replyWriteRemoteControlResponseOnServletResponseAsPlainText() throws IOException {
        final StringWriter writer = new StringWriter(100);
        final Response remoteControlResponse;
        final Mock servletResponse;

        remoteControlResponse = new Response(0, "some response message", new Header[]{});
        servletResponse = mock(HttpServletResponse.class);
        servletResponse.expects("getWriter").will(returnValue(new PrintWriter(writer)));

        new HubServlet().reply((HttpServletResponse) servletResponse, remoteControlResponse);
        assertEquals("some response message", writer.getBuffer().toString());

        verifyMocks();
    }

    @Test
    public void forwardExecuteTheSeleneseCommandOnTheAppropriateRemoteControl() throws IOException {
        final HttpParameters requestParameters;
        final Mock environmentManager;
        final Mock remoteControl;
        final Response response;
        final HubServlet servlet;
        final Mock pool;

        servlet = new HubServlet();
        requestParameters = new HttpParameters();
        requestParameters.put("cmd", "aSeleneseCommand");
        requestParameters.put("sessionId", "a session id");
        pool = mock(DynamicRemoteControlPool.class);
        remoteControl = mock(RemoteControlProxy.class);
        environmentManager = mock(EnvironmentManager.class);
        response = new Response(0, "", new Header[]{});

        HttpServletRequest request = MockHelper.GetMockRequestWithParameters(requestParameters);

        pool.expects("retrieve").with("a session id").will(returnValue(remoteControl));
        remoteControl.expects("forward").with(request).will(returnValue(response));

        assertEquals(response, servlet.forward((HttpServletRequest) request, (DynamicRemoteControlPool) pool, (EnvironmentManager) environmentManager));
        verifyMocks();
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void forwardReturnAnErrorMessageWhenACommandParsingExceptionIsThrown() throws IOException {
        final HttpParameters requestParameters;
        final Mock environmentManager;
        final Response response;
        final HubServlet servlet;
        final Mock pool;

        servlet = new HubServlet();
        requestParameters = new HttpParameters();
        requestParameters.put("cmd", "aSeleneseCommand");
        requestParameters.put("sessionId", "a session id");
        pool = mock(DynamicRemoteControlPool.class);
        environmentManager = mock(EnvironmentManager.class);

        HttpServletRequest request = MockHelper.GetMockRequestWithParameters(requestParameters);

        pool.expects("retrieve").with("a session id").will(throwException(new CommandParsingException("an error message")));

        response = servlet.forward((HttpServletRequest) request, (DynamicRemoteControlPool) pool, (EnvironmentManager) environmentManager);
        assertEquals(200, response.statusCode());
        assertEquals("ERROR: an error message", response.body());
        verifyMocks();
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void forwardReturnAnErrorMessageWhenANoSuchEnvironmentExceptionIsThrown() throws IOException {
        final HttpParameters requestParameters;
        final Mock environmentManager;
        final Response response;
        final HubServlet servlet;
        final Mock pool;

        servlet = new HubServlet();
        requestParameters = new HttpParameters();
        requestParameters.put("cmd", "aSeleneseCommand");
        requestParameters.put("sessionId", "a session id");
        pool = mock(DynamicRemoteControlPool.class);
        environmentManager = mock(EnvironmentManager.class);

        HttpServletRequest request = MockHelper.GetMockRequestWithParameters(requestParameters);

        pool.expects("retrieve").with("a session id").will(throwException(new NoSuchEnvironmentException("an environment")));

        response = servlet.forward((HttpServletRequest) request, (DynamicRemoteControlPool) pool, (EnvironmentManager) environmentManager);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().startsWith(
                "ERROR: Could not find any remote control providing the 'an environment' environment"));
        verifyMocks();
    }

    @Test
    public void requestParametersReturnsAdaptedRequestParameters() {
        final HttpParameters parameters;
        final Map<String, String[]> parameterMap;
        final HubServlet servlet;
        final Mock request;

        parameterMap = new HashMap<String, String[]>();
        parameterMap.put("a name", new String[]{"a value"});
        request = mock(HttpServletRequest.class);
        request.expects("getParameterMap").will(returnValue(parameterMap));
        servlet = new HubServlet();
        parameters = servlet.requestParameters((HttpServletRequest) request);
        assertEquals("a value", parameters.get("a name"));
    }


}
