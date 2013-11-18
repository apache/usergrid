<?php
/**
 * @file
 * Generic tests
 *
 * @author Rod Simpson <rod@apigee.com>
 * @since 09-Mar-2013
 */

//--------------------------------------------------------------
// Generic Tests
//--------------------------------------------------------------
$testname = 'DELETE users/fred';
//@han {generic-delete}
$endpoint = 'users/fred';
$query_string = array();
$result =  $client->delete($endpoint, $query_string);
if ($result->get_error()){
	//error - there was a problem deleting the entity
} else {
	//success - entity deleted
}
//@solo
$data = $result->get_data();
if ($data['action'] != 'delete'){
	//error - there was a problem deleting the entity
  $tester->error($testname);
} else {
	//success - entity deleted
	$tester->success($testname);
}

if (!$result->get_error()) {
  $tester->success($testname);
} else {
	$tester->error($testname);
}

$testname = 'POST users / fred';
//@han {generic-post}
$endpoint = 'users';
$query_string = array();
$body = array('username'=>'fred');
$result = $client->post($endpoint, $query_string, $body);
if ($result->get_error()){
	//error - there was a problem creating the entity
  $tester->error($testname);
} else {
	//success - entity created
	$tester->success($testname);
}
//@solo
$data = $result->get_data();
$json = $result->get_json();
//echo '<pre>'.$json.'</pre><br>';
if (isset($data['entities'][0]['username']) && $data['entities'][0]['username'] == 'fred'){
  $tester->success($testname);
} else {
	$tester->error($testname);
}


$testname = 'PUT users/fred';
//@han {generic-put}
$endpoint = 'users/fred';
$query_string = array();
$body = array('dog'=>'dino');
$result = $client->put($endpoint, $query_string, $body);
if ($result->get_error()){
	//error - there was a problem updating the entity
  $tester->error($testname);
} else {
	//success - entity updated
	$tester->success($testname);
}
//@solo
$data = $result->get_data();
if (isset($data['entities'][0]['dog']) && $data['entities'][0]['dog'] == 'dino'){
  $tester->success($testname);
} else {
	$tester->error($testname);
}

$testname = 'GET users/fred';
//@han {generic-get}
$endpoint = 'users/fred';
$query_string = array();
$result = $client->get($endpoint, $query_string);
if ($result->get_error()){
	//error - there was a problem getting the entity
  $tester->error($testname);
} else {
	//success - entity retrieved
	$tester->success($testname);
}
//@solo
$data = $result->get_data();
if (isset($data['path']) && $data['path'] == '/users'){
  $tester->success($testname);
} else {
	$tester->error($testname);
}
?>
