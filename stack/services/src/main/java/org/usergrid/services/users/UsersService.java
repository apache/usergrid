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
package org.usergrid.services.users;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.persistence.Schema.PROPERTY_EMAIL;
import static org.usergrid.persistence.Schema.PROPERTY_PICTURE;
import static org.usergrid.utils.ConversionUtils.string;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.services.AbstractCollectionService;
import org.usergrid.services.ServiceContext;
import org.usergrid.services.ServiceResults;

public class UsersService extends AbstractCollectionService {

	private static final Logger logger = LoggerFactory.getLogger(UsersService.class);

	public UsersService() {
		super();
		logger.info("/users");

		addHiddenConnection("following");

		addCollections(Arrays.asList("following", "followers"));

		addReplaceParameters(Arrays.asList("followers"),
				Arrays.asList("connecting", "following"));

		addEntityDictionaries(Arrays.asList("rolenames", "permissions"));

	}

	@Override
	public ServiceResults postCollection(ServiceContext context)
			throws Exception {
		Iterator<Map<String, Object>> i = context.getPayload()
				.payloadIterator();
		while (i.hasNext()) {
			Map<String, Object> p = i.next();
			setGravatar(p);
		}
		return super.postCollection(context);
	}

	public void setGravatar(Map<String, Object> p) {
		if (isBlank(string(p.get(PROPERTY_PICTURE)))
				&& isNotBlank(string(p.get("email")))) {
			p.put(PROPERTY_PICTURE,
					"http://www.gravatar.com/avatar/"
							+ md5Hex(string(p.get(PROPERTY_EMAIL)).trim()
									.toLowerCase()));
		}
	}

}
