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


public final class MessageConstants {
	
	public static String APP_ID = "APP_ID";
	public static String DEVICE_ID = "DEVICE_ID";
	public static String NETWORK_CARRIER = "NETWORK_CARRIER";
	public static String NETWORK_TYPE = "NETWORK_TYPE";
	public static String LOCATION = "LOCATION";
	public static String REGEX_ID = "REGEX_ID";
	public static String REGEX_URL = "REGEX_URL";
	public static String FIRST_SAMPLE_TIMESTAMP = "START_TIME";
	public static String LAST_SAMPLE_TIMESTAMP = "END_TIME";
	public static String NUM_SAMPLE = "NUM_SAMPLES";
	public static String NUM_ERRORS = "NUM_ERRORS";
	public static String SUM_LATENCY = "SUM_LATENCY";
	public static String MIN_LATENCY = "MIN_LATENCY";
	public static String MAX_LATENCY = "MAX_LATENCY";
	
   /**
    * A sample message format
    * Line 1: Column Headers Number of columns in headers could change
    * Line 2: Data Row 1
    * Line 3: Data Row 2
    * ..
    * ..
    * ..
    * Example:
    * APP_ID,DEVICE_ID,REGEX_URL,START_TIME,END_TIME,NUM_ERRORS
    * 1,2345678900,.*google.com,1234567,1238999,1
    * 1,2345678900,.*amazon.com/price/quote,1234,678
    */
}
