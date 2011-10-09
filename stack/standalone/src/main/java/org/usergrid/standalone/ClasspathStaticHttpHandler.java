package org.usergrid.standalone;

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.io.OutputBuffer;
import org.glassfish.grizzly.http.server.util.MimeType;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.utils.ArraySet;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Modified version of the StaticHttpHandler to serve resources from the
 * classpath.
 * 
 * {@link HttpHandler}, which processes requests to a static resources.
 * 
 * @author Jeanfrancois Arcand
 * @author Alexey Stashok
 */
public class ClasspathStaticHttpHandler extends HttpHandler {
	private static final Logger LOGGER = Grizzly
			.logger(ClasspathStaticHttpHandler.class);

	protected final ArraySet<Resource> docRoots = new ArraySet<Resource>(
			Resource.class);

	/**
	 * Create <tt>HttpHandler</tt>, which, by default, will handle requests to
	 * the static resources located in the current directory.
	 */
	public ClasspathStaticHttpHandler() {
		addDocRoot(".");
	}

	/**
	 * Create a new instance which will look for static pages located under the
	 * <tt>docRoot</tt>. If the <tt>docRoot</tt> is <tt>null</tt> - static pages
	 * won't be served by this <tt>HttpHandler</tt>
	 * 
	 * @param docRoots
	 *            the folder(s) where the static resource are located. If the
	 *            <tt>docRoot</tt> is <tt>null</tt> - static pages won't be
	 *            served by this <tt>HttpHandler</tt>
	 */
	public ClasspathStaticHttpHandler(String... docRoots) {
		if (docRoots != null) {
			for (String docRoot : docRoots) {
				addDocRoot(docRoot);
			}
		}
	}

	/**
	 * Create a new instance which will look for static pages located under the
	 * <tt>docRoot</tt>. If the <tt>docRoot</tt> is <tt>null</tt> - static pages
	 * won't be served by this <tt>HttpHandler</tt>
	 * 
	 * @param docRoots
	 *            the folders where the static resource are located. If the
	 *            <tt>docRoot</tt> is empty - static pages won't be served by
	 *            this <tt>HttpHandler</tt>
	 */
	public ClasspathStaticHttpHandler(Set<String> docRoots) {
		if (docRoots != null) {
			for (String docRoot : docRoots) {
				addDocRoot(docRoot);
			}
		}
	}

	/**
	 * Return the default directory from where files will be serviced.
	 * 
	 * @return the default directory from where file will be serviced.
	 */
	public Resource getDefaultDocRoot() {
		final Resource[] array = docRoots.getArray();
		return ((array != null) && (array.length > 0)) ? array[0] : null;
	}

	/**
	 * Return the list of directories where files will be serviced from.
	 * 
	 * @return the list of directories where files will be serviced from.
	 */
	public ArraySet<Resource> getDocRoots() {
		return docRoots;
	}

	/**
	 * Add the directory to the list of directories where files will be serviced
	 * from.
	 * 
	 * @param docRoot
	 *            the directory to be added to the list of directories where
	 *            files will be serviced from.
	 * 
	 * @return return the {@link File} representation of the passed
	 *         <code>docRoot</code>.
	 */
	public final Resource addDocRoot(String docRoot) {
		if (docRoot == null) {
			throw new NullPointerException("docRoot can't be null");
		}

		final Resource file = new ClassPathResource(docRoot);
		addDocRoot(file);

		return file;
	}

	/**
	 * Add the directory to the list of directories where files will be serviced
	 * from.
	 * 
	 * @param docRoot
	 *            the directory to be added to the list of directories where
	 *            files will be serviced from.
	 */
	public final void addDocRoot(Resource docRoot) {
		docRoots.add(docRoot);
	}

	/**
	 * Removes the directory from the list of directories where static files
	 * will be serviced from.
	 * 
	 * @param docRoot
	 *            the directory to remove.
	 */
	public void removeDocRoot(File docRoot) {
		docRoots.remove(docRoot);
	}

	/**
	 * Based on the {@link Request} URI, try to map the file from the
	 * {@link #getDocRoots()}, and send it back to a client.
	 * 
	 * @param request
	 *            the {@link Request}
	 * @param response
	 *            the {@link Response}
	 * @throws Exception
	 */
	@Override
	public void service(final Request request, final Response response)
			throws Exception {
		final String uri = getRelativeURI(request);

		if ((uri == null) || !handle(uri, request, response)) {
			onMissingResource(request, response);
		}
	}

	protected String getRelativeURI(final Request request) {
		String uri = request.getRequestURI();
		if (uri.indexOf("..") >= 0) {
			return null;
		}

		final String resourcesContextPath = request.getContextPath();
		if (resourcesContextPath.length() > 0) {
			if (!uri.startsWith(resourcesContextPath)) {
				return null;
			}

			uri = uri.substring(resourcesContextPath.length());
		}

		return uri;
	}

	/**
	 * The method will be called, if the static resource requested by the
	 * {@link Request} wasn't found, so {@link StaticHttpHandler} implementation
	 * may try to workaround this situation. The default implementation - sends
	 * a 404 response page by calling
	 * {@link #customizedErrorPage(Request, Response)}.
	 * 
	 * @param request
	 *            the {@link Request}
	 * @param response
	 *            the {@link Response}
	 * @throws Exception
	 */
	protected void onMissingResource(final Request request,
			final Response response) throws Exception {
		response.setStatus(HttpStatus.NOT_FOUND_404);
		customizedErrorPage(request, response);
	}

	/**
	 * Lookup a resource based on the request URI, and send it using send file.
	 * 
	 * @param uri
	 *            The request URI
	 * @param req
	 *            the {@link Request}
	 * @param res
	 *            the {@link Response}
	 * @throws Exception
	 */
	protected boolean handle(final String uri, final Request req,
			final Response res) throws Exception {

		boolean found = false;

		final Resource[] fileFolders = docRoots.getArray();
		if (fileFolders == null) {
			return false;
		}

		Resource resource = null;

		for (int i = 0; i < fileFolders.length; i++) {
			final Resource webDir = fileFolders[i];
			// local file
			resource = webDir.createRelative(uri);
			final boolean exists = resource.exists();

			if (exists) {
				found = true;
				break;
			}
		}

		if (!found) {
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.log(Level.FINE, "File not found  {0}", resource);
			}
			return true;
		}

		sendFile(res, resource);

		return true;
	}

	public static void sendFile(final Response response, final Resource file)
			throws IOException {
		final String path = file.getFilename();
		final InputStream fis = file.getInputStream();

		try {
			response.setStatus(HttpStatus.OK_200);
			String substr;
			int dot = path.lastIndexOf('.');
			if (dot < 0) {
				substr = file.toString();
				dot = substr.lastIndexOf('.');
			} else {
				substr = path;
			}
			if (dot > 0) {
				String ext = substr.substring(dot + 1);
				String ct = MimeType.get(ext);
				if (ct != null) {
					response.setContentType(ct);
				}
			} else {
				response.setContentType(MimeType.get("html"));
			}

			final long length = file.contentLength();
			response.setContentLengthLong(length);

			final OutputBuffer outputBuffer = response.getOutputBuffer();

			byte b[] = new byte[8192];
			int rd;
			while ((rd = fis.read(b)) > 0) {
				// chunk.setBytes(b, 0, rd);
				outputBuffer.write(b, 0, rd);
			}
		} finally {
			try {
				fis.close();
			} catch (IOException ignore) {
			}
		}
	}

}
