<?php
/**
 * @file
 * Response - a data structure to hold all response-related parameters
 *
 * @author Rod Simpson <rod@apigee.com>
 * @since 26-Apr-2013
 */

namespace Apigee\Usergrid;


class Response extends Request{
  private $data = array();
  private $curl_meta = array();
  private $error = FALSE;
  private $error_code = '';
  private $error_message = '';

  public function __construct($data = array(), $curl_meta = array(), $error = FALSE, $error_code = '', $error_message = '') {
		$this->data = $data;
		$this->curl_meta = $curl_meta;
		$this->error = $error;
		$this->set_error_code = $error_code;
		$this->error_message = $error_message;
  }

  public function set_data($in){
    $this->data = $in;
  }
  public function get_data(){
    return $this->data;
  }

  public function set_curl_meta($in){
    $this->curl_meta = $in;
  }
  public function get_curl_meta(){
    return $this->curl_meta;
  }

  public function set_error($in){
    $this->error = $in;
  }
  public function get_error(){
    return $this->error;
  }

  public function set_error_code($in){
    $this->error_code = $in;
  }
  public function get_error_code(){
    return $this->error_code;
  }

  public function set_error_message($in){
    $this->error_message = $in;
  }
  public function get_error_message(){
    return $this->error_message;
  }
}
