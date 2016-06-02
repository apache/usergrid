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

import java.util.Date;
import java.util.Timer;
import java.util.Vector;

import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.service.ApplicationServiceImpl;
import org.apache.usergrid.apm.service.Device;
import org.apache.usergrid.apm.service.ServiceFactory;


public class MobileToSqsSimulator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//Scanner readUserInput=new Scanner(System.in); 
		Long appId;
		int numMinutes;
		int numSessions;
		App app;

		//while (true)  {
		System.out.println ("##############################");
		System.out.println ("#                            #");
		System.out.println ("#  Mobile to SQS Simulator   #");
		System.out.println ("#      InstaOps Inc           #");
		System.out.println ("#                            #");
		System.out.println ("##############################");

		System.out.println();
		System.out.println();
		System.out.println ("Expects following params in order : appId numSessions uploadFrequencyInMinutes");
		System.out.println ("Number of params " + args.length);

		if (args.length < 3) {
			System.out.println ("Wrong inputs");
			return;
		}
		else
		{
			System.out.println("Going to run simulator with following parameters");
			appId = Long.parseLong(args[0]);
			numSessions = Integer.parseInt(args[1]);
			numMinutes = Integer.parseInt(args[2]);					
			System.out.println("Application ID: " + appId);
			System.out.println("Number of Sessions: " + numSessions);
			System.out.println("Send data interval (min)" + numMinutes);
			app = ServiceFactory.getApplicationService().getApplication(appId);
			if (app != null)
				System.out.println("*** Note: SQS approximately support 500 msg per min, meaning that it can support max about 500 devices per minute ! *** ");
			else
				System.err.println("Could not find app with application id " + appId);
		}		

		//NetworkTestData.populateDataForMinutes2(numMinutes, Calendar.getInstance(), numDevices, appId);

		//resetQueue(appId);

		final int sessionPercentageToExpire = 5;
		final int numIterationToWaitForSessionExpire = 5;
		int numIteration = 0;

		start(app, numSessions, numMinutes,sessionPercentageToExpire,numIterationToWaitForSessionExpire );


		System.out.println ("#####################################");
		System.out.println ("#                                   #");
		System.out.println ("#  Simulator is running with !!     #");
		System.out.println ("#  total sessions "+ numSessions +  "#");
		System.out.println ("#  upload frequency "+ numMinutes +"#");
		System.out.println ("#         ENJOY                     #");
		System.out.println ("#                                   #");
		System.out.println ("#####################################");

	}

	// Deletes queue is necessary
	public static void resetQueue(final String appId)
	{
		ApplicationServiceImpl as = (ApplicationServiceImpl)ServiceFactory.getApplicationService();

		as.resetSQSQueue(appId);
	}

	static Timer timer;

	public static void start(final App app, final int numSessions, final int interval, 
			final int sessionPercentageToExpire, final int numIterationToWaitForSessionExpire )
	{		
		Vector<MobileSession> sessions = new Vector<MobileSession> ();
		Vector<Device> devices = new Vector<Device>();
		GenerateSimulatedMobileData.generateDevices( devices, numSessions*3);
		GenerateSimulatedMobileData.addNewSessions(sessions, devices, numSessions);
		//timer = new Timer();		
		//timer.schedule(new TimerTask() {
		int numSessionsToExpire= Math.max((numSessions * sessionPercentageToExpire)/100,1);
		int numIteration = 0;
		Date startTime = null;
		Date endTime = null;
		long diff = 0;
		int iterationCount = 0;

		//Send a crash data right away
		GenerateSimulatedMobileData.sendCrashData(app,sessions);
		while (true) {


			System.out.println ("Starting iteration " + iterationCount);
			startTime = new Date();
			if (numIteration == numIterationToWaitForSessionExpire) {
				GenerateSimulatedMobileData.expireSessions (sessions, numSessionsToExpire);
				GenerateSimulatedMobileData.addNewSessions(sessions, devices, numSessionsToExpire);
				GenerateSimulatedMobileData.sendCrashData(app,sessions);
				numIteration = 0;
			}
			GenerateSimulatedMobileData.populateSQSWithTestData(numSessions, app, sessions, devices);
			numIteration++;
			endTime = new Date();
			diff = endTime.getTime() - startTime.getTime();
			if (diff < 60000)  { //smaller than 1 minute i.e 60,000 ms
				try {
					System.out.println("Was able to push data in less than 1 minute." +
							"Waiting for " + (60000-diff)/1000 + " seconds before sending next batch to sqs" );
					Thread.sleep(60000-diff);

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			iterationCount++;
			startTime = null;
			endTime = null;
		}

	}
	//, 1000, interval*60000);
	//	}

}
