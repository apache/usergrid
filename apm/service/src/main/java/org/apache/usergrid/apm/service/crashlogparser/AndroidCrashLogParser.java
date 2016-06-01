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


/**
 * 
 * @author Paul Dardeau
 *
 */

public class AndroidCrashLogParser extends AbstractCrashLogParser implements CrashLogParser
{
	public static final String KEY_PACKAGE = "Package";
	public static final String KEY_VERSION = "Version";
	public static final String KEY_ANDROID = "Android";
	public static final String KEY_MFG     = "Manufacturer";
	public static final String KEY_MODEL   = "Model";
	public static final String KEY_DATE    = "Date";

	public static final String KEY_SOURCE_FILE = "SourceFile";
	public static final String KEY_LINE_NUMBER = "LineNumber";
	public static final String KEY_CLASS_NAME  = "ClassName";
	public static final String KEY_METHOD_NAME = "MethodName";

	public static final String CAUSED_BY   = "Caused by";

	public static final String DELIM = ":";

	protected static final String KEY_PACKAGE_DELIM = KEY_PACKAGE + DELIM;
	protected static final String KEY_VERSION_DELIM = KEY_VERSION + DELIM;
	protected static final String KEY_ANDROID_DELIM = KEY_ANDROID + DELIM;
	protected static final String KEY_MFG_DELIM = KEY_MFG + DELIM;
	protected static final String KEY_MODEL_DELIM = KEY_MODEL + DELIM;
	protected static final String KEY_DATE_DELIM = KEY_DATE + DELIM;

	protected static final String CAUSED_BY_DELIM = CAUSED_BY + DELIM;


	public boolean parseCrashLog(String fileContents)
	{
		int keysFound = 0;
		String locationOfCrash = null;

		String packageValue = getValueForKey(KEY_PACKAGE_DELIM,fileContents);
		String versionValue = getValueForKey(KEY_VERSION_DELIM,fileContents);
		String androidValue = getValueForKey(KEY_ANDROID_DELIM,fileContents);
		String mfgValue = getValueForKey(KEY_MFG_DELIM,fileContents);
		String modelValue = getValueForKey(KEY_MODEL_DELIM,fileContents);
		String dateValue = getValueForKey(KEY_DATE_DELIM,fileContents);

		String causedByValue = getValueForKey(CAUSED_BY_DELIM,fileContents);

		if( packageValue != null ) {
			++keysFound;
			mapCrashAttributes.put(KEY_PACKAGE,packageValue);
		}

		if( versionValue != null ) {
			++keysFound;
			mapCrashAttributes.put(KEY_VERSION,versionValue);
		}

		if( androidValue != null ) {
			++keysFound;
			mapCrashAttributes.put(KEY_ANDROID,androidValue);
		}

		if( mfgValue != null ) {
			++keysFound;
			mapCrashAttributes.put(KEY_MFG,mfgValue);
		}

		if( modelValue != null ) {
			++keysFound;
			mapCrashAttributes.put(KEY_MODEL,modelValue);
		}

		if( dateValue != null ) {
			++keysFound;
			mapCrashAttributes.put(KEY_DATE,dateValue);
		}

		if( causedByValue != null ) {
			++keysFound;
			mapCrashAttributes.put(CAUSED_BY,causedByValue);
			locationOfCrash = getLineAfterString(CAUSED_BY_DELIM,fileContents);
		}
		else {
			String nonEmptyLineAfterDate = getNonBlankLinesAfterString(KEY_DATE, fileContents);
			nonEmptyLineAfterDate = nonEmptyLineAfterDate.substring(1); //First character is new line
			String cause = nonEmptyLineAfterDate.substring(0, nonEmptyLineAfterDate.indexOf('\n'));
			mapCrashAttributes.put(CAUSED_BY,cause);	
			locationOfCrash = getLineAfterString(cause,fileContents);
		}

		if( locationOfCrash != null && locationOfCrash.length() > 0 )
		{
			locationOfCrash = locationOfCrash.trim();

			if( locationOfCrash.startsWith("at ") )
			{
				locationOfCrash = locationOfCrash.substring(3);
			}

			final int posOpenParen = locationOfCrash.indexOf("(");
			final int posCloseParen = locationOfCrash.indexOf(")");

			if( (posOpenParen > -1) && (posCloseParen > -1) && (posCloseParen > posOpenParen) )
			{
				String fileAndLine = locationOfCrash.substring(posOpenParen+1,posCloseParen);

				if( (fileAndLine != null) && (fileAndLine.length() > 0) )
				{
					final int posColon = fileAndLine.indexOf(":");
					if( posColon > 0 )
					{
						mapCrashAttributes.put(KEY_SOURCE_FILE,fileAndLine.substring(0,posColon));
						mapCrashAttributes.put(KEY_LINE_NUMBER,fileAndLine.substring(posColon+1));
					}
				}

				locationOfCrash = locationOfCrash.substring(0,posOpenParen);
			}

			final int posLastPeriod = locationOfCrash.lastIndexOf(".");

			if( posLastPeriod > -1 )
			{
				mapCrashAttributes.put(KEY_METHOD_NAME,locationOfCrash.substring(posLastPeriod+1));
				mapCrashAttributes.put(KEY_CLASS_NAME,locationOfCrash.substring(0,posLastPeriod));
			}
		}


		return( keysFound > 3 );
	}

