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
package org.usergrid.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.core.PackagesResourceConfig;

public class CustomResourceConfig extends PackagesResourceConfig {

	/*
	 * private static final Logger logger = Logger
	 * .getLogger(CustomResourceConfig.class.getName());
	 */
	private static final Logger logger = LoggerFactory
			.getLogger(CustomResourceConfig.class);

	public CustomResourceConfig() {
		super();
		logger.info("CustomResourceConfig loaded");
	}

	public CustomResourceConfig(Map<String, Object> props) {
		super(props);
		logger.info("CustomResourceConfig loaded");
	}

	public CustomResourceConfig(String[] paths) {
		super(paths);
		logger.info("CustomResourceConfig loaded");
	}

	/**
	 * 
	 * This method returns the current media type mappings that translate a *
	 * file extension on a URI into a desired {@link MediaType}.
	 * 
	 * @return The mappings from extension to media type.
	 * 
	 * @see com.sun.jersey.api.core.DefaultResourceConfig#getMediaTypeMappings()
	 */

	@Override
	public Map<String, MediaType> getMediaTypeMappings() {
		logger.debug("Setting up mapping");
		Map<String, MediaType> m = new HashMap<String, MediaType>();

		m.put("xml", MediaType.TEXT_XML_TYPE);
		m.put("json", MediaType.APPLICATION_JSON_TYPE);
		m.put("js", new MediaType("application", "javascript"));
		m.put("html", MediaType.TEXT_HTML_TYPE);
		m.put("txt", MediaType.TEXT_PLAIN_TYPE);

		return m;
	}
}
