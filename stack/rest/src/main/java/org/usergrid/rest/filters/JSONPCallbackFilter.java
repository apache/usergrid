package org.usergrid.rest.filters;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

@Component
public class JSONPCallbackFilter implements ContainerRequestFilter {

	private static final Logger logger = LoggerFactory
			.getLogger(JSONPCallbackFilter.class);

	@Context
	protected HttpServletRequest httpServletRequest;

	public JSONPCallbackFilter() {
		logger.info("JSONPCallbackFilter is installed");
	}

	@Override
	public ContainerRequest filter(ContainerRequest request) {
		String callback = httpServletRequest.getParameter("callback");
		if (isNotBlank(callback)) {
			request.getRequestHeaders().putSingle("Accept",
					"application/javascript");
		}
		return request;
	}

}
