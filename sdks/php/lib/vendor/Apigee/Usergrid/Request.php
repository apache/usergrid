<?php
/**
* @file
* Request - a data structure to hold all request-related parameters
*
* @author Rod Simpson <rod@apigee.com>
* @since 26-Apr-2013
*/

namespace Apigee\Usergrid;

require_once (dirname(__FILE__) . '/Exceptions.php');


class Request {
  /**
   * @var string
   */
  private $method;
  /**
   * @var string
   */
  private $endpoint;
  /**
   * @var array
   */
  private $query_string_array; //an array of key value pairs to be appended as the query string
  /**
   * @var array
   */
  private $body;
  /**
   * @var bool
   */
  private $management_query;

  /**
   * Constructor for Request object
   *
   * @param string $method
   * @param string $endpoint
   * @param array $query_string_array
   * @param array $body
   * @param bool $management_query
   */
  public function __construct($method = 'GET', $endpoint = '', $query_string_array = array(), $body = array(), $management_query=FALSE) {
		$this->method = $method;
		$this->endpoint = $endpoint;
		$this->query_string_array = $query_string_array;
		$this->body = $body;
		$this->management_query = $management_query;
  }

  /**
   * Sets (and validates) the HTTP method.
   *
   * @param string $http_method
   * @throws UGException
   */
  public function set_method($http_method){
    if ($http_method !== 'GET' && $http_method !== 'POST' && $http_method !== 'PUT' && $http_method !== 'DELETE') {
      throw new UGException("Unknown HTTP method type $http_method");
    }
    $this->method = $http_method;
  }

  /**
   * Gets the HTTP method (GET, POST, PUT or DELETE)
   *
   * @return string
   */
  public function get_method(){
    return $this->method;
  }

  /**
   * Sets the endpoint base URL.
   *
   * @param $endpoint
   */
  public function set_endpoint($endpoint){
    $this->endpoint = $endpoint;
  }
  /**
   * Returns the endpoint base URL.
   *
   * @return string
   */
  public function get_endpoint(){
    return $this->endpoint;
  }

  /**
   * Sets the query-string array. This should be an associative key-value hash.
   *
   * @param array $in
   */
  public function set_query_string_array($key_value_pairs) {
    if (!is_array($key_value_pairs)) {
      throw new UGException('Request->query_string_array must be an array.');
    }
    $this->query_string_array = $key_value_pairs;
  }
  /**
   * Returns the key-value associative array of query string values.
   *
   * @return array
   */
  public function get_query_string_array(){
    return $this->query_string_array;
  }

  /**
   * Sets the body array. This should be an array structure representing
   * parameters sent via PUT/POST.
   *
   * @param array $body
   */
  public function set_body($body){
    if (!is_array($body)) {
      throw new UGException('Request->body must be an array.');
    }
    $this->body = $body;
  }

  /**
   * Returns the body array.
   *
   * @return array
   */
  public function get_body(){
    return $this->body;
  }

  /**
   * Sets the boolean flag indicating whether this is a "management query" or
   * not.
   *
   * @param bool $in
   */
  public function set_management_query($in){
    //ensure we have a bool
    $this->management_query = (bool)$in;
  }

  /**
   * Returns flag indicating whether this is a "management query".
   *
   * @return bool
   */
  public function get_management_query(){
    return $this->management_query;
  }

}
