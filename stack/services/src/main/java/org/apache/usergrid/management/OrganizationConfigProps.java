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


import java.util.Map;
import java.util.Set;

public interface OrganizationConfigProps {
    String PROPERTIES_DEFAULT_CONNECTION_PARAM = "usergrid.rest.default-connection-param";
    String PROPERTIES_ADMIN_SYSADMIN_EMAIL = AccountCreationProps.PROPERTIES_ADMIN_SYSADMIN_EMAIL;
    String PROPERTIES_ADMIN_ACTIVATION_URL = AccountCreationProps.PROPERTIES_ADMIN_ACTIVATION_URL;
    String PROPERTIES_ADMIN_CONFIRMATION_URL = AccountCreationProps.PROPERTIES_ADMIN_CONFIRMATION_URL;
    String PROPERTIES_ADMIN_RESETPW_URL = AccountCreationProps.PROPERTIES_ADMIN_RESETPW_URL;

    Set<String> getPropertyNames();

    Map<String, String> getPropertyMap();

    String getProperty(String name);

    String getProperty(String name, String defaultValue);

    boolean isProperty(String name, boolean defaultValue);

    int intProperty(String name, int defaultValue);

    long longProperty(String name, long defaultValue);

    void setProperty(String name, String value);

}
