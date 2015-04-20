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

use Apache\Usergrid\Native\UsergridBootstrapper;

$config = [

    'usergrid' => [

        'url' => 'https://api.usergrid.com',
        'version' => '1.0.1',
        'orgName' => null,
        'appName' => null,
        'manifestPath' => null,
        //its better not to set the real values here if using laravel set them in a .env file or
        // if your not using Laravel set them as environment variable and include them here using $_ENV global.
        // so that way you can be sure not to commit privates ID to a public repo
        'clientId' => null,
        'clientSecret' => null,
        'username' => null,
        'password' => null,
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

$boot = new UsergridBootstrapper($config);
$usergrid = $boot->createUsergrid();

