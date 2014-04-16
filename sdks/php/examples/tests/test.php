#!/usr/bin/env php
<?php
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @file
 * main entry point for running tests
 *
 * @author Rod Simpson <rod@apigee.com>
 * @since 09-Mar-2013
 */

//@han {include-sdk}
include '../autoloader.inc.php';
usergrid_autoload('Apache\\Usergrid\\Client');
//@solo

//@han {create-new-client}
$client = new Apache\Usergrid\Client('1hotrod','sandbox');
//@solo

include 'Tester.php';

$tester = new Tester();

include 'generic.php';
include 'entity.php';
include 'collection.php';
include 'user.php';
include 'client_auth.php';
include 'push.php';



//--------------------------------------------------------------
// Summary
//--------------------------------------------------------------
$tester->printSummary();

?>
