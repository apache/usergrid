<?php

/**
* @file
* Request - a data structure to hold all request-related parameters
*
* @author Rod Simpson <rod@apigee.com>
* @since 26-Apr-2013
*/

namespace Apigee\Usergrid;


class Request {
  public $method ='';
  public $endpoint = '';
  public $query_string_array = ''; //an array of key value pairs to be appended as the query string
  public $body = '';
  public $management_query = false;

  public function __construct() {


  }
  public function set_method($in){
    if ($in !== 'GET' && $in !== 'POST' && $in !== 'PUT' && $in !== 'DELETE') {
      throw new Exception('Unknown method type');
    }
    $this->method = $in;
  }
  public function get_method(){
    return $this->method;
  }

  public function set_endpoint($in){
    $this->endpoint = $in;
  }
  public function get_endpoint(){
    return $this->endpoint;
  }

  public function set_query_string_array($in){
    $this->query_string_array = $in;
  }
  public function get_query_string_array(){
    return $this->query_string_array;
  }

  public function set_body($in){
    $this->body = $in;
  }
  public function get_body(){
    return $this->body;
  }

  public function set_management_query($in){
    //ensure we have a bool
    if ($in) {
      $this->management_query = true;
    }
    $this->management_query = false;
  }
  public function get_management_query(){
    return $this->management_query;
  }

}

?>