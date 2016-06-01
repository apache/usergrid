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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.inject.Injector;
import org.apache.http.HttpStatus;
import org.apache.usergrid.management.*;
import org.apache.usergrid.utils.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.management.cassandra.ManagementServiceImpl;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.entities.Application;

import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_ADMIN_INVITED;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_EMAIL_USER_CONFIRMATION;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class UsergridApmIntegrationManagementServiceImpl extends ManagementServiceImpl {

    public UsergridApmIntegrationManagementServiceImpl(Injector injector) {
        super(injector);
    }

    private static final String USERGRID_USER_LOGIN_URL = "usergrid.user.login.url";

    private static final Logger logger = LoggerFactory
            .getLogger(UsergridApmIntegrationManagementServiceImpl.class);

    @Override
    public ApplicationInfo createApplication(UUID organizationId,
            String applicationName, Map<String, Object> properties)
            throws Exception {
        // do the usual app creation process
        ApplicationInfo appInfo = super.createApplication(organizationId,
                applicationName, null, properties, false);
        if (appInfo != null)
            logger.info("successfully created app 	with " + appInfo.getId());
        else
            throw new Exception(
                    "Problem creating a new app so not going to add APM related stuff ");
        // following is a no-op if APM is not enabled for this deployment
        registerAppWithAPM(getOrganizationByUuid(organizationId), appInfo);
        return appInfo;
    }

    @Override
    public void sendAppUserConfirmationEmail( UUID applicationId, User user ) throws Exception {
        String token = getConfirmationTokenForAppUser( applicationId, user.getUuid() );
        OrganizationConfig orgConfig = getOrganizationConfigForApplication(applicationId);
        String confirmationPropertyUrl = orgConfig.getFullUrlTemplate(OrganizationConfigProps.WorkflowUrl.USER_CONFIRMATION_URL);
        String confirmation_url =
                buildUserAppUrl( applicationId, confirmationPropertyUrl, user, token);

        OrganizationInfo organization = getOrganizationByUuid( user.getUuid() );

        MapUtils.HashMapBuilder<String, String> builder =
                hashMap("confirmation_url", confirmation_url)
                        .map("name", user.getName())
                        .map("orgname", organization.getName());

        sendAppUserEmail( user, "User Account Confirmation: " + user.getEmail(),
                emailMsg(builder, PROPERTIES_EMAIL_USER_CONFIRMATION ) );
    }

    @Override
    public void sendAdminUserInvitedEmail( UserInfo user, OrganizationInfo organization ) throws Exception {
        MapUtils.HashMapBuilder<String, String> builder =
                hashMap("organization_name", organization.getName())
                .map("link", properties.getProperty( USERGRID_USER_LOGIN_URL, "https://accounts.apigee.com/accounts/sign_in" ));


        sendAdminUserEmail( user, "User Invited To Organization",
                emailMsg(builder, PROPERTIES_EMAIL_ADMIN_INVITED ) );
    }


    @Override
    public Object registerAppWithAPM(OrganizationInfo orgInfo,
            ApplicationInfo appInfo) throws Exception {
        String jsonApmAppConfig = null;
        if (UsergridApmIntegrationConfig.getAPMConfig().isApmEnabled()) {
            logger.info("APM functionality is enabled so going to register new app with Apigee APM");
            UsergridApmIntegrationAppDetailsForAPM appDetails = new UsergridApmIntegrationAppDetailsForAPM();
            appDetails.setOrgUUID(orgInfo.getUuid());
            appDetails.setOrgName(orgInfo.getName());
            appDetails.setAppUUID(appInfo.getId());
            appDetails.setAppName(getJustAppName(appInfo.getName()));
            appDetails.setCreatedDate(new Date());
            // mAX does not have concept of multiple app owners yet so will pick
            // one.
            appDetails.setAppAdminEmail(getApmAdminEmailAddressForApp(orgInfo
                    .getUuid()));
            logger.info("Admin email address " + appDetails.getAppAdminEmail());
            // Make a call to Mobile Analytics (APM) to create App in its system
            // and get the JSON representation of App. If there are any errors,
            // we simply log it and proceed.

            try {
                jsonApmAppConfig = registerAppWithApigeeAPM(appDetails);
            } catch (Exception apmException) {
                logger.error("Problem registering app with APM for org: "
                        + appDetails.getOrgName() + " appName: "
                        + appDetails.getAppName());
                jsonApmAppConfig = null;
                // TODO: Should send an email to admins
            }
            if (jsonApmAppConfig != null) {
                EntityManager em = emf.getEntityManager(appInfo.getId());
                em.setProperty(
                        new SimpleEntityRef(Application.ENTITY_TYPE, appInfo
                                .getId()),
                        UsergridApmIntegrationMobileAPMConstants.APIGEE_MOBILE_APM_CONFIG_JSON_KEY,
                        jsonApmAppConfig);
                Application appEntity = em.getApplication();
                // appEntity.setProperty("apigeeMobileConfig",
                // "some json value");
                // em.updateApplication(appEntity);
                logger.info("APM stuff in Application Entity "
                        + appEntity
                                .getProperty(UsergridApmIntegrationMobileAPMConstants.APIGEE_MOBILE_APM_CONFIG_JSON_KEY));
            }
        } else {
            logger.info("APM aka mAX functionality is NOT enabled in this deployment");
        }
        return jsonApmAppConfig;
    }

    private String registerAppWithApigeeAPM(UsergridApmIntegrationAppDetailsForAPM appDetails)
            throws Exception {
        UsergridApmIntegrationConfig apmConfig = UsergridApmIntegrationConfig.getAPMConfig();

        // example url:
        // http://apigeeapmdev.elasticbeanstalk.com/org/app/apm/appConfig
        // we first see if this org has dedicated APM provisioned. If it
        //does not then we default to free system
        String apmServerUrl = null;
        apmServerUrl = apmConfig.getOrgSpecificAPMEndPoint(appDetails.getOrgName());
        if (apmServerUrl == null)
        	apmServerUrl = apmConfig.getApigeeFreeAPMServerUrl();

        String apigeeAPMAppCreationUrl = apmServerUrl
                + appDetails.getOrgName() + "/" + appDetails.getAppName()
                + "/apm/appConfig";

        logger.info("going to do a POST request to url "
                + apigeeAPMAppCreationUrl);

        Client client = Client.create();
        WebResource webResource = client.resource(apigeeAPMAppCreationUrl);

        String jsonPayload = new ObjectMapper().writeValueAsString(appDetails);
        logger.info("JSON request to server for create app" + jsonPayload);

        ClientResponse response = webResource.type("application/json").post(
                ClientResponse.class, jsonPayload);

        if (response.getStatus() != HttpStatus.SC_OK) {
            // TODO: Should send an email to sysadmin
            logger.error("Problem registering app with APM ."
                    + response.getEntity(String.class));
            throw new RuntimeException(
                    "Failed to register app with Apigee APM : HTTP error code : "
                            + response.getStatus());
        }

        String output = response.getEntity(String.class);
        logger.info("Response string for create app request from APM .... "
                + output);

        // validate that response has APM application id
        if (output != null && output.contains("instaOpsApplicationId"))
            return output;
        else
            return null;

    }

    public String getApmAdminEmailAddressForApp(UUID orgUUID) {
        List<UserInfo> adminUsers = null;
        try {
            adminUsers = getAdminUsersForOrganization(orgUUID);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (adminUsers != null && adminUsers.size() != 0)
            return adminUsers.get(0).getEmail(); // pick one since mAX does not
                                                 // have multiple app owner
                                                 // concepts. This email is used
                                                 // to send crash notification
                                                 // on APM.
        else
            return UsergridApmIntegrationMobileAPMConstants.APIGEE_APM_ADMIN_DEFAULT_EMAIL_ADDRESS;

    }

    /**
     * 
     * @param orgAppNameWithSlash
     *            It's of the form orgName/appName . Only one slash is expected.
     * @return
     */
    private String getJustAppName(String orgAppNameWithSlash) {
        int slashLocation = orgAppNameWithSlash.indexOf('/');
        return orgAppNameWithSlash.substring(slashLocation + 1);
    }

    /**
     * need to figure how to autowire this with properties file given that
     * superclass already has one protected ApigeeApmConfigProperties
     * apmProperties;
     * 
     * 
     * public ApigeeApmConfigProperties getApmProperties() { return
     * apmProperties; }
     * 
     * @Autowired public void setApmProperties(Properties apmProperties) {
     *            logger.info ("setting amp properties"); this.apmProperties =
     *            new ApigeeApmConfigPropertiesImpl(apmProperties); }
     */

}
