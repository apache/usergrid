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

include('data.php');

use Apache\Usergrid\Native\Facades\Usergrid;
use Apache\Usergrid\Native\UsergridBootstrapper;


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


// to user
$to_user = Usergrid::notifications()->toUser();


//to users
$to_users = Usergrid::notifications()->toUsers();


//to group
$to_group = Usergrid::notifications()->toGroup();


// to groups
$to_groups = Usergrid::notifications()->toGroups();


// to device
$to_device = Usergrid::notifications()->toDevice();


// to devices
$to_devices = Usergrid::notifications()->toDevices();
