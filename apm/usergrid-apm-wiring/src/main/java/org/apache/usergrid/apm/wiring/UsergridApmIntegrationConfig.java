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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UsergridApmIntegrationConfig {

    private static final Log log = LogFactory.getLog(UsergridApmIntegrationConfig.class);
    private static Properties deploymentConfigProperties = null;
    private static UsergridApmIntegrationConfig apmConfig = null;

    private boolean apmEnabled = false;

    private String environment; // such as dev, test, production

    protected String apigeeFreeAPMServerUrl;

    private UsergridApmIntegrationConfig() {

    }

    public static UsergridApmIntegrationConfig getAPMConfig() {
        if (apmConfig != null)
            return apmConfig;
        apmConfig = new UsergridApmIntegrationConfig();
        deploymentConfigProperties = new Properties();
        try {
            deploymentConfigProperties.load(Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("apm-config.properties"));
            apmConfig.apmEnabled = Boolean.valueOf(deploymentConfigProperties
                    .getProperty("ApigeeAPMEnabled"));
            apmConfig.environment = deploymentConfigProperties
                    .getProperty("DeploymentEnvironment");
            String apmServer = deploymentConfigProperties
                    .getProperty("ApmRestServerUrl");
            if (!apmServer.endsWith("/"))
                apmServer = apmServer.concat("/");
            apmConfig.apigeeFreeAPMServerUrl = apmServer;
            log.info("ApigeeApmConfig  apmEnabled: " + apmConfig.apmEnabled
                    + " apmUrl:  " + apmConfig.apigeeFreeAPMServerUrl);

        } catch (FileNotFoundException e) {
            log.fatal("Could not find the properties file with APM Deployment info. Means APM is not enabled.");
            log.error(e);
        } catch (IOException e) {
            log.fatal("Could not find the properties file with APM Deployment info. Means APM is not enabled.");
            log.error(e);
        } catch (Exception e) {
            log.fatal("Problem reading properties file with APM Deployment info");
            log.error(e);
        }
        return apmConfig;
    }

    public String getApigeeFreeAPMServerUrl() {
        return apigeeFreeAPMServerUrl;
    }

    public String getEnvironment() {
        return environment;
    }

    public boolean isApmEnabled() {
        return apmEnabled;
    }
    
    public String getOrgSpecificAPMEndPoint(String orgName) {
    	return deploymentConfigProperties.getProperty(orgName);
    }

    public static void main(String[] args) {
        UsergridApmIntegrationConfig conf = UsergridApmIntegrationConfig.getAPMConfig();

        System.out.println(conf.getEnvironment());
        System.out.println(conf.getApigeeFreeAPMServerUrl());
        System.out.println(conf.getOrgSpecificAPMEndPoint("prabhat1"));
    }

}
