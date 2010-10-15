package com.thoughtworks.selenium.grid;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;


/**
 * Invoke HTTP GET requests and gather status code and text body for the response.
 * <br/>
 * Implementation is simplistic but should cover Selenium RC limited vocabulary.
 */
public class HttpClient {

    private static final Log logger = LogFactory.getLog(HttpClient.class);
    private final org.apache.commons.httpclient.HttpClient client;

    public HttpClient(org.apache.commons.httpclient.HttpClient client) {
        this.client = client;
    }

    public HttpClient() {
        this(new org.apache.commons.httpclient.HttpClient(new org.apache.commons.httpclient.MultiThreadedHttpConnectionManager()));
    }

    public Response get(String url) throws IOException {
        return request(new GetMethod(url));
    }

    public Response post(String url, HttpParameters parameters) throws IOException {
        return request(buildPostMethod(url, parameters));
    }

    protected PostMethod buildPostMethod(String url, HttpParameters parameters) {
        final PostMethod postMethod;

        postMethod = new PostMethod(url);
        postMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; ; charset=UTF-8");
        for (String name : parameters.names()) {
            postMethod.setParameter(name, parameters.get(name));
        }
        return postMethod;
    }

    protected Response request(HttpMethod method) throws IOException {
        int statusCode;
        String body;

        try {
            statusCode = client.executeMethod(method);
            if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                method.releaseConnection();

                HttpMethod postMethod = redirectMethod((PostMethod) method);
                statusCode = client.executeMethod(postMethod);
                method = postMethod;
            }
            if (statusCode == HttpStatus.SC_NO_CONTENT) {
                body = "";
            } else {
                body = new String(method.getResponseBody(), "utf-8");
            }
            logger.debug("Remote Control replied with '" + statusCode + " / '" + body + "'");
            return new Response(statusCode, body, method.getResponseHeaders());
        } finally {
            method.releaseConnection();
        }
    }

    private HttpMethod redirectMethod(PostMethod postMethod) {
        GetMethod method = new GetMethod(postMethod.getResponseHeader("Location").getValue());
        for (Header header : postMethod.getRequestHeaders()) {
            method.addRequestHeader(header);
        }
        return method;
    }

    public Response postWebDriverRequest(String url, HttpServletRequest request) throws IOException {
        return request(convertHttpServletRequestToPostMethod(url, request));
    }


    public Response deleteWebDriverRequest(String url, HttpServletRequest request) throws IOException {
        return request(convertHttpServletRequestToDeleteMethod(url, request));
    }

    public Response putWebDriverRequest(String url, HttpServletRequest request) throws IOException {
        return request(convertHttpServletRequestToPutMethod(url, request));
    }

    public Response getWebDriverRequest(String url, HttpServletRequest request) throws IOException {
        return request(convertHttpServletRequestToGetMethod(url, request));
    }

    private PostMethod convertHttpServletRequestToPostMethod(String url, HttpServletRequest request) {
        PostMethod postMethod = new PostMethod(url);

        for (Enumeration headers = request.getHeaderNames(); headers.hasMoreElements();) {
            String headerName = (String) headers.nextElement();
            String headerValue = (String) request.getHeader(headerName);
            postMethod.addRequestHeader(headerName, headerValue);
        }

        postMethod.removeRequestHeader("Host");
        postMethod.addRequestHeader("Host", request.getRequestURL().toString());

        for (Enumeration names = request.getParameterNames(); names.hasMoreElements();) {
            String paramName = (String) names.nextElement();
            String paramValue = (String) request.getParameter(paramName);
            postMethod.addParameter(paramName, paramValue);
        }

        StringBuilder requestBody = new StringBuilder();
        try {
            BufferedReader reader = request.getReader();
            String line;
            while (null != (line = reader.readLine())) {
                requestBody.append(line);
            }
            reader.close();
        } catch (IOException e) {
            requestBody.append("");
        }

        postMethod.setRequestEntity(new StringRequestEntity(requestBody.toString()));

        return postMethod;
    }

    private GetMethod convertHttpServletRequestToGetMethod(String url, HttpServletRequest request) {
        GetMethod method = new GetMethod(url);

        for (Enumeration headers = request.getHeaderNames(); headers.hasMoreElements();) {
            String headerName = (String) headers.nextElement();
            String headerValue = (String) request.getHeader(headerName);
            method.addRequestHeader(headerName, headerValue);
        }

        method.removeRequestHeader("Host");
        method.addRequestHeader("Host", request.getRequestURL().toString());

        return method;
    }


    private DeleteMethod convertHttpServletRequestToDeleteMethod(String url, HttpServletRequest request) {
        DeleteMethod method = new DeleteMethod(url);

        for (Enumeration headers = request.getHeaderNames(); headers.hasMoreElements();) {
            String headerName = (String) headers.nextElement();
            String headerValue = (String) request.getHeader(headerName);
            method.addRequestHeader(headerName, headerValue);
        }

        method.removeRequestHeader("Host");
        method.addRequestHeader("Host", request.getRequestURL().toString());

        return method;
    }


    private PutMethod convertHttpServletRequestToPutMethod(String url, HttpServletRequest request) {
        PutMethod method = new PutMethod(url);

        for (Enumeration headers = request.getHeaderNames(); headers.hasMoreElements();) {
            String headerName = (String) headers.nextElement();
            String headerValue = (String) request.getHeader(headerName);
            method.addRequestHeader(headerName, headerValue);
        }

        method.removeRequestHeader("Host");
        method.addRequestHeader("Host", request.getRequestURL().toString());

        StringBuilder requestBody = new StringBuilder();
        try {
            BufferedReader reader = request.getReader();
            String line;
            while (null != (line = reader.readLine())) {
                requestBody.append(line);
            }
            reader.close();
        } catch (IOException e) {
            requestBody.append("");
        }

        method.setRequestEntity(new StringRequestEntity(requestBody.toString()));

        return method;
    }

}
