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
 * Push Notification tests
 *
 * @author Rod Simpson <rod@apigee.com>
 * @since 09-Mar-2013
 */

//--------------------------------------------------------------
// Push tests
//--------------------------------------------------------------
$testname = 'Create apple notifier';
$apple_name = 'name_'.time();
$environment = 'development';
$p12Certificate_path = "@pushtest_dev.p12";
$result =  $client->createNewNotifierApple($apple_name, $environment, $p12Certificate_path);
echo '<pre>'.$result->get_json().'</pre><br>';
if ($result->get_error()){
  $tester->error($testname);
} else {
	$tester->success($testname);
}

$testname = 'Create google notifier';
$google_name = 'name_'.time();
$apiKey = 'AIzaSyCIH_7WC0mOqBGMOXyQnFgrBpOePgHvQJM';
$result =  $client->createNewNotifierAndroid($google_name, $apiKey);
echo '<pre>'.$result->get_json().'</pre><br>';
if ($result->get_error()){
  $tester->error($testname);
} else {
	$tester->success($testname);
}

$testname = 'Create notification';
$notification = $client->createNotification();
$notification->set_notifier_name($apple_name);
$notification->set_message("Test Message");
$notification->set_delivery_time(time());
$notification->set_recipients_list(array(0=>'fred'));
$notification->set_recipient_type(USERS);

$result = $client->scheduleNotification($notification);
echo '<pre>'.$result->get_json().'</pre><br>';
if ($result->get_error()){
  $tester->error($testname);
} else {
	$tester->success($testname);
}

?>
