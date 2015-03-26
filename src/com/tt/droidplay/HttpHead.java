package com.tt.droidplay;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that implements the head of a HTTP request.
 * 
 * @author Tuomas Tikka
 */
public class HttpHead {

	private static final String TAG = "HttpHead";
	
	// the http method (e.g. GET)
	private String method;
	
	// the uri, incluing query parameters (e.g. /mnt/sdcard/test.mp4)
	private String uri;
	
	// the protocol, including the version (e.g. HTTP/1.1)
	private String protocol;
	
	// the request headers
	private Map<String, String> headers;

	/**
	 * Initialize the http head object.
	 */
	public HttpHead() {
		headers = new HashMap<String, String>();
	}

	public void addHeader(String name, String value) {
		headers.put(name, value);
	}

	//
	// Getters and setters
	//
	
	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	
}
