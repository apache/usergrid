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

/** The PHP SDK returns all responses as Illuminate\Support\Collection subclasses so the word collection below is php collection class not usergrid collection */

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


/**
 * Attributes are a new feature that make getting related models easy as accessing a property on a model object
 * The way relationships work before this feature it required you to call the getRelationship method on the application
 * service which is still available, so for example you would first get the user then once you had the user you create a
 * array that contained the uuid of the user the related collection name and the relationship name then call the api passing in the
 * array but for the default relationships we already have the data that we need to make the api call so that is what
 * I've done.
 * <pre>
 * <?php
 *  $user = Usergrid::users()->findById(['uuid' => '1234abcd']) ;
 *  $device = $user->device;
 * ?>
 * </pre>
 *  That's all you need to do to get a device for the user this only works when you have one user in your user collection
 *  if you call this with more then one user in your user collection it will return the device for the first user in the
 *  collection.
 *
 */

$user = Usergrid::users()->findById(['uuid' => '1234abcd']);

echo "device" . PHP_EOL;
var_dump($user->device);
var_dump('=================================================================');

echo "roles" . PHP_EOL;
var_dump($user->roles);
var_dump('=================================================================');

echo "groups" . PHP_EOL;
var_dump($user->groups);
var_dump('=================================================================');

echo "connections" . PHP_EOL;
var_dump($user->connections);
var_dump('=================================================================');


var_dump('=================================================================');
echo "GROUPS" . PHP_EOL;
var_dump('=================================================================');


$group = Usergrid::groups()->findById(['uuid' => '121212']);


echo "roles" . PHP_EOL;
var_dump($group->roles);
var_dump('=================================================================');

echo "groups" . PHP_EOL;
var_dump($group->users);
var_dump('=================================================================');

echo "connections" . PHP_EOL;
var_dump($group->connections);
var_dump('=================================================================');