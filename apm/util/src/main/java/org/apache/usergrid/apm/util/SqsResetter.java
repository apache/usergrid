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

import org.apache.usergrid.apm.service.ApplicationServiceImpl;
import org.apache.usergrid.apm.service.ServiceFactory;


public class SqsResetter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//Scanner readUserInput=new Scanner(System.in); 
		Long appId;


		//while (true)  {
		System.out.println ("##############################");
		System.out.println ("#                            #");
		System.out.println ("#  SQS Queue Resetter        #");
		System.out.println ("#      opsFuse Inc           #");
		System.out.println ("#                            #");
		System.out.println ("##############################");

		System.out.println();
		System.out.println();
		System.out.println ("Expects following params in order : appId");
		System.out.println ("Number of params " + args.length);

		if (args.length < 1) {
			System.out.println ("Wrong inputs");
			return;
		}
		else
		{
			System.out.println("Resetting queue with following parameters");
			appId = Long.parseLong(args[0]);
			System.out.println("Application ID: " + appId);
		}		

		//NetworkTestData.populateDataForMinutes2(numMinutes, Calendar.getInstance(), numDevices, appId);

		resetQueue(appId.toString());
			
		System.out.println ("##############################");
		System.out.println ("#                            #");
		System.out.println ("#   Finished Resetting       #");
		System.out.println ("#         		              #");
		System.out.println ("#                            #");
		System.out.println ("##############################");
		
	}
	
	// Deletes queue is necessary
	public static void resetQueue(final String appId)
	{
		ApplicationServiceImpl as = (ApplicationServiceImpl)ServiceFactory.getApplicationService();
		
		as.resetSQSQueue(appId);
	}
	


}
