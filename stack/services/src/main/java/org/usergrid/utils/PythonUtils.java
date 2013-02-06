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
package org.usergrid.utils;

import static org.usergrid.utils.StringUtils.compactWhitespace;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
