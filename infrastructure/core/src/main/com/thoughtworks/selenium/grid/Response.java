package com.thoughtworks.selenium.grid;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;


public class Response {

    private final int statusCode;
    private String body;
    private final Header[] headers;

    public Response() {
        this("");
    }

    public Response(String errorMessage) {
        this(200, "ERROR: " + errorMessage, new Header[]{});
    }

    public Response(int statusCode, String body) {
        this(statusCode, body, new Header[]{});
    }

    public Response(HttpMethod method) {
        this.statusCode = method.getStatusCode();
        try {
            this.body = method.getResponseBodyAsString();
        } catch (IOException e) {
            this.body = "";
        }
        this.headers = method.getResponseHeaders();
    }

    public Response(int statusCode, String body, Header[] headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }

    public int statusCode() {
        return statusCode;
    }

    public Header[] headers() {
        return this.headers;
    }

    public Header getHeader(String name) {
        for (Header header : this.headers) {
            if (header.getName().equals(name)) {
                return header;
            }
        }
        return null;
    }

    public String body() {
        return body;
    }

}
