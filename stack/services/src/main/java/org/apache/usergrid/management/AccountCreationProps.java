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


import java.util.Properties;


public interface AccountCreationProps {
    String PROPERTIES_MAILER_EMAIL = "usergrid.management.mailer";

    String PROPERTIES_EMAIL_SYSADMIN_ORGANIZATION_ACTIVATED =
            "usergrid.management.email.sysadmin-organization-activated";
    String PROPERTIES_EMAIL_SYSADMIN_ADMIN_ACTIVATED =
            "usergrid.management.email.sysadmin-admin-activated";
    String PROPERTIES_EMAIL_ADMIN_PASSWORD_RESET = "usergrid.management.email.admin-password-reset";
    String PROPERTIES_EMAIL_SYSADMIN_ORGANIZATION_ACTIVATION =
            "usergrid.management.email.sysadmin-organization-activation";
    String PROPERTIES_EMAIL_ORGANIZATION_CONFIRMATION =
            "usergrid.management.email.organization-confirmation";
    String PROPERTIES_EMAIL_ORGANIZATION_CONFIRMED_AWAITING_ACTIVATION =
            "usergrid.management.email.organization-activation-pending";
    String PROPERTIES_EMAIL_ORGANIZATION_ACTIVATED =
            "usergrid.management.email.organization-activated";
    String PROPERTIES_EMAIL_SYSADMIN_ADMIN_ACTIVATION =
            "usergrid.management.email.sysadmin-admin-activation";
    String PROPERTIES_EMAIL_ADMIN_CONFIRMATION = "usergrid.management.email.admin-confirmation";
    String PROPERTIES_EMAIL_ADMIN_CONFIRMED_AWAITING_ACTIVATION =
            "usergrid.management.email.admin-confirmed";
    String PROPERTIES_EMAIL_ADMIN_ACTIVATED = "usergrid.management.email.admin-activated";
    String PROPERTIES_EMAIL_ADMIN_INVITED = "usergrid.management.email.admin-invited";
    String PROPERTIES_EMAIL_ADMIN_USER_ACTIVATION =
            "usergrid.management.email.admin-user-activation";
    String PROPERTIES_EMAIL_ADMIN_USER_ACTIVATED = "usergrid.management.email.admin-user-activated";
    String PROPERTIES_EMAIL_USER_CONFIRMATION = "usergrid.management.email.user-confirmation";
    String PROPERTIES_EMAIL_USER_CONFIRMED_AWAITING_ACTIVATION =
            "usergrid.management.email.user-confirmed";
    String PROPERTIES_EMAIL_USER_ACTIVATED = "usergrid.management.email.user-activated";
    String PROPERTIES_EMAIL_USER_PASSWORD_RESET = "usergrid.management.email.user-password-reset";
    String PROPERTIES_EMAIL_USER_PIN_REQUEST = "usergrid.management.email.user-pin";
    String PROPERTIES_EMAIL_FOOTER = "usergrid.management.email.footer";

    String PROPERTIES_USER_ACTIVATION_URL = "usergrid.user.activation.url";
    String PROPERTIES_USER_CONFIRMATION_URL = "usergrid.user.confirmation.url";
    String PROPERTIES_USER_RESETPW_URL = "usergrid.user.resetpw.url";
    String PROPERTIES_ADMIN_ACTIVATION_URL = "usergrid.admin.activation.url";
    String PROPERTIES_ADMIN_CONFIRMATION_URL = "usergrid.admin.confirmation.url";
    String PROPERTIES_ORGANIZATION_ACTIVATION_URL = "usergrid.organization.activation.url";
    String PROPERTIES_ADMIN_RESETPW_URL = "usergrid.admin.resetpw.url";

    String PROPERTIES_USERGRID_SYSADMIN_LOGIN_FETCH_ORGS = "usergrid.sysadmin.login.fetch_orgs";

    String PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION =
            "usergrid.management.admin_users_require_confirmation";
    String PROPERTIES_ORGANIZATIONS_REQUIRE_CONFIRMATION =
            "usergrid.management.organizations_require_confirmation";
    String PROPERTIES_NOTIFY_ADMIN_OF_ACTIVATION = "usergrid.management.notify_admin_of_activation";

    String PROPERTIES_SYSADMIN_APPROVES_ADMIN_USERS =
            "usergrid.sysadmin.approve.users";
    String PROPERTIES_SYSADMIN_APPROVES_ORGANIZATIONS =
            "usergrid.sysadmin.approve.organizations";
    String PROPERTIES_NOTIFY_SYSADMIN_OF_NEW_ORGANIZATIONS =
            "usergrid.management.notify_sysadmin_of_new_organizations";
    String PROPERTIES_NOTIFY_SYSADMIN_OF_NEW_ADMIN_USERS =
            "usergrid.management.notify_sysadmin_of_new_admin_users";

    String PROPERTIES_SYSADMIN_LOGIN_PASSWORD = "usergrid.sysadmin.login.password";
    String PROPERTIES_SYSADMIN_LOGIN_EMAIL = "usergrid.sysadmin.login.email";
    String PROPERTIES_SYSADMIN_LOGIN_NAME = "usergrid.sysadmin.login.name";
    String PROPERTIES_SYSADMIN_LOGIN_ALLOWED = "usergrid.sysadmin.login.allowed";

    String PROPERTIES_ADMIN_SYSADMIN_EMAIL = "usergrid.admin.sysadmin.email";
    String PROPERTIES_ORG_SYSADMIN_EMAIL = "usergrid.org.sysadmin.email";
    String PROPERTIES_DEFAULT_SYSADMIN_EMAIL = "usergrid.sysadmin.email";

    String PROPERTIES_TEST_ACCOUNT_ADMIN_USER_PASSWORD =
            "usergrid.test-account.admin-user.password";
    String PROPERTIES_TEST_ACCOUNT_ADMIN_USER_EMAIL = "usergrid.test-account.admin-user.email";
    String PROPERTIES_TEST_ACCOUNT_ADMIN_USER_NAME = "usergrid.test-account.admin-user.name";
    String PROPERTIES_TEST_ACCOUNT_ADMIN_USER_USERNAME =
            "usergrid.test-account.admin-user.username";
    String PROPERTIES_TEST_ACCOUNT_ORGANIZATION = "usergrid.test-account.organization";
    String PROPERTIES_TEST_ACCOUNT_APP = "usergrid.test-account.app";
    String PROPERTIES_SETUP_TEST_ACCOUNT = "usergrid.setup-test-account";

    String PROPERTIES_USERGRID_BINARY_UPLOADER="usergrid.binary.uploader";

    boolean newOrganizationsNeedSysAdminApproval();

    boolean newAdminUsersNeedSysAdminApproval();

    boolean newAdminUsersRequireConfirmation();

    boolean newOrganizationsRequireConfirmation();

    boolean notifySysAdminOfNewAdminUsers();

    boolean notifySysAdminOfNewOrganizations();

    boolean notifyAdminOfActivation();

    /**
     * Retrieves the maximum number of organizations to show when the admin logs in.
     * Default is 10
     */
    int getMaxOrganizationsForSuperUserLogin();

    String getProperty( String name );

    String getProperty( String name, String defaultValue );

    boolean isProperty( String name );

    int intProperty( String name, String defaultValue );

    void setProperty( String name, String value );

    Properties getMailProperties();

    SuperUser getSuperUser();

    interface SuperUser{
        boolean isEnabled();
        String getUsername();
        String getEmail();
        String getPassword();
    }
}
