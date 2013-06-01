<?php
/**
 * @file
 * Client Auth tests
 *
 * @author Rod Simpson <rod@apigee.com>
 * @since 01-Apr-2013
 */

//--------------------------------------------------------------
// Client Auth
//--------------------------------------------------------------
//@han {client-auth}
$testname = 'Use Client Auth type - ';
$client->set_auth_type(AUTH_CLIENT_ID);
$client->set_client_id('YXA6Us6Rg0b0EeKuRALoGsWhew');
$client->set_client_secret('YXA6OYsTy6ENwwnbvjtvsbLpjw5mF40');
//@solo

//--------------------------------------------------------------
// Generic Client Auth Tests
//--------------------------------------------------------------
$testname = 'DELETE users/fred';

$endpoint = 'users/fred';
$query_string = array();
$result =  $client->delete($endpoint, $query_string);
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
$endpoint = 'users';
$query_string = array();
$body = array('username'=>'fred');
$result = $client->post($endpoint, $query_string, $body);
$data = $result->get_data();
if (isset($data['entities'][0]['username']) && $data['entities'][0]['username'] == 'fred'){
  $tester->success($testname);
} else {
  $tester->error($testname);
}


$testname = 'PUT users/fred';
$endpoint = 'users/fred';
$query_string = array();
$body = array('dog'=>'dino');
$result = $client->put($endpoint, $query_string, $body);
$data = $result->get_data();
if (isset($data['entities'][0]['dog']) && $data['entities'][0]['dog'] == 'dino'){
  $tester->success($testname);
} else {
  $tester->error($testname);
}

$testname = 'GET users/fred';
$endpoint = 'users/fred';
$query_string = array();
$result = $client->get($endpoint, $query_string);
$data = $result->get_data();
if (isset($data['path']) && $data['path'] == '/users'){
  $tester->success($testname);
} else {
  $tester->error($testname);
}


$client->set_auth_type(AUTH_APP_USER);
$client->set_client_id(null);
$client->set_client_secret(null);
?>
