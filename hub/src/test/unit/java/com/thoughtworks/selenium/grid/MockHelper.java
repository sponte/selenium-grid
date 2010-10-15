package com.thoughtworks.selenium.grid;

import org.jbehave.classmock.UsingClassMock;
import org.jbehave.core.minimock.UsingMiniMock;
import org.jbehave.core.mock.Mock;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;


/**
 * Helper Class for Custom Assertions.
 */
public class MockHelper extends UsingClassMock {

    public static HttpServletRequest GetMockHttpServletRequest() {
        UsingMiniMock miniMock = new UsingMiniMock();
        Mock request = new UsingMiniMock().mock(HttpServletRequest.class);
        request.stubs("getParameterMap").will(miniMock.returnValue(new HashMap<String, String[]>()));
        return (HttpServletRequest) request;
    }

    public static HttpServletRequest GetMockRequestWithParameters(HttpParameters parameters) {
        UsingMiniMock miniMock = new UsingMiniMock();
        Mock request = miniMock.mock(HttpServletRequest.class);
        HashMap params = new HashMap<String, String[]>();
        for (String name : parameters.names()) {
            params.put(name, new String[]{parameters.get(name)});
        }
        request.stubs("getParameterMap").will(miniMock.returnValue(params));
        return (HttpServletRequest) request;
    }

}
