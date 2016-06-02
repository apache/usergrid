/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.usergrid.apm.util;

import java.net.URI;

public class DomainParsing {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String domain = null;
		String webAddress = null;
		try {
			webAddress = "http://www.cfdasfsddfasdfdafdasfaafsnn.com?xyx=bah";
			if (webAddress.indexOf("?") != -1)
				webAddress = webAddress.substring(0,webAddress.indexOf('?'));
			domain = new URI(webAddress).getHost();
			System.out.println(domain);
		} catch (Exception e) {
			domain = webAddress.substring(0, Math.min(20,webAddress.length()));
		}

	}

}
