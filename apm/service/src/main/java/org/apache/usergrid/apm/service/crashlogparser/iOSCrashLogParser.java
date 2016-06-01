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
package org.apache.usergrid.apm.service.crashlogparser;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * 
 * @author Paul Dardeau
 *
 */

public class iOSCrashLogParser extends AbstractCrashLogParser implements CrashLogParser {
	
	private static final Log log = LogFactory.getLog(iOSCrashLogParser.class);
	
	public static final String DELIM = ":";

	public static final String KEY_HARDWARE_MODEL   = "Hardware Model";
	public static final String KEY_PROCESS          = "Process";
	public static final String KEY_PATH             = "Path";
	public static final String KEY_IDENTIFIER       = "Identifier";
	public static final String KEY_VERSION          = "Version";
	public static final String KEY_CODE_TYPE        = "Code Type";
	public static final String KEY_PARENT_PROCESS   = "Parent Process";
	public static final String KEY_DATE_TIME        = "Date/Time";
	public static final String KEY_OS_VERSION       = "OS Version";
	public static final String KEY_REPORT_VERSION   = "Report Version";
	public static final String KEY_EXCEPTION_TYPE   = "Exception Type";
	public static final String KEY_EXCEPTION_CODES  = "Exception Codes";
	public static final String KEY_CRASHED_THREAD   = "Crashed Thread";
	public static final String KEY_EXCEPTION_THREAD = "Exception Thread";
	public static final String KEY_EXCEPTION_CLASS  = "Exception Class";
	public static final String KEY_EXCEPTION_REASON = "Exception Reason";
	public static final String KEY_STACK_TRACE      = "Stack Trace";

	protected static final String KEY_HARDWARE_MODEL_DELIM  = KEY_HARDWARE_MODEL + DELIM;
	protected static final String KEY_PROCESS_DELIM         = KEY_PROCESS + DELIM;
	protected static final String KEY_PATH_DELIM            = KEY_PATH + DELIM;
	protected static final String KEY_IDENTIFIER_DELIM      = KEY_IDENTIFIER + DELIM;
	protected static final String KEY_VERSION_DELIM         = KEY_VERSION + DELIM;
	protected static final String KEY_CODE_TYPE_DELIM       = KEY_CODE_TYPE + DELIM;
	protected static final String KEY_PARENT_PROCESS_DELIM  = KEY_PARENT_PROCESS + DELIM;
	protected static final String KEY_DATE_TIME_DELIM       = KEY_DATE_TIME + DELIM;
	protected static final String KEY_OS_VERSION_DELIM      = KEY_OS_VERSION + DELIM;
	protected static final String KEY_REPORT_VERSION_DELIM  = KEY_REPORT_VERSION + DELIM;
	protected static final String KEY_EXCEPTION_TYPE_DELIM  = KEY_EXCEPTION_TYPE + DELIM;
	protected static final String KEY_EXCEPTION_CODES_DELIM = KEY_EXCEPTION_CODES + DELIM;
	protected static final String KEY_CRASHED_THREAD_DELIM  = KEY_CRASHED_THREAD + DELIM;


