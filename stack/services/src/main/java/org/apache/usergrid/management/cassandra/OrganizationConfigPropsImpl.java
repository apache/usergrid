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
package org.apache.usergrid.management.cassandra;


import org.apache.usergrid.management.OrganizationConfigProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;


public class OrganizationConfigPropsImpl implements OrganizationConfigProps {
    private static final Logger logger = LoggerFactory.getLogger( OrganizationConfigPropsImpl.class );

    private static final String DEFAULT_CONNECTION_PARAM_DEFAULTVALUE = "all";
    private static final String ADMIN_SYSADMIN_EMAIL_DEFAULTVALUE = null; // null will fall back to system level admin
    private static final String ADMIN_ACTIVATION_URL_DEFAULTVALUE = ""; // should be configured in properties file
    private static final String ADMIN_CONFIRMATION_URL_DEFAULTVALUE = ""; // should be configured in properties file
    private static final String ADMIN_RESETPW_URL_DEFAULTVALUE = ""; // should be configured in properties file

    private static final Map<String, String> noConfigDefaults = new HashMap<>();
    static {
        noConfigDefaults.put(PROPERTIES_DEFAULT_CONNECTION_PARAM, DEFAULT_CONNECTION_PARAM_DEFAULTVALUE);
        noConfigDefaults.put(PROPERTIES_ADMIN_SYSADMIN_EMAIL, ADMIN_SYSADMIN_EMAIL_DEFAULTVALUE);
        noConfigDefaults.put(PROPERTIES_ADMIN_ACTIVATION_URL, ADMIN_ACTIVATION_URL_DEFAULTVALUE);
        noConfigDefaults.put(PROPERTIES_ADMIN_CONFIRMATION_URL, ADMIN_CONFIRMATION_URL_DEFAULTVALUE);
        noConfigDefaults.put(PROPERTIES_ADMIN_RESETPW_URL, ADMIN_RESETPW_URL_DEFAULTVALUE);
    }

    //protected final Properties properties;
    protected final Map<String, String> map;

    public OrganizationConfigPropsImpl(Properties properties) {
        map = new HashMap<>();
        noConfigDefaults.forEach((k,v) -> map.put(k, properties.getProperty(k, v)));
    }

    public Set<String> getPropertyNames() {
        return new HashSet<>(noConfigDefaults.keySet());
    }

    public Map<String, String> getPropertyMap() {
        return new HashMap<>(map);
    }

    public String getProperty(String name) {
        String propertyValue = map.get(name);
        if (isBlank(propertyValue)) {
            logger.warn("Missing value for " + name);
            propertyValue = null;
        }
        return propertyValue;
    }


    public String getProperty(String name, String defaultValue) {
        return map.getOrDefault(name, defaultValue);
    }


    public boolean isProperty(String name, boolean defaultValue) {
        String val = getProperty(name);
        if (isBlank(val)) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(val);
        }
    }

    public int intProperty(String name, int defaultValue) {
        String val = getProperty(name);
        if (isBlank(val)) {
            return defaultValue;
        } else {
            return Integer.parseInt(val);
        }
    }

    public long longProperty(String name, long defaultValue) {
        String val = getProperty(name);
        if (isBlank(val)) {
            return defaultValue;
        } else {
            return Long.parseLong(val);
        }
    }

    public void setProperty(String name, String value) {
        map.put(name,value);
    }

}