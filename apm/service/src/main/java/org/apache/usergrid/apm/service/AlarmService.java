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
package org.apache.usergrid.apm.service;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.model.ClientLog;
import org.apache.usergrid.apm.service.util.AsyncMailer;
import org.apache.usergrid.apm.service.util.Email;
import org.apache.usergrid.apm.model.CrashLogDetails;

public class AlarmService {


	private ApplicationService applicationService;

	SimpleDateFormat formatter=new SimpleDateFormat("dd-MMM-yyyy HH:MM");

	private static final Log log = LogFactory.getLog(AlarmService.class);

	public String formatCriticalErrors(List<ClientLog> logs)
	{

		StringBuffer message = new StringBuffer();

		message.append(logs.size() + " critical errors were detected at " + formatter.format(new Date())+ " <br/>");

		message.append("Errors : <br/>");

		for(ClientLog log : logs)
		{
			message.append(log.toString() +"<br/>");
		}

		return message.toString();

	}


	public ApplicationService getApplicationService() {
		return applicationService;
	}


	public void setApplicationService(ApplicationService applicationService) {
		this.applicationService = applicationService;
	}

	public void sendCriticalErrors(List<ClientLog> logs, Long appId)
	{

		App app = applicationService.getApplication(appId);

		String appName = app.getAppName();

		String subject = "Apigee App Monitoring Alerts for app: " + appName;
		String body = formatCriticalErrors(logs);
		String email = app.getAppOwner();

		Email alarmEmail = new Email(subject, body,email);		

		if(!suppressAlarm(app))
		{
			log.info("Sending critical error alarms for app : " + appId);
			AsyncMailer.send(alarmEmail);
		} else
		{
			log.info("Supressing critical error alarms for app : " + appId);
		}

		//parse the crash logs and persist them. 
		//TODO: See if this should happen in Asynch fashion
		parseAndPersistCrashLogs(logs);
	}

	public void parseAndPersistCrashLogs(List<ClientLog> logs) {
		List<CrashLogDetails> crashes = new ArrayList<CrashLogDetails>();
		for (ClientLog l : logs) {
			if ("CRASH".equals(l.getTag())) {
				CrashLogDetails c = getCrashLogFromClientLog(l);
				crashes.add(c);
			}				
		}
		ServiceFactory.getCrashLogDBService().saveCrashLogs(crashes);
	}

	public CrashLogDetails getCrashLogFromClientLog(ClientLog l ) {
		CrashLogDetails cld = new CrashLogDetails();
		cld.setAppId(l.getAppId());
		cld.setFullAppName(l.getFullAppName());
		cld.setDeviceId(l.getDeviceId());
		cld.setDeviceModel(l.getDeviceModel());
		cld.setDeviceOperatingSystem(l.getDeviceOperatingSystem());
		cld.setDevicePlatform(l.getDevicePlatform());
		cld.setDeviceType(l.getDeviceType());
		cld.setNetworkCarrier(l.getNetworkCarrier());
		cld.setNetworkType(l.getNetworkType());
		cld.setNetworkCountry(l.getNetworkCountry());
		cld.setLatitude(l.getLatitude());
		cld.setLongitude(l.getLongitude());
		cld.setApplicationVersion(l.getApplicationVersion());
		cld.setAppConfigType(l.getAppConfigType());
		cld.setDeviceId(l.getDeviceId());
		cld.setTimeStamp(l.getTimeStamp());
		//logMessage is basically crash file name on S3 for crash detected on native SDKs
		String message = l.getLogMessage();		
		log.info("logMessage in crash log related client log is " + message);
		if (message != null && (message.endsWith(".stacktrace") || message.endsWith(".crash"))) {
			cld.setCrashFileName(message);
			cld.setCrashSummary(CrashUtil.getCrashSummary(cld.getFullAppName(),message));			
		}
		else
			cld.setCrashSummary(message);
		return cld;
	}



	public boolean suppressAlarm(App app)
	{	
		
		if (app.getFullAppName().equalsIgnoreCase(ApplicationService.DEMO_APP_FULL_NAME))
			return true;		
		return UsergridInternalRestServerConnector.isCrashNotificationDisabled(app.getOrgName(),app.getAppName());		

	}
	



}
