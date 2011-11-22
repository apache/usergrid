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
package org.usergrid.utils;

import static org.usergrid.utils.StringUtils.compactWhitespace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

public class PythonUtils {

	private static final Logger logger = LoggerFactory.getLogger(PythonUtils.class);

	public static PyObject getPyClass(String moduleName, String clsName) {
		PyObject pyObject = null;
		PythonInterpreter interpreter = new PythonInterpreter();

		try {
			interpreter.exec("from " + moduleName + " import " + clsName);
			pyObject = interpreter.get(clsName);

		} catch (Exception e) {
			logger.error("The Python module '" + moduleName
					+ "' is not found: " + compactWhitespace(e.toString()));
		}
		return pyObject;
	}

	@SuppressWarnings("unchecked")
	public static <T> T createObject(Class<T> interfaceType, PyObject pyClass) {

		Object javaObj = null;

		PyObject newObj = pyClass.__call__();

		javaObj = newObj.__tojava__(interfaceType);

		return (T) javaObj;
	}

	public static Object createObject(Object interfaceType, String moduleName,
			String clsName) {

		PyObject pyObject = getPyClass(moduleName, clsName);

		Object javaObj = null;
		try {

			PyObject newObj = pyObject.__call__();

			javaObj = newObj.__tojava__(Class.forName(interfaceType.toString()
					.substring(interfaceType.toString().indexOf(" ") + 1,
							interfaceType.toString().length())));
		} catch (Exception ex) {
			logger.error("Unable to create Python object: "
					+ compactWhitespace(ex.toString()));
		}

		return javaObj;
	}

	public static String getModuleName(String s) {
		if (s == null) {
			return null;
		}
		int i = s.lastIndexOf('.');
		if (i < 0) {
			return s;
		}
		return s.substring(0, i);
	}

	public static String getClassName(String s) {
		if (s == null) {
			return null;
		}
		int i = s.lastIndexOf('.');
		if (i < 0) {
			return s;
		}
		return s.substring(i + 1, s.length());
	}
}
