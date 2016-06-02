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
package org.apache.usergrid.apm.wiring;

public class UsergridApmIntegrationMobileAPMConstants {

    // Different App Config Types
    public static final String CONFIG_TYPE_DEFAULT = "Default";
    public static final String CONFIG_TYPE_DEVICE_LEVEL = "Beta";
    public static final String CONFIG_TYPE_DEVICE_TYPE = "Device";
    public static final String CONFIG_TYPE_AB = "A/B";

    // Log Levels
    public static final int LOG_ASSERT = 7;
    public static final int LOG_ERROR = 6;
    public static final int LOG_WARN = 5;
    public static final int LOG_INFO = 4;
    public static final int LOG_DEBUG = 3;
    public static final int LOG_VERBOSE = 2;

    public static final String[] logLevelsString = { "", "", "V", "D", "I",
            "W", "E", "A" };

    // Configuration Filters
    public static final String FILTER_TYPE_DEVICE_NUMBER = "DEVICE_NUMBER";
    public static final String FILTER_TYPE_DEVICE_ID = "DEVICE_ID";
    public static final String FILTER_TYPE_DEVICE_MODEL = "DEVICE_MODEL";
    public static final String FILTER_TYPE_DEVICE_PLATFROM = "DEVICE_PLATFORM";
    public static final String FILTER_TYPE_NETWORK_TYPE = "NETWORK_TYPE";
    public static final String FILTER_TYPE_NETWORK_OPERATOR = "NETWORK_OPERATOR";

    public static final String APIGEE_MOBILE_APM_CONFIG_JSON_KEY = "apigeeMobileConfig";

    // It needs to go to a property file

    public static final String APIGEE_APM_ADMIN_DEFAULT_EMAIL_ADDRESS = "mobile@apigee.com";

    public static final String CHART_PERIOD_1HR = "1h";
    public static final String CHART_PERIOD_3HR = "3h";
    public static final String CHART_PERIOD_6HR = "6h";
    public static final String CHART_PERIOD_12HR = "12h";
    public static final String CHART_PERIOD_24HR = "24h";
    public static final String CHART_PERIOD_1WK = "1w";

    public static final String CHART_DATA_REFERENCE_POINT_NOW = "NOW";
    public static final String CHART_DATA_REFERENCE_POINT_YESTERDAY = "YESTERDAY";
    public static final String CHART_DATA_REFERENCE_POINT_LAST_WEEK = "LAST_WEEK";

    public static final String CHART_VIEW_TIMESERIES = "TIMESERIES";
    public static final String CHART_VIEW_PIE = "PIE";
    public static final String CHART_VIEW_BAR = "BAR";

}
