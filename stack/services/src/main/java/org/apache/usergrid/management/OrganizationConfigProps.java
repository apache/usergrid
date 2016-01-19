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
package org.apache.usergrid.management;

import org.apache.usergrid.system.ServerEnvironmentProps;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

public interface OrganizationConfigProps {
    String ORGPROPERTIES_API_URL_BASE = ServerEnvironmentProps.API_URL_BASE;
    String ORGPROPERTIES_DEFAULT_CONNECTION_PARAM = "usergrid.rest.default-connection-param";
    String ORGPROPERTIES_ADMIN_SYSADMIN_EMAIL = AccountCreationProps.PROPERTIES_ADMIN_SYSADMIN_EMAIL;

    // these can not currently be set as org config items, but they select
    // the full URL to be created from the org-specific API URL base and the
    // hardcoded paths
    //
    // use these specifiers with getFullUrl() to select the URL to be built
    enum WorkflowUrl {
        ORGANIZATION_ACTIVATION_URL,
        ADMIN_ACTIVATION_URL,
        ADMIN_CONFIRMATION_URL,
        ADMIN_RESETPW_URL,
        USER_ACTIVATION_URL,
        USER_CONFIRMATION_URL,
        USER_RESETPW_URL
    }

    Set<String> getOrgPropertyNames();

    Properties getPropertiesMap();

    Map<String, String> getDefaultPropertiesMap();

    Map<String, String> getOrgPropertiesMap();

    String getProperty(String name);

    String getProperty(String name, String defaultValue);

    boolean boolProperty(String name, boolean defaultValue);

    int intProperty(String name, int defaultValue);

    long longProperty(String name, long defaultValue);

    void setProperty(String name, String value);

    String getFullUrlTemplate(WorkflowUrl urlType);

    String getFullUrl(WorkflowUrl urlType, Object ... arguments);

}