	public boolean parseCrashLog(String fileContents)
	{
		int keysFound = 0;

		String hwModelValue = getValueForKey(KEY_HARDWARE_MODEL_DELIM,fileContents);
		String processValue = getValueForKey(KEY_PROCESS_DELIM,fileContents);
		String pathValue = getValueForKey(KEY_PATH_DELIM,fileContents);
		String identifierValue = getValueForKey("\n" + KEY_IDENTIFIER_DELIM,fileContents);
		String versionValue = getValueForKey(KEY_VERSION_DELIM,fileContents);
		String codeTypeValue = getValueForKey(KEY_CODE_TYPE_DELIM,fileContents);
		String parentProcessValue = getValueForKey(KEY_PARENT_PROCESS_DELIM,fileContents);
		String dateTimeValue = getValueForKey(KEY_DATE_TIME_DELIM,fileContents);
		String osVersionValue = getValueForKey(KEY_OS_VERSION_DELIM,fileContents);
		String reportVersionValue = getValueForKey(KEY_REPORT_VERSION_DELIM,fileContents);
		String exceptionTypeValue = getValueForKey(KEY_EXCEPTION_TYPE_DELIM,fileContents);
		String exceptionCodesValue = getValueForKey(KEY_EXCEPTION_CODES_DELIM,fileContents);
		String crashedThreadValue = getValueForKey(KEY_CRASHED_THREAD_DELIM,fileContents);

		if (hwModelValue != null) {
			++keysFound;
			mapCrashAttributes.put(KEY_HARDWARE_MODEL,hwModelValue);
		}

		if (processValue != null) {
			++keysFound;
			mapCrashAttributes.put(KEY_PROCESS,processValue);
		}

		if (pathValue != null) {
			++keysFound;
			mapCrashAttributes.put(KEY_PATH,pathValue);
		}

		if (identifierValue != null) {
			++keysFound;
			mapCrashAttributes.put(KEY_IDENTIFIER,identifierValue);
		}

		if (versionValue != null) {
			++keysFound;
			mapCrashAttributes.put(KEY_VERSION,versionValue);
		}

		if (codeTypeValue != null) {
			++keysFound;
			mapCrashAttributes.put(KEY_CODE_TYPE,codeTypeValue);
		}

		if (parentProcessValue != null) {
			++keysFound;
			mapCrashAttributes.put(KEY_PARENT_PROCESS,parentProcessValue);
		}

		if (dateTimeValue != null) {
			++keysFound;
			mapCrashAttributes.put(KEY_DATE_TIME,dateTimeValue);
		}

		if (osVersionValue != null) {
			++keysFound;
			mapCrashAttributes.put(KEY_OS_VERSION,osVersionValue);
		}

		if (reportVersionValue != null) {
			++keysFound;
			mapCrashAttributes.put(KEY_REPORT_VERSION,reportVersionValue);
		}

		if (exceptionTypeValue != null) {
			++keysFound;
			mapCrashAttributes.put(KEY_EXCEPTION_TYPE,exceptionTypeValue);
		}

		if (exceptionCodesValue != null) {
			++keysFound;
			mapCrashAttributes.put(KEY_EXCEPTION_CODES,exceptionCodesValue);
		}

		if (crashedThreadValue != null) {
			++keysFound;
			mapCrashAttributes.put(KEY_CRASHED_THREAD,crashedThreadValue);
		}

		final int posUncaughtException = fileContents.indexOf("uncaught exception");

		if( posUncaughtException > 0 ) {
			final int posFirstQuote = fileContents.indexOf("'", posUncaughtException+18);
			if( posFirstQuote > 0 ) {
				final int posSecondQuote = fileContents.indexOf("'", posFirstQuote+1);
				if( posSecondQuote > 0 ) {
					String exceptionClass = fileContents.substring(posFirstQuote+1,posSecondQuote);

					++keysFound;
					mapCrashAttributes.put(KEY_EXCEPTION_CLASS, exceptionClass);

					final int posReasonLabel = fileContents.indexOf(", reason:", posSecondQuote+1);

					if( posReasonLabel > 0 ) {
						final int posFirstReasonQuote = fileContents.indexOf("'", posReasonLabel+9);

						if( posFirstReasonQuote > 0 ) {
							final int posSecondReasonQuote = fileContents.indexOf("'", posFirstReasonQuote+1);
							if( posSecondReasonQuote > 0 ) {
								String exceptionReason = fileContents.substring(posFirstReasonQuote+1,posSecondReasonQuote);

								++keysFound;
								mapCrashAttributes.put(KEY_EXCEPTION_REASON,exceptionReason);
							}
						}
					}
				}
			}
		}

		String stackTrace = null;

		String KEY_EXCEPTION_BACKTRACE = "Exception Backtrace";
		final int posExceptionBacktrace = fileContents.indexOf(KEY_EXCEPTION_BACKTRACE);
		if( posExceptionBacktrace > 0 ) {
			String threadIdLine = this.getLineAfterString(KEY_EXCEPTION_BACKTRACE,fileContents);
			if( threadIdLine != null && threadIdLine.length() > 0 ) {
				if( threadIdLine.startsWith("Thread ") && threadIdLine.endsWith(":") ) {
					String exceptionThreadIndex = threadIdLine.substring(7).trim();
					exceptionThreadIndex = exceptionThreadIndex.substring(0,exceptionThreadIndex.length()-1);
					mapCrashAttributes.put(KEY_EXCEPTION_THREAD,exceptionThreadIndex);
					stackTrace = getNonBlankLinesAfterString(threadIdLine,fileContents);
				}
			}
		}

		if( (null == stackTrace) && (crashedThreadValue.length() > 0) ) {
			String threadIdLine = "Thread " + crashedThreadValue.trim() + " Crashed:";
			stackTrace = getNonBlankLinesAfterString(threadIdLine,fileContents);
		}

		if( (stackTrace != null) && (stackTrace.length() > 0) ) {
			mapCrashAttributes.put(KEY_STACK_TRACE,stackTrace);
		}

		return(keysFound > 7);
	}

