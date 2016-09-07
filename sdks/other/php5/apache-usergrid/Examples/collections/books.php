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

use Apache\Usergrid\Api\Filters\Date;
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


foreach ($books_data as $book) {
    Usergrid::application()->EntityJsonPost($book);
}


$books = Usergrid::application()->EntityGet(['collection' => 'books', 'limit' => 25]);


// get result count just call the Illuminate\Support\Collection  count method
var_dump($books->entities->count());


// As responses are model object you can treat them like a assoc arrays
var_dump($books->entities[0]['uuid']);

// if you like a more object orientated way then use the Collection Class methods

// get all uuid
var_dump($books->entities->fetch('uuid'));

//get first uuid
var_dump($books->entities->fetch('uuid')->first());

// get first item in collection -- this is the first item in my response php collection not the Usergrid Collection (table).
var_dump($books->entities->first());

// get last item in collection -- this is the last item in my response php collection not the Usergrid Collection (table).
var_dump($books->entities->last());

// convert created date to string
var_dump(Date::convert($books->entities->fetch('created')->first()));

// Illuminate\Support\Collection class support all advanced collection methods

// pop last item off collection
$book = $books->entities->pop();

// Converting methods
$json_ = $books->entities->toJson();

//Convert the object into something JSON serializable.
$books->entities->jsonSerialize();

// Get an iterator for the items in collection
$iterator = $books->entities->getIterator();

//Get a CachingIterator instance
$caching_iterator = $books->entities->getCachingIterator();

/// Here are some more Methods that you can call on your responses .. To get the most out of this SDK please look at the Illuminate\Support\Collection class
/// which is the supper class of Apache/Usergrid/Api/Models/BaseCollection class
/**
 * $books->unique();
 * $books->transform();
 * $books->take();
 * $books->splice();
 * $books->sum($callback );
 * $books->values();
 * $books->sortByDesc($callback);
 * $books->sortBy();
 * $books->shuffle();
 * $books->chunk();
 */