	public Object toModel(HashMap<String,String> mapCrashAttributes) {
		AndroidCrashLog crashLog = new AndroidCrashLog();

		if( mapCrashAttributes.containsKey(KEY_PACKAGE) ) {
			crashLog.setPackageName(mapCrashAttributes.get(KEY_PACKAGE));
		}

		if( mapCrashAttributes.containsKey(KEY_VERSION) ) {
			crashLog.setAppVersion(mapCrashAttributes.get(KEY_VERSION));
		}

		if( mapCrashAttributes.containsKey(KEY_ANDROID) ) {
			crashLog.setOsVersion(mapCrashAttributes.get(KEY_ANDROID));
		}

		if( mapCrashAttributes.containsKey(KEY_MFG) ) {
			crashLog.setManufacturer(mapCrashAttributes.get(KEY_MFG));
		}

		if( mapCrashAttributes.containsKey(KEY_MODEL) ) {
			crashLog.setHardwareModel(mapCrashAttributes.get(KEY_MODEL));
		}

		if( mapCrashAttributes.containsKey(KEY_DATE) ) {
			crashLog.setDateTime(mapCrashAttributes.get(KEY_DATE));
		}

		if( mapCrashAttributes.containsKey(KEY_SOURCE_FILE) ) {
			crashLog.setSourceFile(mapCrashAttributes.get(KEY_SOURCE_FILE));
		}

		if( mapCrashAttributes.containsKey(KEY_LINE_NUMBER) ) {
			crashLog.setLineNumber(mapCrashAttributes.get(KEY_LINE_NUMBER));
		}

		if( mapCrashAttributes.containsKey(KEY_CLASS_NAME) ) {
			crashLog.setClassName(mapCrashAttributes.get(KEY_CLASS_NAME));
		}

		if( mapCrashAttributes.containsKey(KEY_METHOD_NAME) ) {
			crashLog.setMethodName(mapCrashAttributes.get(KEY_METHOD_NAME));
		}

		if( mapCrashAttributes.containsKey(CAUSED_BY) ) {
			crashLog.setCausedBy(mapCrashAttributes.get(CAUSED_BY));
		}

		return crashLog;
	}


	@Override
	public String getCrashSummary() {
		if (mapCrashAttributes.get(KEY_METHOD_NAME) == null)
			return mapCrashAttributes.get(CAUSED_BY);
		else
			return mapCrashAttributes.get(CAUSED_BY) + " Method : " + mapCrashAttributes.get(KEY_METHOD_NAME) 
					+ " Class :" + mapCrashAttributes.get(KEY_CLASS_NAME) + " Line: " + mapCrashAttributes.get(KEY_LINE_NUMBER);
	}

}
