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
package org.usergrid.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.apache.commons.beanutils.MethodUtils;
import org.springframework.util.StringUtils;

public class Command {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if ((args == null) || (args.length < 1)) {
			System.out.println("No command specified");
			return;
		}

		String command = args[0];

		Class<?> clazz = null;

		try {
			clazz = Class.forName(command);
		} catch (ClassNotFoundException e) {
		}

		if (clazz == null) {
			try {
				clazz = Class.forName("org.usergrid.tools." + command);
			} catch (ClassNotFoundException e) {
			}
		}

		if (clazz == null) {
			try {
				clazz = Class.forName("org.usergrid.tools."
						+ StringUtils.capitalize(command));
			} catch (ClassNotFoundException e) {
			}
		}

		if (clazz == null) {
			System.out.println("Unable to find command");
			return;
		}

		args = Arrays.copyOfRange(args, 1, args.length);

		try {
			if (ToolBase.class.isAssignableFrom(clazz)) {
				ToolBase tool = (ToolBase) clazz.newInstance();
				tool.startTool(args);
			} else {
				MethodUtils.invokeStaticMethod(clazz, "main", (Object) args);
			}
		} catch (NoSuchMethodException e) {
			System.out.println("Unable to invoke command");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.out.println("Unable to invoke command");
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			System.out.println("Error while invoking command");
			e.printStackTrace();
		} catch (InstantiationException e) {
			System.out.println("Error while instantiating tool object");
			e.printStackTrace();
		}
	}

}
