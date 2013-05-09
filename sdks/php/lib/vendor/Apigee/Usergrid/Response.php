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
  public $data = array();
  public $curl_meta = array();
  public $error = false;
  public $error_message = '';

  public function __construct() {


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

  public function set_error_message($in){
    $this->error_message = $in;
  }
  public function get_error_message(){
    return $this->error_message;
  }
}

?>