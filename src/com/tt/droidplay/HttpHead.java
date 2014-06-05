package com.tt.droidplay;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Class that implements the head of a HTTP request.
 * 
 * @author Tuomas Tikka
 */
public class HttpHead {

	// the http method (e.g. GET)
	private String method;
	
	// the uri, incluing query parameters (e.g. /mnt/sdcard/test.mp4)
	private String uri;
	
	// the protocol, including the version (e.g. HTTP/1.1)
	private String protocol;
	
	// the request headers
	private Map<String, String> headers;
	
	/**
	 * Parse the http head object from an input stream.
	 * 
	 * @param in The input stream
	 * @return The http head object
	 */
	public static HttpHead parse(InputStream in) {
		try {
			HttpHead head = new HttpHead();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			int index = 0;
			while (!(line = br.readLine()).isEmpty()) {
				if (index == 0) {
					StringTokenizer st = new StringTokenizer(line, " ");
					if (st.countTokens() == 3) {
						head.method = st.nextToken().trim();
						head.uri = st.nextToken().trim();
						head.protocol = st.nextToken().trim();
					}
				} else {
					StringTokenizer st = new StringTokenizer(line, ":");
					if (st.countTokens() == 2) {
						String name = st.nextToken().trim();
						String value = st.nextToken().trim();
						head.headers.put(name, value);
					}
				}
				index++;
			}
			return (head);
		} catch (Exception e) {
			return (null);
		}
	}
	
	/**
	 * Initialize the http head object.
	 */
	public HttpHead() {
		headers = new HashMap<>();
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
