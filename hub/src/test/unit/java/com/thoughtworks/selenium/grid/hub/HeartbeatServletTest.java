package com.thoughtworks.selenium.grid.hub;

import com.thoughtworks.selenium.grid.hub.remotecontrol.DynamicRemoteControlPool;
import com.thoughtworks.selenium.grid.hub.remotecontrol.RemoteControlProxy;
import org.jbehave.classmock.UsingClassMock;
import org.jbehave.core.mock.Mock;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static junit.framework.Assert.assertEquals;


public class HeartbeatServletTest extends UsingClassMock {

    @Test
    public void replySetContentTypeAsPlainText() throws IOException {
        final Mock servletResponse;

        servletResponse = mock(HttpServletResponse.class);
        servletResponse.expects("setContentType").with("text/plain");
        servletResponse.expects("getWriter").will(returnValue(new PrintWriter(new StringWriter(100))));

        new HeartbeatServlet().reply("a host", "1234", (HttpServletResponse) servletResponse);
        verifyMocks();
    }

    @Test
    public void replyWritesNotRegisteredOnServletResponseWhenHubIsNotRegistered() throws IOException {
        final StringWriter writer = new StringWriter(100);
        final Mock servletResponse;

        servletResponse = mock(HttpServletResponse.class);
        servletResponse.expects("getWriter").will(returnValue(new PrintWriter(writer)));

        new HeartbeatServlet().reply("a host", "1234", (HttpServletResponse) servletResponse);
        assertEquals("Hub : Not Registered", writer.getBuffer().toString());

        verifyMocks();
    }

    @Test
    public void replyWritesNotRegisteredOnServletResponseWhenPortIsNotANumber() throws IOException {
        final StringWriter writer = new StringWriter(100);
        final Mock servletResponse;

        servletResponse = mock(HttpServletResponse.class);
        servletResponse.expects("getWriter").will(returnValue(new PrintWriter(writer)));

        new HeartbeatServlet().reply("a host", "Not a Number", (HttpServletResponse) servletResponse);
        assertEquals("Hub : Not Registered", writer.getBuffer().toString());

        verifyMocks();
    }

    @Test
    public void replyWritesOKOnServletResponseWhenHubIsRegistered() throws IOException {
        final StringWriter writer = new StringWriter(100);
        final Mock servletResponse;
        final Mock pool;
        servletResponse = mock(HttpServletResponse.class);
        servletResponse.expects("getWriter").will(returnValue(new PrintWriter(writer)));
        pool = mock(DynamicRemoteControlPool.class);
        pool.expects("isRegistered").with(eq(new RemoteControlProxy("a host", 1234, "", null))).will(returnValue(true));

        new HeartbeatServlet() {
            @Override
            protected DynamicRemoteControlPool remoteControlPool() {
                return (DynamicRemoteControlPool) pool;
            }
        }.reply("a host", "1234", (HttpServletResponse) servletResponse);
        assertEquals("Hub : OK", writer.getBuffer().toString());

        verifyMocks();
    }

}
