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
 * Response - a data structure to hold all response-related parameters
 *
 * @author Rod Simpson <rod@apigee.com>
 * @since 26-Apr-2013
 */

namespace Apache\Usergrid;


class Response extends Request{
  private $data = array();
  private $json = '';
  private $curl_meta = array();
  private $error = FALSE;
  private $error_code = '';
  private $error_message = '';

  public function __construct($data = array(), $curl_meta = array(), $error = FALSE, $error_code = '', $error_message = '', $json = '') {
		$this->data = $data;
		$this->json = $json;
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

  public function set_json($in){
    $this->json = $in;
  }
  public function get_json(){
    return $this->json;
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
