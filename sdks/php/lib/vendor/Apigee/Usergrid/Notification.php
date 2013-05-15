<?php
/**
 * @file
 * Notification - a data structure to hold all notification-related parameters
 *
 * @author Rod Simpson <rod@apigee.com>
 * @since 09-Mar-2013
 */

namespace Apigee\Usergrid;

define('DEVICES', 'DEVICES');
define('GROUPS', 'GROUPS');
define('USERS', 'USERS');

class Notification extends Response{
	private $message = '';
	private $notifier_name;
	private $delivery_time = 0;
	private $recipients_list=array();
	private $recipeint_type = DEVICES;
	private $errors = array();

  public function __construct($message = '', $notifier_name = '', $delivery_time = 0, $recipients_list= array(), $recipeint_type=DEVICES) {
		$this->message = $message;
		$this->notifier_name = $notifier_name;
		$this->delivery_time = $delivery_time;
		$this->recipients_list = $recipients_list;
		$this->recipeint_type = $recipeint_type;
  }

  public function set_message($in){
    $this->message = $in;
  }
  public function get_message(){
    return $this->message;
  }

  public function set_notifier_name($in){
    $this->notifier_name = $in;
  }
  public function get_notifier_name(){
    return $this->notifier_name;
  }

  public function set_delivery_time($in){
    $this->delivery_time = $in;
  }
  public function get_delivery_time(){
    return $this->delivery_time;
  }

  public function set_recipients_list($in){
    $this->recipients_list = $in;
  }
  public function get_recipients_list(){
    return $this->recipients_list;
  }

  public function set_recipeint_type($in){
    $this->recipeint_type = $in;
  }
  public function get_recipeint_type(){
    return $this->recipeint_type;
  }

	public function log_error($in){
    $this->errors[] = $in;
  }
  public function errors(){
    return count($this->errors)?TRUE:FALSE;
  }
  public function get_error_array(){
    return $this->errors;
  }

}

?>
