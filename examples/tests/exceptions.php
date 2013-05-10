<?php
/**
 * @file
 * exceptions tests
 *
 * @author Rod Simpson <rod@apigee.com>
 * @since 09-Mar-2013
 */

//--------------------------------------------------------------
// Exception Tests - NOT DONE YET!!!
//--------------------------------------------------------------
$testname = 'DELETE users/fred';
//@han {generic-delete}
$endpoint = 'users/idontexist';
$query_string = array();
try {
	$result =  $client->delete($endpoint, $query_string);
} catch (Exception $e) {
	//entity didn't exist on the server, so UG_404_NotFound is thrown
	$tester->success($testname);
}

?>

?>
