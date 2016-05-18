package org.apache.usergrid.apm.service;


public interface SchedulerService {
	
	//Need to think of all methods that need to be done here such as
	//configuring new set of queue names 
	//configuring new trigger, timer and new job that need to be executed through UI
	
	public void start ();
	
	public void stop ();

}
