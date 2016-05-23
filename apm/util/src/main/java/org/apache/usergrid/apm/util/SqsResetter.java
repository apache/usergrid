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
