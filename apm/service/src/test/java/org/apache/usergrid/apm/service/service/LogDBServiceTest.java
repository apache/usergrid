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
package org.apache.usergrid.apm.service.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.apache.usergrid.apm.service.LogDBService;
import org.apache.usergrid.apm.service.ServiceFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;

import org.apache.usergrid.apm.model.ApigeeMobileAPMConstants;
import org.apache.usergrid.apm.model.ClientLog;
;

public class LogDBServiceTest extends TestCase {

	LogDBService logDBService;
	
	@Override
	protected void setUp() throws Exception {
		logDBService = ServiceFactory.getLogDBService();
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		// TODO Auto-generated method stub
		super.tearDown();
		//ServiceFactory.getHibernateSession().close();
	}
	public void testSaveLog() {
		
		ClientLog logRecord = new ClientLog();
		logRecord.setAppConfigType(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);
		logRecord.setAppId(123123123L);
		logRecord.setTimeStamp(new Date());
		logRecord.setLogLevel(ApigeeMobileAPMConstants.logLevelsString[ApigeeMobileAPMConstants.LOG_DEBUG]);
		logRecord.setLogMessage("HelloWorldMessage");
		
		logDBService.saveLog(logRecord);
		
		//List<ClientLog> logs = logDBService.getLogsForApp(123123123L);
		
	}

	public void testSaveLogs() {
		
		ClientLog logRecord = new ClientLog();
		logRecord.setAppConfigType(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);
		logRecord.setAppId(123123123L);
		logRecord.setTimeStamp(new Date());
		logRecord.setLogLevel("D");
		logRecord.setLogMessage("HelloWorldMessage");
		
		ClientLog logRecord2 = new ClientLog();
		logRecord2.setAppConfigType(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);
		logRecord2.setAppId(123123123L);
		logRecord2.setTimeStamp(new Date());
		logRecord2.setLogLevel("A");
		logRecord2.setLogMessage("Scary HelloWorld Message");
		
		List<ClientLog> logs = new ArrayList<ClientLog>();
		logs.add(logRecord);
		logs.add(logRecord2);
		
		logDBService.saveLogs(logs);
		
		//fail("Not yet implemented");
	}

//	public void testGetLogsForApp() {
//		fail("Not yet implemented");
//	}

	public void testGetLogs() {
		
		Integer startId;
		int numberOfRows;
		List<Criterion> criteria;
		List<Order> orders;
		
		List<ClientLog> clientLogs = logDBService.getLogs(null, 10, null, null);
		
		for (ClientLog logRecord : clientLogs)
		{
			System.out.println(clientLogs.toString());
		}
	}


}
