<?php
/**
 * @file
 * main entry point for running tests
 *
 * @author Rod Simpson <rod@apigee.com>
 * @since 09-Mar-2013
 */

//@han {include-sdk}
include '../autoloader.inc.php';
usergrid_autoload('Apigee\\Usergrid\\Client');
//@solo

//@han {create-new-client}
$client = new Apigee\Usergrid\Client('1hotrod','sandbox');
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