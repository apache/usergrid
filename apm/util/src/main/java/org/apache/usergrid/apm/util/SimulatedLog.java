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

import java.util.Random;

import org.apache.usergrid.apm.model.ClientLog;

public class SimulatedLog {
	public int logLevel;
	public String logMessage;
	public String tag;


	static Random generator = new Random();

	public static String[] assertLogMessages = {
		"User query status successful with no data. This should not be happening",
		"Could not resize images for portrait view"
	};

	public static String[] errorLogMessages = {
		"Encountered an error: Error Domain=org.restkit.RestKit.ErrorDomain Code=1004 \"invalid_grant\" UserInf" ,
		"RMSynchronizationManagerV1 refreshEntity:mergeFilter:webService:force:completionBlock:> Refr",
		"AmazonAbstractWebServiceClient.m|-[AmazonAbstractWebServiceClient invoke:rawRequest:unmarshallerDele", 
		"Error 404 while retrieving bitmap from http://gravatar.com/blavatar/de70836ffeb84948d6253160956ba141?s=60&d=404", 
		"locationManager:didFailWithError: Location manager failed with error: The operation couldnot be completed"
	};

	public static String[] warningLogMessages = {		
		"Connecting with no cache turned on",
		"Crash detection on orientation change enabled",
		"Response did not conform with message schema",
		"First authentication failed because of user error",
		"Not enough data for pagination to work.",
		"Pausing for 5 seconds before prompting user ggain"
	};

	public static String[] infoLogMessages = {		
		"Conditional caching applied",
		"Setting number of items to display to 10 as default",
		"Purchase UI view has 1 item in cart"		
	};

	public static String[] debugLogMessages = {		
		"Starting with cached data since no network connection is there",
		"DEBUG: defaulting to landscape view",
		"Orientation change detection is disabled",
		"Starting with 3 columan layout with 5 rows "
	};

	public SimulatedLog () {

	}

	public SimulatedLog (int logLevel, String logMessage) {
		this.logLevel = logLevel;
		this.logMessage = logMessage;			
	}

	public static SimulatedLog getSimulatedLog (int level) {
		SimulatedLog sl = new SimulatedLog();
		sl.logLevel = level;
		if (level == ClientLog.ASSERT) {
			sl.logMessage = assertLogMessages[generator.nextInt(assertLogMessages.length)];
			sl.tag="SYSTEM";
		}
		else if (level == ClientLog.ERROR)  {
			sl.logMessage = errorLogMessages[generator.nextInt(errorLogMessages.length)];
			sl.tag = "CORE";
		}
		else if (level == ClientLog.WARN)  {
			sl.logMessage = warningLogMessages[generator.nextInt(warningLogMessages.length)];
			sl.tag = "UI";
		}
		else if (level == ClientLog.INFO) {
			sl.logMessage = infoLogMessages[generator.nextInt(infoLogMessages.length)];
			sl.tag = "NON-UI";
		}
		else if (level == ClientLog.DEBUG) {
			sl.logMessage = debugLogMessages[generator.nextInt(debugLogMessages.length)];
			sl.tag="GENERIC";
		}
		return sl;
	}



}
