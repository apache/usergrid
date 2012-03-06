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
package org.usergrid.management.utils;

import static org.usergrid.persistence.Schema.PROPERTY_ACTIVATED;
import static org.usergrid.persistence.Schema.PROPERTY_DISABLED;
import static org.usergrid.utils.ConversionUtils.getBoolean;

import org.usergrid.persistence.Entity;

public class AccountUtils {

	public static boolean isUserActivated(Entity user) {
		if (user == null) {
			return false;
		}
		return getBoolean(user.getProperty(PROPERTY_ACTIVATED));
	}

	public static boolean isUserDisabled(Entity user) {
		if (user == null) {
			return false;
		}
		return getBoolean(user.getProperty(PROPERTY_DISABLED));
	}

}
