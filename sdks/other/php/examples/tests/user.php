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
 * User tests
 *
 * @author Rod Simpson <rod@apigee.com>
 * @since 09-Mar-2013
 */

//--------------------------------------------------------------
// User tests
//--------------------------------------------------------------
$testname = 'DELETE users/marty';
$endpoint = 'users/marty';
$query_string = array();
$result =  $client->delete($endpoint, $query_string);
$result_data = $result->get_data();
if (isset($result_data)){
  $tester->success($testname);
} else {
	$tester->error($testname);
}

$testname = 'Signup user';
//@han {create-user}
$marty =  $client->signup('marty', 'mysecurepassword','marty@timetravel.com', 'Marty McFly');
if ($marty) {
	//user created
} else {
	//there was an error
}
//@solo
if ($marty->get('username') == 'marty'){
  $tester->success($testname);
} else {
	$tester->error($testname);
}

$testname = 'Update user';
//@han {update-user}
$marty->set('state', 'California');
$marty->set('girlfriend', 'Jennifer');
$result = $marty->save();
//@solo
if ($result->get_error()) {
	$tester->error($testname);
} else {
	$tester->success($testname);
}

$testname = 'Log user in';
//@han {log-user-in}
if ($client->login('marty', 'mysecurepassword')) {
	//the login call will return an OAuth token, which is saved
	//in the client. Any calls made now will use the token.
	//once a user has logged in, their user object is stored
	//in the client and you can access it this way:
	$token = $client->get_oauth_token();
	$tester->success($testname);
} else {
	$tester->error($testname);
}
//@solo

$testname = 'Get logged in user';
$marty = $client->get_logged_in_user();
if (isset($marty) && $marty->get('username') == 'marty') {
	$tester->success($testname);
} else {
	$tester->error($testname);
}

$testname = 'Update user password';
//@han {update-user-password}
$marty->set('oldpassword', 'mysecurepassword');
$marty->set('newpassword', 'mynewsecurepassword');
$marty->save();
//@solo
$result_data = $result->get_data();
if (isset($result_data['action']) && $result_data['action'] == 'put') {
	$tester->success($testname);
} else {
	$tester->error($testname);
}

$testname = 'Log user out';
//@han {log-user-out}
$client->log_out();
if ($client->is_logged_in()) {
	//error - user is still logged in
	$tester->error($testname);
} else {
	//success - user was logged out
	$tester->success($testname);
}
//@solo

$testname = 'Log user in with new password';
if ($client->login('marty', 'mynewsecurepassword')){
	$tester->success($testname);
} else {
	$tester->error($testname);
}

$testname = 'Delete user';
//@han {destroy-user}
$result = $marty->destroy();
if ($result->get_error()) {
	//there was an error deleting the user
	$tester->error($testname);
} else {
	//success - user was deleted
	$tester->success($testname);
}
//@solo


?>
