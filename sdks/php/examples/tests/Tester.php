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
 * Tester - a simple class for logging during a test run
 *
 * @author Rod Simpson <rod@apigee.com>
 * @since 09-Mar-2013
 */

class Tester {

  public $error_count = 0;
	public $success_count = 0;

	//logging
	public $log_success = true;
	public $log_error = true;
	public $log_notice = true;

  public function __construct() {

  }

	//logging functions
	public function success($message){
		$this->success_count++;
		if ($this->log_success) {
			echo('SUCCESS: ' . $message . '<br>');
		}
	}

	public function error($message){
		$this->error_count++;
		if ($this->log_error) {
			echo('ERROR: ' . $message . '<br>');
		}
	}

	public function notice($message){
		if ($this->log_notice) {
			echo('NOTICE: ' . $message . '<br>');
		}
	}

	function printSummary(){
		echo '<br><br><br>';
		echo '----------       Summary       ----------- <br>';
		if ($this->error_count > 0) {
			echo 'ERROR: BUILD NOT STABLE <br>';
		} else {
			echo 'OK: BUILD STABLE <br>';
		}
		echo 'Error Count: ' . $this->error_count . ' <br>';
		echo 'Success Count: ' . $this->success_count . ' <br>';
		echo '---------- Thanks for playing! ----------- <br>';
	}

}


?>
