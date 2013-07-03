<?php
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
$environment = 'Development';
$p12Certificate_path = "@certificate.p12";
$result =  $client->createNewNotifierApple($apple_name, $environment, $p12Certificate_path);
if ($result->get_error()){
  $tester->error($testname);
} else {
	$tester->success($testname);
}

$testname = 'Create google notifier';
$google_name = 'name_'.time();
$apiKey = 'AIzaSyCIH_7WC0mOqBGMOXyQnFgrBpOePgHvQJM';
$result =  $client->createNewNotifierAndroid($google_name, $apiKey);
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
if ($result->get_error()){
  $tester->error($testname);
} else {
	$tester->success($testname);
}

?>
