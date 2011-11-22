/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Stack.
 * 
 * Usergrid Stack is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * Usergrid Stack is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License along
 * with Usergrid Stack. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU AGPL version 3 section 7
 * 
 * Linking Usergrid Stack statically or dynamically with other modules is making
 * a combined work based on Usergrid Stack. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 * 
 * In addition, as a special exception, the copyright holders of Usergrid Stack
 * give you permission to combine Usergrid Stack with free software programs or
 * libraries that are released under the GNU LGPL and with independent modules
 * that communicate with Usergrid Stack solely through:
 * 
 *   - Classes implementing the org.usergrid.services.Service interface
 *   - Apache Shiro Realms and Filters
 *   - Servlet Filters and JAX-RS/Jersey Filters
 * 
 * You may copy and distribute such a system following the terms of the GNU AGPL
 * for Usergrid Stack and the licenses of the other code concerned, provided that
 ******************************************************************************/
package org.usergrid.rest.filters;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.services.ServiceManagerFactory;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ContainerResponseWriter;

@Component
public class MeteringFilter implements ContainerRequestFilter,
		ContainerResponseFilter {

	@Context
	protected HttpServletRequest httpServletRequest;

	EntityManagerFactory emf;
	ServiceManagerFactory smf;
	Properties properties;
	ManagementService management;

	private static final Logger logger = LoggerFactory.getLogger(MeteringFilter.class);

	public MeteringFilter() {
		logger.info("MeteringFilter installed");
	}

	@Autowired
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}

	@Autowired
	public void setServiceManagerFactory(ServiceManagerFactory smf) {
		this.smf = smf;
	}

	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	@Autowired
	public void setManagementService(ManagementService management) {
		this.management = management;
	}

	@Override
	public ContainerRequest filter(ContainerRequest request) {

		try {
			request.setEntityInputStream(new InputStreamAdapter(request
					.getEntityInputStream()));
			httpServletRequest.setAttribute("application.request.timetamp",
					System.currentTimeMillis());
		} catch (Exception e) {
			logger.error("Unable to capture request", e);
		}
		return request;
	}

	public void countDataWritten(long written) {
		try {
			UUID applicationId = (UUID) httpServletRequest
					.getAttribute("applicationId");
			if (applicationId != null) {

				Map<String, Long> counters = new HashMap<String, Long>();

				Long timestamp = (Long) httpServletRequest
						.getAttribute("application.request.timetamp");
				if ((timestamp != null) && (timestamp > 0)) {
					long time = System.currentTimeMillis() - timestamp;
					logger.info("Application: " + applicationId + ", spent "
							+ time + " milliseconds of CPU time");
					counters.put("application.request.time", time);
				}

				Long read = (Long) httpServletRequest
						.getAttribute("application.request.upload");
				if ((read != null) && (read > 0)) {
					logger.info("Application: " + applicationId + ", received "
							+ written + " bytes");
					counters.put("application.request.upload", read);
				}

				if (written > 0) {
					logger.info("Application: " + applicationId + ", sending "
							+ written + " bytes");
					counters.put("application.request.download", written);
				}

				if (emf != null) {
					EntityManager em = emf.getEntityManager(applicationId);
					em.incrementAggregateCounters(null, null, null, counters);
				} else {
					logger.error("No EntityManagerFactory configured");
				}

			}
		} catch (Exception e) {
			logger.error("Unable to capture output", e);
		}
	}

	public void countDataRead(long read) {
		try {
			if (read > 0) {
				httpServletRequest.setAttribute("application.request.upload",
						read);
			}
		} catch (Exception e) {
			logger.error("Unable to capture input", e);
		}
	}

	private final class InputStreamAdapter extends FilterInputStream {

		long total = 0;

		protected InputStreamAdapter(InputStream in) {
			super(in);
		}

		@Override
		public int available() throws IOException {
			int i = super.available();
			return i;
		}

		@Override
		public int read() throws IOException {
			int b = super.read();
			if (b != -1) {
				total++;
			} else {
				countDataRead(total);
				total = 0;
			}
			return b;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int l = super.read(b, off, len);
			if (l != -1) {
				total += l;
			}
			if ((l == -1) || (l < len)) {
				countDataRead(total);
				total = 0;
			}
			return l;
		}

		@Override
		public int read(byte[] b) throws IOException {
			int l = super.read(b);
			if (l != -1) {
				total += l;
			}
			if ((l == -1) || (l < b.length)) {
				countDataRead(total);
				total = 0;
			}
			return l;
		}

		@Override
		public void close() throws IOException {
			super.close();
			countDataRead(total);
		}

	}

	private final class ContainerResponseWriterAdapter implements
			ContainerResponseWriter {

		private final ContainerResponseWriter crw;
		private OutputStreamAdapter out = null;

		ContainerResponseWriterAdapter(ContainerResponseWriter crw) {
			this.crw = crw;
		}

		@Override
		public OutputStream writeStatusAndHeaders(long contentLength,
				ContainerResponse response) throws IOException {

			// logger.info("Wrapping output stream");
			OutputStream o = crw.writeStatusAndHeaders(contentLength, response);

			if (out == null) {
				out = new OutputStreamAdapter(o);
			}

			return out;
		}

		@Override
		public void finish() throws IOException {
			crw.finish();
			if (out != null) {
				countDataWritten(out.getTotal());
			}
		}

		private final class OutputStreamAdapter extends FilterOutputStream {

			long total = 0;

			public OutputStreamAdapter(OutputStream out) {
				super(out);
			}

			public long getTotal() {
				return total;
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				out.write(b, off, len);
				total += len;
			}

			@Override
			public void write(byte[] b) throws IOException {
				out.write(b);
				total += b.length;
			}

			@Override
			public void write(int b) throws IOException {
				out.write(b);
				total += 1;
			}

		}

	}

	@Override
	public ContainerResponse filter(ContainerRequest request,
			ContainerResponse response) {
		try {
			response.setContainerResponseWriter(new ContainerResponseWriterAdapter(
					response.getContainerResponseWriter()));
		} catch (Exception e) {
			logger.error("Unable to capture response", e);
		}
		return response;
	}

}
