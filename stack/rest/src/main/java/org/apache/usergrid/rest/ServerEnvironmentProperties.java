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
package org.apache.usergrid.rest;


import org.apache.usergrid.system.ServerEnvironmentProps;
import java.util.Properties;


public class ServerEnvironmentProperties {

    private Properties properties;


    public ServerEnvironmentProperties( Properties properties ) {
        this.properties = properties;
    }


    public Properties getProperties() {
        return properties;
    }


    public String getProperty( String key ) {
        return properties.getProperty( key );
    }


    @Deprecated // use OrganizationConfig to access, as this can be overridden for an org
    public String getApiBase() {
        return properties.getProperty(ServerEnvironmentProps.API_URL_BASE);
    }


    public String getRecaptchaPublic() {
        return properties.getProperty( ServerEnvironmentProps.RECAPTCHA_PUBLIC );
    }


    public String getRecaptchaPrivate() {
        return properties.getProperty( ServerEnvironmentProps.RECAPTCHA_PRIVATE );
    }


    public String getRedirectRoot() {
        return properties.getProperty( ServerEnvironmentProps.REDIRECT_ROOT );
    }
}
