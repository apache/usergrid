package com.apigee.apm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ApigeeApmConfig {

    private static final Log log = LogFactory.getLog(ApigeeApmConfig.class);
    private static Properties deploymentConfigProperties = null;
    private static ApigeeApmConfig apmConfig = null;

    private boolean apmEnabled = false;

    private String environment; // such as dev, test, production

    protected String apigeeFreeAPMServerUrl;

    private ApigeeApmConfig() {

    }

    public static ApigeeApmConfig getAPMConfig() {
        if (apmConfig != null)
            return apmConfig;
        apmConfig = new ApigeeApmConfig();
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
        ApigeeApmConfig conf = ApigeeApmConfig.getAPMConfig();

        System.out.println(conf.getEnvironment());
        System.out.println(conf.getApigeeFreeAPMServerUrl());
        System.out.println(conf.getOrgSpecificAPMEndPoint("prabhat1"));
    }

}
