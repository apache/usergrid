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
        e.printStackTrace();
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
