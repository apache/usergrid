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
  private $method ='';
  private $endpoint = '';
  private $query_string_array = ''; //an array of key value pairs to be appended as the query string
  private $body = '';
  private $management_query = FALSE;

  public function __construct($method = 'GET', $endpoint = '', $query_string_array = array(), $body = array(), $management_query=FALSE) {
		$this->method = $method;
		$this->endpoint = $endpoint;
		$this->query_string_array = $query_string_array;
		$this->body = $body;
		$this->management_query = $management_query;
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