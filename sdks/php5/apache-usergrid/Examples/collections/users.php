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


// call user by page size 20
$users_paged = Usergrid::users()->all();
var_dump(get_class($users_paged->entities));

//// get user 50 page size
$users_paged_50 = Usergrid::users()->all(['limit' => 50]);
var_dump($users_paged_50->entities);

// get all users
$all_users = Usergrid::usersIterator();
foreach ($all_users as $user) {
//    var_dump($user['uuid']); // as array
}

// find user by query
$find_user_by_query = Usergrid::users()->find(['ql' => "select * where email='jason@apps4u.com.au'"]);
var_dump($find_user_by_query->entities->fetch('uuid'));

$find_user_by_uuid = Usergrid::users()->findById(['uuid' => $find_user_by_query->entities->fetch('uuid')->first()]);
var_dump($find_user_by_uuid->entities);


// AS all results as PHP Collections and the entities property is always returned as a PHP Collection you can fetch nested records
$user_addr = Usergrid::users()->findById(['uuid' => 'Jason']);
echo $user_addr->entities->fetch('adr.addr1');
//or
echo $user_addr->entities->fetch('adr.city');

// get users device URL -- nested fetch on php collection
$users_nested = Usergrid::users()->all();
var_dump($users_nested->entities->fetch('metadata.collections.devices')->first());

// The response that is returned is a PHP collection that has a Zero indexed $item property.
// but as its a collection class it has some methods that can help you find what you need and one
// of my fav feature is changing the Zero indexed collection to be indexed by the entity uuid or name or any other property.
$users_by = Usergrid::users()->all();

$users_by_uuid = $users_by->entities->keyBy('uuid');
var_dump($users_by_uuid->get('add uuid of user'));

$users_by_name = $users_by->entities->keyBy('username');
var_dump($users_by_name->get('jasonk'));

$users_by_email = $users_by->entities->keyBy('email');
var_dump($users_by_email->get('jasonk@apps4u.com.au'));

// sort by key
$sorted_by_email = $users_by->sortBy('username');
var_dump($sorted_by_email);


// add user to group
//$user_to_group = Usergrid::groups()->addUser(['entity_name_or_uuid' => 'group_name_or_uuid', 'user_name_or_uuid' => 'user name or uuid']);

//$user_remove_group = Usergrid::groups()->removeUser(['entity_name_or_uuid' => 'group_name_or_uuid', 'user_name_or_uuid' => 'user name or uuid']);