	public Object toModel(HashMap<String,String> mapCrashAttributes) {
		iOSCrashLog crashLog = new iOSCrashLog();

		if( mapCrashAttributes.containsKey(KEY_HARDWARE_MODEL) ) {
			crashLog.setHardwareModel(mapCrashAttributes.get(KEY_HARDWARE_MODEL));
		}

		if( mapCrashAttributes.containsKey(KEY_PROCESS) ) {
			crashLog.setProcess(mapCrashAttributes.get(KEY_PROCESS));
		}

		if( mapCrashAttributes.containsKey(KEY_PATH) ) {
			crashLog.setPath(mapCrashAttributes.get(KEY_PATH));
		}

		if( mapCrashAttributes.containsKey(KEY_IDENTIFIER) ) {
			crashLog.setIdentifier(mapCrashAttributes.get(KEY_IDENTIFIER));
		}

		if( mapCrashAttributes.containsKey(KEY_VERSION) ) {
			crashLog.setAppVersion(mapCrashAttributes.get(KEY_VERSION));
		}

		if( mapCrashAttributes.containsKey(KEY_CODE_TYPE) ) {
			crashLog.setCodeType(mapCrashAttributes.get(KEY_CODE_TYPE));
		}

		if( mapCrashAttributes.containsKey(KEY_PARENT_PROCESS) ) {
			crashLog.setParentProcess(mapCrashAttributes.get(KEY_PARENT_PROCESS));
		}

		if( mapCrashAttributes.containsKey(KEY_DATE_TIME) ) {
			crashLog.setDateTime(mapCrashAttributes.get(KEY_DATE_TIME));
		}

		if( mapCrashAttributes.containsKey(KEY_OS_VERSION) ) {
			crashLog.setOsVersion(mapCrashAttributes.get(KEY_OS_VERSION));
		}

		if( mapCrashAttributes.containsKey(KEY_REPORT_VERSION) ) {
			crashLog.setReportVersion(mapCrashAttributes.get(KEY_REPORT_VERSION));
		}

		if( mapCrashAttributes.containsKey(KEY_EXCEPTION_TYPE) ) {
			crashLog.setExceptionType(mapCrashAttributes.get(KEY_EXCEPTION_TYPE));
		}

		if( mapCrashAttributes.containsKey(KEY_EXCEPTION_CODES) ) {
			crashLog.setExceptionCodes(mapCrashAttributes.get(KEY_EXCEPTION_CODES));
		}

		if( mapCrashAttributes.containsKey(KEY_CRASHED_THREAD) ) {
			crashLog.setCrashedThread(mapCrashAttributes.get(KEY_CRASHED_THREAD));
		}

		if( mapCrashAttributes.containsKey(KEY_EXCEPTION_THREAD) ) {
			crashLog.setExceptionThread(mapCrashAttributes.get(KEY_EXCEPTION_THREAD));
		}

		if( mapCrashAttributes.containsKey(KEY_EXCEPTION_CLASS) ) {
			crashLog.setExceptionClass(mapCrashAttributes.get(KEY_EXCEPTION_CLASS));
		}

		if( mapCrashAttributes.containsKey(KEY_EXCEPTION_REASON) ) {
			crashLog.setExceptionReason(mapCrashAttributes.get(KEY_EXCEPTION_REASON));
		}

		if( mapCrashAttributes.containsKey(KEY_STACK_TRACE) ) {
			crashLog.setStackTrace(mapCrashAttributes.get(KEY_STACK_TRACE));
		}

		return crashLog;
	}

	@Override
	public String getCrashSummary() {
		StringBuilder sb = new StringBuilder();

		String hwModel = mapCrashAttributes.get(KEY_HARDWARE_MODEL);
		String exceptionType = mapCrashAttributes.get(KEY_EXCEPTION_TYPE);
		String exceptionClass = mapCrashAttributes.get(KEY_EXCEPTION_CLASS);
		String exceptionReason = mapCrashAttributes.get(KEY_EXCEPTION_REASON);
		String exceptionThread = mapCrashAttributes.get(KEY_EXCEPTION_THREAD);
		String crashedThread = mapCrashAttributes.get(KEY_CRASHED_THREAD);
		String exceptionCodes = mapCrashAttributes.get(KEY_EXCEPTION_CODES);


		if( (hwModel != null) && (hwModel.length() > 0) ) {
			sb.append("HW model: ");
			sb.append(hwModel);
			sb.append("; ");
		}

		if( (exceptionType != null) && (exceptionType.length() > 0) ) {
			sb.append(exceptionType);
			sb.append("; ");
		}

		if( (exceptionClass != null) && (exceptionClass.length() > 0) ) {
			sb.append(exceptionClass);
			sb.append("; ");
		}

		if( (exceptionReason != null) && (exceptionReason.length() > 0) ) {
			sb.append(exceptionReason);
		} else {
			if( (exceptionThread != null) && (exceptionThread.length() > 0) ) {
				sb.append("Exception in thread ");
				sb.append(exceptionThread);
			} else if( (crashedThread != null) && (crashedThread.length() > 0) ) {
				sb.append("Crash in thread ");
				sb.append(crashedThread);
			} 
		}

		if( (exceptionCodes != null) && (exceptionCodes.length() > 0) ) {
			sb.append(" Exception Codes: ");
			sb.append(exceptionCodes);
		}


		return sb.toString();
	}

}

