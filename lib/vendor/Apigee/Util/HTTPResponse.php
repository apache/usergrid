<?php
/**
 * @file
 * Convenience class for holding responses from HTTP calls.
 *
 * @author Daniel Johnson <djohnson@apigee.com>
 * @since 26-Apr-2013
 */

namespace Apigee\Util;

class HTTPResponse {

  public $error;
  public $code;
  public $request;
  public $data;
  public $protocol;
  public $status_message;
  public $headers;
  public $redirect_url;

  public $parsed_objects;

  public function __construct() {
    $this->error = FALSE;
    $this->code = 0;
    $this->request = '';
    $this->data = '';
    $this->protocol = '';
    $this->status_message = '';
    $this->headers = array();
    $this->redirect_url = '';

    $this->parsed_objects = array();
  }

}