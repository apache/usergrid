<?php
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
