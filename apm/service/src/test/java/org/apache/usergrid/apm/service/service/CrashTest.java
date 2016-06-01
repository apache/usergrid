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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.IOUtils;

import org.apache.usergrid.apm.model.ClientLog;
import org.apache.usergrid.apm.service.CrashUtil;
import org.apache.usergrid.apm.service.ServiceFactory;
import org.apache.usergrid.apm.service.crashlogparser.AndroidCrashLogParser;
import org.apache.usergrid.apm.service.crashlogparser.CrashLogParser;
import org.apache.usergrid.apm.service.crashlogparser.iOSCrashLogParser;
import org.apache.usergrid.apm.model.CrashLogDetails;

import junit.framework.TestCase;

public class CrashTest extends TestCase{
	
	public void testiOSCrashParsing () throws IOException {
		CrashLogParser parser = new iOSCrashLogParser();
		InputStreamReader is = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("ios.crash"));
		String fileContent = IOUtils.toString(is);
		parser.parseCrashLog(fileContent);		
		assertTrue("Parsed iOS crash properly" , parser.getCrashSummary() != null && parser.getCrashSummary().contains("Exception Type"));
		System.out.println ("Crash summary : " + parser.getCrashSummary());
	}
	
	
	
	public void testAndroidCrashParsing () throws IOException {
		CrashLogParser parser = new AndroidCrashLogParser();
		InputStreamReader is = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("android.stacktrace"));
		String fileContent = IOUtils.toString(is);
		parser.parseCrashLog(fileContent);		
		assertTrue ("Parsed Android crash properly" , parser.getCrashSummary() != null && parser.getCrashSummary().contains("NullPointerException"));
		System.out.println ("Crash summary : " + parser.getCrashSummary());
	}
	
	public void testAndroidCrashParsingWithNoCausedByInLog () throws IOException {
		CrashLogParser parser = new AndroidCrashLogParser();
		InputStreamReader is = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("android-no-causedBy.stacktrace"));
		String fileContent = IOUtils.toString(is);
		parser.parseCrashLog(fileContent);		
		assertTrue("Parsed Android crash properly" , parser.getCrashSummary() != null && parser.getCrashSummary().contains("NullPointerException"));
		System.out.println ("Crash summary : " + parser.getCrashSummary());
	}
	
	public void testCrashDownloadAndParsing () {
		String summary = CrashUtil.getCrashSummary("Demo_AcmeBank", "11b86c18-e302-4b26-bdf7-5c059387608e.crash");
		assert (summary != null && summary.length() != 0);
		System.out.println ("Crash summary : " + summary);
	}
	
	public void testCrashDownloadAndSaving () {
		ClientLog cl = new ClientLog();
		cl.setAppId(1L);
		cl.setFullAppName("Demo_AcmeBank");
		cl.setTimeStamp(Calendar.getInstance().getTime());
		//TODO: Upload a new crash file and update file name accordingly
		cl.setLogMessage("128ddf31-7eab-4042-8d42-b9ec40998729.stacktrace");
		cl.setTag("CRASH");
		List<ClientLog> list = new ArrayList<ClientLog>();
		list.add(cl);
		
		ServiceFactory.getAlarmService().parseAndPersistCrashLogs(list);
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR_OF_DAY, -1);
		List<CrashLogDetails> list1 = ServiceFactory.getCrashLogDBService().getCrashLogs(1L, cal.getTime());
		assert (list.size() != 0);
		System.out.println ("crash summary " + list1.get(0).getCrashSummary());
		assert(list1.get(0).getCrashSummary() != null);
	}
	
	
	

}
