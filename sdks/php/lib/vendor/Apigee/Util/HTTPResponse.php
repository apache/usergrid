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
  public $code;
  public $error;
  public $request;
  public $data;
  public $headers;

  public function __construct() {
    $this->error = FALSE;
    $this->code = 0;
    $this->request = '';
    $this->data = '';
    $this->headers = array();
  }
}