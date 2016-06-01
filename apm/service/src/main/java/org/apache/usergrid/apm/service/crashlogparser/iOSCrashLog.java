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

/**
 * 
 * @author Paul Dardeau
 *
 */

public class iOSCrashLog extends AbstractCrashLog
{
	private String process;
	private String path;
	private String identifier;
	private String codeType;
	private String parentProcess;
	private String reportVersion;
	private String exceptionType;
	private String exceptionCodes;
	private String crashedThread;
	private String exceptionThread;
	private String exceptionClass;
	private String exceptionReason;
	private String stackTrace;

	
	public String getCrashType() {
		StringBuilder crashType = new StringBuilder();
		
		crashType.append(exceptionType);
		crashType.append(";");
		crashType.append(stackTrace);
		
		return crashType.toString();
	}
	
	public boolean isSameCrashType(iOSCrashLog otherCrashLog) {
		return this.getCrashType().equals(otherCrashLog.getCrashType());
	}

	public boolean crashOccurredOnSimulator() {
		String hwModel = getHardwareModel();
		String osVersion = getOsVersion();
		boolean crashOnSimulator = false;
		
		if( ((hwModel != null) && hwModel.equals("x86_64")) ||
			((osVersion != null) && osVersion.startsWith("Max OS X")) ||
			((codeType != null) && codeType.equals("X86")) )
		{
			crashOnSimulator = true;
		}
		
		return crashOnSimulator;
	}
	
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");
		
		result.append("iOSCrashLog" + NEW_LINE);
		
		result.append(" Hardware Model: " + getHardwareModel() + NEW_LINE);
		result.append(" App Version: " + getAppVersion() + NEW_LINE);
		result.append(" OS Version: " + getOsVersion() + NEW_LINE);
		result.append(" Date/Time: " + getDateTime() + NEW_LINE);
		
		result.append(" Process: " + process + NEW_LINE);
		result.append(" Path: " + path + NEW_LINE);
		result.append(" Identifier: " + identifier + NEW_LINE);
		result.append(" Code Type: " + codeType + NEW_LINE);
		result.append(" Parent Process: " + parentProcess + NEW_LINE);
		result.append(" Report Version: " + reportVersion + NEW_LINE);
		result.append(" Exception Type: " + exceptionType + NEW_LINE);
		result.append(" Exception Codes: " + exceptionCodes + NEW_LINE);
		result.append(" Crashed Thread: " + crashedThread + NEW_LINE);
		result.append(" Exception Thread: " + exceptionThread + NEW_LINE);
		result.append(" Exception Class: " + exceptionClass + NEW_LINE);
		result.append(" Exception Reason: " + exceptionReason + NEW_LINE);
		result.append(" On Simulator (Y/N): " + (crashOccurredOnSimulator() ? "Y" : "N") + NEW_LINE);
		result.append(" Stack Trace: " + stackTrace + NEW_LINE);
		
		return result.toString();
	}

	public String getProcess() {
		return process;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getIdentifier() {
		return identifier;
	}
	
	public String getCodeType() {
		return codeType;
	}
	
	public String getParentProcess() {
		return parentProcess;
	}
	
	public String getReportVersion() {
		return reportVersion;
	}
	
	public String getExceptionType() {
		return exceptionType;
	}
	
	public String getExceptionCodes() {
		return exceptionCodes;
	}
	
	public String getCrashedThread() {
		return crashedThread;
	}
	
	public String getExceptionThread() {
		return exceptionThread;
	}
	
	public String getExceptionClass() {
		return exceptionClass;
	}
	
	public String getExceptionReason() {
		return exceptionReason;
	}
	
	public String getStackTrace() {
		return stackTrace;
	}

	public void setProcess(String process) {
		this.process = process;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	public void setCodeType(String codeType) {
		this.codeType = codeType;
	}
	
	public void setParentProcess(String parentProcess) {
		this.parentProcess = parentProcess;
	}
	
	public void setReportVersion(String reportVersion) {
		this.reportVersion = reportVersion;
	}
	
	public void setExceptionType(String exceptionType) {
		this.exceptionType = exceptionType;
	}
	
	public void setExceptionCodes(String exceptionCodes) {
		this.exceptionCodes = exceptionCodes;
	}
	
	public void setCrashedThread(String crashedThread) {
		this.crashedThread = crashedThread;
	}
	
	public void setExceptionThread(String exceptionThread) {
		this.exceptionThread = exceptionThread;
	}
	
	public void setExceptionClass(String exceptionClass) {
		this.exceptionClass = exceptionClass;
	}
	
	public void setExceptionReason(String exceptionReason) {
		this.exceptionReason = exceptionReason;
	}
	
	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}

}
