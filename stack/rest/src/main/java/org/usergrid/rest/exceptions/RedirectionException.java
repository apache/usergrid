package org.usergrid.rest.exceptions;

import java.net.URI;
import java.net.URISyntaxException;

public class RedirectionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	URI uri = null;

	public RedirectionException(String location) {
		try {
			uri = new URI(location);
		} catch (URISyntaxException e) {
		}
	}

	public URI getUri() {
		return uri;
	}

}
