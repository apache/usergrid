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

public class AndroidCrashLog extends AbstractCrashLog
{
	private String packageName;
	private String manufacturer;
	private String sourceFile;
	private String lineNumber;
	private String className;
	private String methodName;
	private String causedBy;

	
	public String getCrashType() {
		StringBuilder crashType = new StringBuilder();

		crashType.append(causedBy);
		crashType.append(";");
		crashType.append(className);
		crashType.append(";");
		crashType.append(methodName);
		crashType.append(";");
		crashType.append(lineNumber);
		
		return crashType.toString();
	}
	
	public boolean isSameCrashType(AndroidCrashLog otherCrashLog) {
		return this.getCrashType().equals(otherCrashLog.getCrashType());
	}
	
	public boolean crashOccurredOnSimulator() {
		String hwModel = getHardwareModel();
		boolean crashOnSimulator = false;
		
		if( ((hwModel != null) && hwModel.equals("sdk")) ||
			((manufacturer != null) && manufacturer.equals("unknown")) )
		{
			crashOnSimulator = true;
		}
		
		return crashOnSimulator;
	}

	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");
		
		result.append("AndroidCrashLog" + NEW_LINE);
		
		result.append(" Hardware Model: " + getHardwareModel() + NEW_LINE);
		result.append(" App Version: " + getAppVersion() + NEW_LINE);
		result.append(" OS Version: " + getOsVersion() + NEW_LINE);
		result.append(" Date/Time: " + getDateTime() + NEW_LINE);
		
		result.append(" Package: " + packageName + NEW_LINE);
		result.append(" Manufacturer: " + manufacturer + NEW_LINE);
		result.append(" Source File: " + sourceFile + NEW_LINE);
		result.append(" Line Number: " + lineNumber + NEW_LINE);
		result.append(" Class Name: " + className + NEW_LINE);
		result.append(" Method Name: " + methodName + NEW_LINE);
		result.append(" Caused By: " + causedBy + NEW_LINE);
		result.append(" On Simulator (Y/N): " + (crashOccurredOnSimulator() ? "Y" : "N") + NEW_LINE);
		
		return result.toString();
	}

	public String getPackageName() {
		return packageName;
	}
	
	public String getManufacturer() {
		return manufacturer;
	}
	
	public String getSourceFile() {
		return sourceFile;
	}
	
	public String getLineNumber() {
		return lineNumber;
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getMethodName() {
		return methodName;
	}
	
	public String getCausedBy() {
		return causedBy;
	}
	
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	
	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}
	
	public void setSourceFile(String sourceFile) {
		this.sourceFile = sourceFile;
	}
	
	public void setLineNumber(String lineNumber) {
		this.lineNumber = lineNumber;
	}
	
	public void setClassName(String className) {
		this.className = className;
	}
	
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	
	public void setCausedBy(String causedBy) {
		this.causedBy = causedBy;
	}

}
