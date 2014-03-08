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
package org.apache.usergrid.rest.utils;


import javax.ws.rs.core.MultivaluedMap;


/**
 * Utilities and constants for path noodling
 *
 * @author zznate
 */
public class PathingUtils {

    public static final String PARAM_APP_NAME = "applicationName";
    public static final String PARAM_ORG_NAME = "organizationName";
    public static final String SLASH = "/";


    /**
     * Combine the two parameters to return a new path which represents the appName. Previously, application names had
     * to be unique accross the system. This is part of the refactoring to treat the application name internally as a
     * combination of organization and application names.
     *
     * @return a new string in the format "organizationName/applicationName"
     */
    public static String assembleAppName( String organizationName, String applicationName ) {
        return new String( organizationName.toLowerCase() + SLASH + applicationName.toLowerCase() );
    }


    /** Same as above except we pull the parameters from the pathParams */
    public static String assembleAppName( MultivaluedMap<String, String> pathParams ) {
        return assembleAppName( pathParams.getFirst( PARAM_ORG_NAME ), pathParams.getFirst( PARAM_APP_NAME ) );
    }
}
