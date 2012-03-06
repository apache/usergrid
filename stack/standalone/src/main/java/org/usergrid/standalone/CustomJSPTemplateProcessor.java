/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.standalone;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.container.ContainerException;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.core.reflection.ReflectionHelper;
import com.sun.jersey.server.impl.container.servlet.RequestDispatcherWrapper;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.template.ViewProcessor;

@Provider
public class CustomJSPTemplateProcessor implements ViewProcessor<String> {

	private static final Logger logger = LoggerFactory
			.getLogger(CustomJSPTemplateProcessor.class);

	private @Context
	HttpContext hc;

	private @Context
	ServletContext servletContext;

	private @Context
	ThreadLocal<HttpServletRequest> requestInvoker;

	private @Context
	ThreadLocal<HttpServletResponse> responseInvoker;

	private final String basePath;

	public CustomJSPTemplateProcessor(@Context ResourceConfig resourceConfig) {
		logger.info("CustomJSPTemplateProcessor installed");

		String path = (String) resourceConfig.getProperties().get(
				ServletContainer.JSP_TEMPLATES_BASE_PATH);
		if (path == null) {
			basePath = "";
		} else if (path.charAt(0) == '/') {
			basePath = path;
		} else {
			basePath = "/" + path;
		}
	}

	public String findJsp(String path) throws MalformedURLException {
		if (servletContext.getResource(path) != null) {
			return path;
		} else {
			// check if the entry exists in web.xml through the
			// RequestDispatcher
			ServletContext jspContext = servletContext.getContext(path);
			if (jspContext != null) {
				RequestDispatcher jspReqDispatcher = servletContext
						.getRequestDispatcher(path);
				if (jspReqDispatcher != null) {
					return path;
				}
			}
			RequestDispatcher reqDispatcher = servletContext
					.getRequestDispatcher(path);
			if (reqDispatcher != null) {
				return path;
			}
		}
		return null;
	}

	@Override
	public String resolve(String path) {
		if (servletContext == null) {
			return null;
		}

		if (basePath != "") {
			path = basePath + path;
		}

		try {
			if (findJsp(path) != null) {
				return path;
			}

			if (!path.endsWith(".jsp")) {
				path = path + ".jsp";
				if (findJsp(path) != null) {
					return path;
				}
			}
		} catch (MalformedURLException ex) {
			// TODO log
		}

		return null;
	}

	@Override
	public void writeTo(String resolvedPath, Viewable viewable, OutputStream out)
			throws IOException {
		if (hc.isTracingEnabled()) {
			hc.trace(String.format(
					"forwarding view to JSP page: \"%s\", it = %s",
					resolvedPath,
					ReflectionHelper.objectToString(viewable.getModel())));
		}

		// Commit the status and headers to the HttpServletResponse
		out.flush();

		RequestDispatcher d = servletContext.getRequestDispatcher(resolvedPath);
		if (d == null) {
			throw new ContainerException("No request dispatcher for: "
					+ resolvedPath);
		}

		d = new RequestDispatcherWrapper(d, basePath, hc, viewable);

		try {
			d.forward(requestInvoker.get(), responseInvoker.get());
		} catch (Exception e) {
			throw new ContainerException(e);
		}
	}
}
