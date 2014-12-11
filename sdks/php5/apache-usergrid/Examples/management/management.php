<?php
/**
 * Copyright 2010-2014 baas-platform.com, Pty Ltd. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
include('vendor/autoload.php');


use Apache\Usergrid\Native\Facades\Usergrid;
use Apache\Usergrid\Native\UsergridBootstrapper;

/**
 * When working with the management api any calls that require a application to target will use the default app name set in the config but some times you may want to
 * make a api call to a different application which is possible when the url requires a application name it taken from the config but if you pass in a different application
 * name in the method arguments it will override the default application name just for that api call so If I wanted to add a user to two application I could make the same call
 * twice but pass in a application name only for the 2nd call.
 */

/** Source your config from file I'm using array here just for ease of use.
 * When using Laravel Framework publish the package config file when using with
 * other modern PHP frameworks just use their default config system .
 */
$config = [
    'usergrid' => [
        'url' => 'https://api.usergrid.com',
        'version' => '1.0.1', // set manifest version
        'orgName' => '',
        'appName' => '',
        'manifestPath' => null, //leave as default or change to your own custom folder
        'clientId' => '',
        'clientSecret' => '',
        'username' => '',
        'password' => '',
        /**
         * The Auth Type setting is the Oauth 2 end point you want to get the OAuth 2
         * Token from.  You have two options here one is 'application' the other is 'organization'
         *
         *  organization will get the the token from http://example.com/management using  client_credentials or password grant type
         *  application will get the token from http://example.com/managment/org_name/app_name using client_credentials or password grant type
         */
        'auth_type' => 'organization',
        /** The Grant Type to use
         *
         * This has to be set to one of the 2 grant types that Apache Usergrid
         * supports which at the moment is client_credentials or password but at
         * 2 level organization or application
         */
        'grant_type' => 'client_credentials',
        /**
         * if you want to manage your own auth flow by calling the token api and setting the token your self just set this to false
         * */
        'enable_oauth2_plugin' => true
    ]
];

$bootstrapper = new UsergridBootstrapper($config);
Usergrid::instance($bootstrapper);


// Get organization activity
$activity_feed = Usergrid::management()->OrgFeedGet();

// get org details
$organization_details = Usergrid::management()->OrgGet();

//get organizations application
$organization_applications = Usergrid::management()->OrgAppsGet();

//create application
$app = ['name' => 'app name -- required'];
$new_application = Usergrid::management()->OrgAppsJsonPost($app);

// delete application
$deleted_application = Usergrid::management()->OrgAppDelete($app);

//get irg admin users
$organization_admin_users = Usergrid::management()->OrgUsersGet();

/** There are many more api calls just look at the management manifest file to get the method name's and arguments to pass .
 * The management manifest file is a copy of the swagger file for usergrid so you can also run the swagger UI tool on your usergrid install as well.
 */