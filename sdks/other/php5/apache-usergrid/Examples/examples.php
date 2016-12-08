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
        'manifestPath' => './src/Manifests', //leave as default or change to your own custom folder
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
        'auth_type' => 'application',
        /** The Grant Type to use
         *
         * This has to be set to one of the 2 grant types that Apache Usergrid
         * supports which at the moment is client_credentials or password but at
         * 2 level organization or application
         */
        'grant_type' => 'password',
        /**
         * if you want to manage your own auth flow by calling the token api and setting the token your self just set this to false
         * */
        'enable_oauth2_plugin' => true
    ]
];

/** Usergrid Facades Off */

//call the bootstrap class that will create the api client then get the usergrid instance from the bootstrapper
$bootstrapper = new UsergridBootstrapper($config);
$usergrid = $bootstrapper->createUsergrid();

/** Note: I'm using Users for the first lot of example's but you could do the same for all default collections eg: groups, devices, roles, notification etc*/

// All responses are model objects that subclass the baseCollection class so all collection methods are available eg: first(), map(), fetch(), hasValue(), hasKey() etc.

//find users with query
$user = $usergrid->users()->find(['ql' => 'select * where activated=true']);

//var_dump($user->entities->first());

//request that throw exception in this case 404 not found
try {

    $user2 = $usergrid->users()->findById(['uuid' => 'uuid-number']);
//    var_dump($user2->get('entities'));
} catch (Apache\Usergrid\Api\Exception\NotFoundException $e) {

//    var_dump($e->getResponse());
}
/**
 * There a errors for all Usergrid response errors and http errors so you can create a catch all error class or plug it into your fav php frameworks error handling.
 * Apache\Usergrid\Api\Exception\BadRequestException = http 400
 * Apache\Usergrid\Api\Exception\UnauthorizedException = http 401
 * Apache\Usergrid\Api\Exception\RequestFailedException = http 402
 * Apache\Usergrid\Api\Exception\NotFoundException = http 404
 * Apache\Usergrid\Api\Exception\ServerErrorException = http 500
 * Apache\Usergrid\Api\Exception\ServerErrorException = http 502
 * Apache\Usergrid\Api\Exception\ServerErrorException = http 503
 * Apache\Usergrid\Api\Exception\ServerErrorException = http 504
 */

// collection Iterators no need to keep calling request with a cursor to get all entities in a collection
// UserIterator(), DeviceIterator() GroupsIterator() , RolesIterator() etc.
$user_iterator = $usergrid->usersIterator();

foreach ($user_iterator as $iUser) {
    var_dump($iUser);
    var_dump("---------------------------------------------------------------------------------");
}

// create new user
$new_user = ['name' => 'jasonk', 'username' => 'JasonK', 'email' => 'jason@example.com', 'password' => 'some_password'];
//$created_user = $usergrid->users()->create($new_user);
//var_dump($created_user->entities);

//Update Users by name or uuid
$new_email = ['email' => 'jason@example', 'entity_name_or_uuid' => 'benn'];
$updated_user = $usergrid->users()->update($new_email);
//var_dump($updated_user->entities);

// delete a user
//$deleted_user = $usergrid->users()->delete(['entity_name_or_uuid' => 'benn']);
//var_dump($deleted_user);

//get custom collection
$custom_collection = $usergrid->application()->EntityGet(['collection' => 'shops']);
//var_dump($custom_collection->entities->get('name'));

//get custom collection with query
$custom_collection_query = $usergrid->application()->EntityGet([
    'collection' => 'shops',
    'ql' => "select * where country='aus'"
]);
//var_dump($custom_collection_query->get('entities'));

// Post custom collection as JSON data
$custom_entity = [
    'collection' => 'shops',
    'name' => 'shop_name',
    'adr' => ['street' => '1 main st', 'location' => 'sydney', 'post_code' => '2323'],
    'type' => 'pet_shop'
];
//$created_entity = $usergrid->application()->EntityJsonPost($custom_entity);
//var_dump($created_entity->entities);

// update custom Entity
$custom_entity_edit = [
    'collection' => 'shops',
    'entity_name_or_uuid' => '918a044a-618a-11e4-8c11-253e9c3723a9',
    ['adr' => ['street' => '3 main st', 'location' => 'act', 'post_code' => '3323']]
];
$edited_entity = $usergrid->application()->EntityPut($custom_entity_edit);


/** Usergrid Facades On */

//create a bootstrap instance and then call the static instance method on the Usergrid facade
$bootstrapper2 = new UsergridBootstrapper($config);
Usergrid::instance($bootstrapper2);


// find users with query
$fUser = Usergrid::users()->find(['ql' => 'select * where activated=true']);

$fUser_iterator = Usergrid::usersIterator();

foreach ($fUser_iterator as $iUser) {
    var_dump($iUser);
    var_dump("---------------------------------------------------------------------------------");
}

// create new user
$fNew_user = [
    'name' => 'jasonk',
    'username' => 'JasonK2',
    'email' => 'jaso2n@example.com',
    'password' => 'some_password'
];
$fCreated_user = Usergrid::users()->create($fNew_user);
//var_dump($fCreated_user->entities);

//Update Users by name or uuid
$fNew_email = ['email' => 'jason@example', 'entity_name_or_uuid' => 'benn'];
$fUpdated_user = Usergrid::users()->update($fNew_email);
//var_dump($fUpdated_user->entities);

// delete a user
$fDeleted_user = Usergrid::users()->delete(['entity_name_or_uuid' => 'benn']);
//var_dump($fDeleted_user->entities);

//get custom collection
$fCustom_collection = Usergrid::application()->EntityGet(['collection' => 'shops']);
//var_dump($custom_collection->get('entities'));

//get custom collection with query
$fCustom_collection_query = Usergrid::application()->EntityGet([
    'collection' => 'shops',
    'ql' => "select * where country='aus'"
]);
//var_dump($custom_collection_query->get('name'));

// Post custom collection as JSON data
$fCustom_entity = [
    'collection' => 'shops',
    'name' => 'shop_name3',
    'adr' => ['street' => '1 main st', 'location' => 'sydney', 'post_code' => '2323'],
    'type' => 'pet_shop'
];
$fCreated_entity = Usergrid::applictions()->EntityJsonPost($custom_entity);
//var_dump($fCreated_entity->entities);

// update entity
$fCustom_entity_edit = [
    'collection' => 'shops',
    'entity_name_or_uuid' => 'shop_name2',
    ['adr' => ['street' => '3 main st', 'location' => 'act', 'post_code' => '3323']]
];
$fEdited_entity = Usergrid::applications()->EntityPut($fCustom_entity_edit);
//var_dump($fEdited_entity->entities);


/** Relationships */
$related_data = [
    'collection' => 'required',
    'entity_id' => 'required',
    'relationship' => 'required',
    'ql' => 'optional'
];
$get_relationship = Usergrid::application()->GetRelationship($related_data);


$create_relationship_data = [
    'collection' => 'required',
    'first_entity_id' => 'required',
    'relationship' => 'required',
    'second_entity_id' => 'required'
];
$new_relationship = Usergrid::application()->CreateRelationship($create_relationship_data);


/** Groups  */

//add user to group
$fAdd_user_to_group_data = [
    'entity_name_or_uuid' => 'group_name',
    'user_name_or_uuid' => 'username'
];
$fAdded_user_to_group = Usergrid::groups()->addUser($fAdd_user_to_group_data);

//delete user from group
$fDeleted_user_from_group = Usergrid::groups()->deleteUser($fAdd_user_to_group_data);