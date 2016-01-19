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

import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;


public class OrganizationConfigPropsImpl implements OrganizationConfigProps {

    private static final String DEFAULTVALUE_API_URL_BASE = "http://localhost:8080";
    private static final String DEFAULTVALUE_DEFAULT_CONNECTION_PARAM = "all";
    private static final String DEFAULTVALUE_ADMIN_SYSADMIN_EMAIL = null; // null will fall back to system level admin

    private static final Map<String, String> noConfigDefaults = new HashMap<>();
    static {
        noConfigDefaults.put(ORGPROPERTIES_API_URL_BASE, DEFAULTVALUE_API_URL_BASE);
        noConfigDefaults.put(ORGPROPERTIES_DEFAULT_CONNECTION_PARAM, DEFAULTVALUE_DEFAULT_CONNECTION_PARAM);
        noConfigDefaults.put(ORGPROPERTIES_ADMIN_SYSADMIN_EMAIL, DEFAULTVALUE_ADMIN_SYSADMIN_EMAIL);
    }

    private static final String URLPATH_ORGANIZATION_ACTIVATION = "/management/organizations/%s/activate";
    private static final String URLPATH_ADMIN_ACTIVATION = "/management/users/%s/activate";
    private static final String URLPATH_ADMIN_CONFIRMATION = "/management/users/%s/confirm";
    private static final String URLPATH_ADMIN_RESETPW = "/management/users/%s/resetpw";
    private static final String URLPATH_USER_ACTIVATION = "/%s/%s/users/%s/activate";
    private static final String URLPATH_USER_CONFIRMATION = "/%s/%s/users/%s/confirm";
    private static final String URLPATH_USER_RESETPW = "/%s/%s/users/%s/resetpw";

    private static final Map<WorkflowUrl,String> urlPaths = new HashMap<>();
    static {
        urlPaths.put(WorkflowUrl.ORGANIZATION_ACTIVATION_URL, URLPATH_ORGANIZATION_ACTIVATION);
        urlPaths.put(WorkflowUrl.ADMIN_ACTIVATION_URL, URLPATH_ADMIN_ACTIVATION);
        urlPaths.put(WorkflowUrl.ADMIN_CONFIRMATION_URL, URLPATH_ADMIN_CONFIRMATION);
        urlPaths.put(WorkflowUrl.ADMIN_RESETPW_URL, URLPATH_ADMIN_RESETPW);
        urlPaths.put(WorkflowUrl.USER_ACTIVATION_URL, URLPATH_USER_ACTIVATION);
        urlPaths.put(WorkflowUrl.USER_CONFIRMATION_URL, URLPATH_USER_CONFIRMATION);
        urlPaths.put(WorkflowUrl.USER_RESETPW_URL, URLPATH_USER_RESETPW);
    }

    protected final Properties properties;

    protected final Map<String, String> defaultProperties;
    protected final Map<String, String> orgProperties;


    public OrganizationConfigPropsImpl(Properties properties) {
        this(properties, null);
    }

    public OrganizationConfigPropsImpl(Properties properties, Map<String, String> map) {
        this.properties = new Properties(properties);

        this.defaultProperties = new HashMap<>(noConfigDefaults);
        // add any corresponding properties to default props map
        noConfigDefaults.keySet().forEach((k) -> {
            String value = properties.getProperty(k);

            // ok if value is empty string
            if (value != null) {
                this.defaultProperties.put(k, value);
            }
        });

        this.orgProperties = map != null ? new HashMap<>(map) : new HashMap<>();
        //noConfigDefaults.forEach((k,v) -> map.put(k, properties.getProperty(k, v)));
    }

    public OrganizationConfigPropsImpl(OrganizationConfigProps orgConfigProps) {
        this.properties = orgConfigProps.getPropertiesMap();
        this.defaultProperties = orgConfigProps.getDefaultPropertiesMap();
        this.orgProperties = orgConfigProps.getOrgPropertiesMap();
    }

    public boolean orgPropertyNameValid(String name) {
        return noConfigDefaults.containsKey(name);
    }

    @Override
    public Set<String> getOrgPropertyNames() {
        return new HashSet<>(noConfigDefaults.keySet());
    }

    @Override
    public Properties getPropertiesMap() {
        return new Properties(properties);
    }

    @Override
    public Map<String, String> getDefaultPropertiesMap() {
        return new HashMap<>(defaultProperties);
    }

    @Override
    public Map<String, String> getOrgPropertiesMap() {
        return new HashMap<>(orgProperties);
    }

    //
    // 1. return from orgProperties (if it exists)
    // 2. return from properties (if it exists)
    // 3. return no config defaults (if it exists)
    // 4. if none exists, return null
    //
    @Override
    public String getProperty(String name) {
        String propertyValue;

        if (orgPropertyNameValid(name)) {
            // return from org-specific properties, if set
            propertyValue = orgProperties.containsKey(name) ?
                    orgProperties.get(name) : defaultProperties.get(name);
        } else {
            // not an org config item, return from properties
            propertyValue = properties.getProperty(name);
        }

        return !isBlank(propertyValue) ? propertyValue : null;
    }


    @Override
    public String getProperty(String name, String defaultValue) {
        String propertyValue = getProperty(name);
        return !isBlank(propertyValue) ? propertyValue : defaultValue;
    }


    @Override
    public boolean boolProperty(String name, boolean defaultValue) {
        String val = getProperty(name);
        return !isBlank(val) ? Boolean.parseBoolean(val) : defaultValue;
    }

    @Override
    public int intProperty(String name, int defaultValue) {
        String val = getProperty(name);
        return !isBlank(val) ? Integer.parseInt(val) : defaultValue;
    }

    @Override
    public long longProperty(String name, long defaultValue) {
        String val = getProperty(name);
        return !isBlank(val) ? Long.parseLong(val) : defaultValue;
    }

    @Override
    public void setProperty(String name, String value) {
        orgProperties.put(name,value);
    }

    @Override
    public String getFullUrlTemplate(WorkflowUrl urlType) {
        String urlTemplate = null;
        if (urlPaths.containsKey(urlType)) {
            urlTemplate = getProperty(ORGPROPERTIES_API_URL_BASE) + urlPaths.get(urlType);
        }
        return urlTemplate;
    }

    @Override
    public String getFullUrl(WorkflowUrl urlType, Object ... arguments) {
        String urlTemplate = getFullUrlTemplate(urlType);
        return String.format(urlTemplate, arguments);
    }

}