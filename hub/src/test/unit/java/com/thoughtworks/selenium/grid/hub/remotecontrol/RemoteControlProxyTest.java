package com.thoughtworks.selenium.grid.hub.remotecontrol;

import com.thoughtworks.selenium.grid.HttpClient;
import com.thoughtworks.selenium.grid.HttpParameters;
import com.thoughtworks.selenium.grid.MockHelper;
import com.thoughtworks.selenium.grid.Response;
import org.apache.commons.httpclient.Header;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static com.thoughtworks.selenium.grid.AssertionHelper.*;
import static junit.framework.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RemoteControlProxyTest {

    @Test(expected = IllegalArgumentException.class)
    public void contructorThrowsIllegalArgumentExceptionWhenServerIsNull() {
        new RemoteControlProxy(null, 5555, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void contructorThrowsIllegalArgumentExceptionWhenEnvironementIsNull() {
        new RemoteControlProxy("localhost", 1234, null, null);
    }

    @Test
    public void hostReturnsTheHostSpecifiedInConstructor() {
        assertEquals("a host", new RemoteControlProxy("a host", 0, "", null).host());
    }

    @Test
    public void portReturnsThePortSpecifiedInConstructor() {
        assertEquals(24, new RemoteControlProxy("", 24, "", null).port());
    }

    @Test
    public void environmentReturnsTheEnvironmentSpecifiedInConstructor() {
        assertEquals("an environment", new RemoteControlProxy("", 0, "an environment", null).environment());

    }

    @Test
    public void sessionInProgressReturnsFalseByDefault() {
        assertFalse(new RemoteControlProxy("a host", 0, "", null).sessionInProgress());
    }

    @Test
    public void sesssionInProgressReturnsTrueAfterCallingRegisterNewSession() {
        final RemoteControlProxy remoteControl;

        remoteControl = new RemoteControlProxy("a host", 0, "", null);
        remoteControl.registerNewSession();
        assertTrue(remoteControl.sessionInProgress());
    }

    @Test(expected = IllegalStateException.class)
    public void registerNewSessionThrowsAnIllegalStateExpressionWhenRegisteringMoreSessionThanAllowedByConcurrentSessionMax() {
        final RemoteControlProxy remoteControl;

        remoteControl = new RemoteControlProxy("a host", 0, "", null);
        remoteControl.registerNewSession();
        remoteControl.registerNewSession();
    }

    @Test
    public void unregisterSessionDecreasesSetSessionInProgressToFalse() {
        final RemoteControlProxy remoteControl;

        remoteControl = new RemoteControlProxy("a host", 0, "", null);
        remoteControl.registerNewSession();
        remoteControl.unregisterSession();
        assertFalse(remoteControl.sessionInProgress());
    }

    @Test(expected = IllegalStateException.class)
    public void unregisterSessionThrowsAnIllegalStateExceptionWhenThereIsNoSessionInProgress() {
        final RemoteControlProxy remoteControl;

        remoteControl = new RemoteControlProxy("a host", 0, "", null);
        remoteControl.unregisterSession();
    }

    @Test
    public void canHandleNewSessionReturnTrueWhenConcurrentSessinCountIsLowerThanConcurrentSessionMax() {
        final RemoteControlProxy remoteControl;

        remoteControl = new RemoteControlProxy("a host", 0, "", null);
        assertTrue(remoteControl.canHandleNewSession());
    }

    @Test
    public void canHandleNewSessionReturnFalseWhenConcurrentSessinCountIsEqualToConcurrentSessionMax() {
        final RemoteControlProxy remoteControl;

        remoteControl = new RemoteControlProxy("a host", 0, "", null);
        remoteControl.registerNewSession();
        assertFalse(remoteControl.canHandleNewSession());
    }

    @Test
    public void remoteControlDriverURLTargetsTheSeleniumDriver() {
        final RemoteControlProxy proxy = new RemoteControlProxy("localhost", 5555, "", null);
        assertEquals("http://localhost:5555/selenium-server/driver/", proxy.remoteControlDriverURL());
    }

    @Test
    public void remoteControlPingURLTargetsTheBlankPage() {
        final RemoteControlProxy proxy = new RemoteControlProxy("localhost", 5555, "", null);
        assertEquals("http://localhost:5555/selenium-server/heartbeat", proxy.remoteControlPingURL());
    }

    @Test
    public void forwardReturnsTheResponseOfTheSeleniumRC() throws IOException {
        final IRemoteControlProxy proxy;
        final Response expectedResponse;
        final HttpParameters parameters;

        HttpClient client = mock(HttpClient.class);
        parameters = new HttpParameters();
        HttpServletRequest request = MockHelper.GetMockRequestWithParameters(parameters);
        expectedResponse = new Response(0, "", new Header[]{});
        when(client.post("http://foo:10/selenium-server/driver/", parameters)).thenReturn(expectedResponse);
        proxy = new RemoteControlProxy("foo", 10, "", (HttpClient) client);
        assertEquals(expectedResponse, proxy.forward(request));
    }

    @Test
    public void toStringMethodReturnsAHumanFriendlyDescriptionWithServerAndPortInformation() {
        assertEquals("[RemoteControlProxy grid.thoughtworks.org:4444#false]",
                new RemoteControlProxy("grid.thoughtworks.org", 4444, "", null).toString());
    }

    @Test
    public void toStringIncludesSessionInProgressInformation() {
        final RemoteControlProxy remoteControl;

        remoteControl = new RemoteControlProxy("grid.org", 4444, "", null);
        remoteControl.registerNewSession();
        assertEquals("[RemoteControlProxy grid.org:4444#true]", remoteControl.toString());
    }

    @Test
    public void aRemoteControlsIsNotEqualToARandomObject() {
        assertNotEquals(new RemoteControlProxy("a.host.com", 24, "", new HttpClient()), "a random object");
    }


    @Test
    public void aRemoteControlsIsNotEqualToItself() {
        final RemoteControlProxy aRemoteControl;

        aRemoteControl = new RemoteControlProxy("a.host.com", 24, "", new HttpClient());
        assertEquals(aRemoteControl, aRemoteControl);
    }

    @Test
    public void twoRemoteControlsAreEqualIfTheirHostAndPortMatch() {
        assertEquals(new RemoteControlProxy("a.host.com", 24, "", new HttpClient()),
                new RemoteControlProxy("a.host.com", 24, "", new HttpClient()));
    }

    @Test
    public void twoRemoteControlsAreNotEqualIfTheirHostsDoNotMatch() {
        final RemoteControlProxy anotherRemoteControl;
        final RemoteControlProxy aRemoteControl;

        aRemoteControl = new RemoteControlProxy("a.host.com", 24, "", new HttpClient());
        anotherRemoteControl = new RemoteControlProxy("another.host.com", 24, "", new HttpClient());
        assertNotEquals(aRemoteControl, anotherRemoteControl);
    }

    @Test
    public void twoRemoteControlsAreNotEqualIfTheirPortsDoNotMatch() {
        final RemoteControlProxy anotherRemoteControl;
        final RemoteControlProxy aRemoteControl;

        aRemoteControl = new RemoteControlProxy("a.host.com", 24, "", new HttpClient());
        anotherRemoteControl = new RemoteControlProxy("a.host.com", 64, "", new HttpClient());
        assertNotEquals(aRemoteControl, anotherRemoteControl);
    }

    @Test
    public void twoRemoteControlsHaveTheSameHashcodeIfTheirHostAndPortMatch() {
        assertSameHashCode(new RemoteControlProxy("a.host.com", 24, "", new HttpClient()),
                new RemoteControlProxy("a.host.com", 24, "", new HttpClient()));
    }

    @Test
    public void twoRemoteControlsDoNotHaveTheSameHashcodelIfTheirHostsDoNotMatch() {
        final RemoteControlProxy anotherRemoteControl;
        final RemoteControlProxy aRemoteControl;

        aRemoteControl = new RemoteControlProxy("a.host.com", 24, "", new HttpClient());
        anotherRemoteControl = new RemoteControlProxy("another.host.com", 24, "", new HttpClient());
        assertDistinctHashCodes(aRemoteControl, anotherRemoteControl);
    }

    @Test
    public void twoRemoteControlsDoNotHaveTheSameHashcodelIfTheirPortsDoNotMatch() {
        final RemoteControlProxy anotherRemoteControl;
        final RemoteControlProxy aRemoteControl;

        aRemoteControl = new RemoteControlProxy("a.host.com", 24, "", new HttpClient());
        anotherRemoteControl = new RemoteControlProxy("a.host.com", 64, "", new HttpClient());
        assertDistinctHashCodes(aRemoteControl, anotherRemoteControl);
    }

    @Test
    public void unreliableReturnsFalseWhenTheResponseIsSuccessful() throws IOException {
        final RemoteControlProxy proxy;
        final Response successfulResponse;

        HttpClient client = mock(HttpClient.class);
        successfulResponse = new Response(200, "", new Header[]{});
        when(client.get("http://foo:10/selenium-server/heartbeat")).thenReturn(successfulResponse);
        proxy = new RemoteControlProxy("foo", 10, "", (HttpClient) client);
        proxy.registerNewSession();
        assertFalse(proxy.unreliable());
    }

    @Test
    public void unreliableReturnsTrueWhenTheResponseIsA500() throws IOException {
        final RemoteControlProxy proxy;
        final Response badResponse;

        HttpClient client = mock(HttpClient.class);
        badResponse = new Response(500, "", new Header[]{});
        when(client.get("http://foo:10/selenium-server/heartbeat")).thenReturn(badResponse);
        proxy = new RemoteControlProxy("foo", 10, "", (HttpClient) client);
        proxy.registerNewSession();
        assertTrue(proxy.unreliable());
    }

    @Test
    public void unreliableReturnsTrueWhenTheRemoteControlCannotBeReached() throws IOException {
        final RemoteControlProxy proxy;

        HttpClient client = mock(HttpClient.class);
        when(client.get("http://foo:10/selenium-server/heartbeat")).thenThrow(new RuntimeException());
        proxy = new RemoteControlProxy("foo", 10, "", (HttpClient) client);
        proxy.registerNewSession();
        assertTrue(proxy.unreliable());
    }

    @Test
    public void unreliableReturnsFalseWhenTheRemoteControlCannotBeReachedAtFirstButRecovers() throws IOException {
        final RemoteControlProxy proxy;
        final Response successfulResponse;

        HttpClient client = mock(HttpClient.class);
        successfulResponse = new Response(200, "", new Header[]{});
        when(client.get("http://foo:10/selenium-server/heartbeat"))
                .thenThrow(new RuntimeException())
                .thenReturn(successfulResponse);
        proxy = new RemoteControlProxy("foo", 10, "", (HttpClient) client);
        proxy.registerNewSession();
        assertFalse(proxy.unreliable());
    }

    @Test
    public void unreliableReturnsFalseWhenTheResponseIsA500ThenA200() throws IOException {
        final RemoteControlProxy proxy;
        final Response badResponse;
        final Response successfulResponse;

        HttpClient client = mock(HttpClient.class);
        badResponse = new Response(500, "", new Header[]{});
        successfulResponse = new Response(200, "", new Header[]{});
        when(client.get("http://foo:10/selenium-server/heartbeat")).thenReturn(badResponse, successfulResponse);
        proxy = new RemoteControlProxy("foo", 10, "", (HttpClient) client);
        proxy.registerNewSession();
        assertFalse(proxy.unreliable());
    }

}
