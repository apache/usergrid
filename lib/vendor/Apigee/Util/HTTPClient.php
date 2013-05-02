<?php
/**
 * @file
 * Provides a wrapper for http/https REST requests, based on cURL.
 *
 * @author Daniel Johnson <djohnson@apigee.com>
 * @since 01-May-2013
 */

namespace Apigee\Util;

class HTTPClient {

  /**
   * Executes an http (or https) call and returns data and metadata.
   *
   * This function accepts the same parameters as drupal_http_request(), with
   * the exception that it also accepts an 'auth_method' option. It does not
   * return the full list of response headers (since PHP's flavor of cURL does
   * not expose them), but it does return content-type and content-length
   * headers. This is generally enough for normal purposes. It does not return
   * the HTTP status message unless the status is in the 400-500 status range,
   * nor does it return the redirect url in the case of 300-range statuses.
   *
   * Expected members of the $opts array (all are optional):
   * - headers (associative array of request headers)
   * - method (defaults to GET)
   * - data (string payload for PUT/POST)
   * - auth_method (string description of HTTP authorization method. see below)
   *
   * Valid 'auth_method' values (case-insensitive):
   * - "Basic" (Default value. Cleartext, most widely supported.)
   * - "Digest" (More secure, but less widely supported; platform independent)
   * - "GSS Negotiate" (Used mostly with Active Directory)
   * - "NTLM" (Deprecated; used with older Windows NT authentication)
   * - "Any" (Tries all four of the above, and goes with the first that works)
   * - "Any Safe" (Like "Any", except excludes "Basic")
   * Any invalid 'auth_method' value will resolve to "Basic".
   *
   * If authentication is needed, username/password should be passed as part of
   * the URI, like so:
   * https://username:password@example.com/
   * This is done to maintain compatibility with drupal_http_request().
   * The URL will be parsed out and the user credentials will be sent in the
   * request headers rather than in the URI.
   *
   * @param string $url
   * @param array $opts
   * @return \Apigee\Util\HTTPResponse
   */
  public static function exec($url, $opts = array()) {
    // Initialize our return object
    $result = new HTTPResponse();

    // Fill in any missing options
    $opts += array(
      'headers' => array(), // contains request-headers
      'method' => 'GET',
      'data' => NULL,
      'auth_method' => 'Basic'
    );

    $uri = @parse_url($url);
    if (!$uri) {
      $result->error = 'Missing scheme';
      $result->code = -1001;
      return $result;
    }
    $scheme = (isset($uri['scheme']) ? strtolower($uri['scheme']) : 'http');
    if ($scheme != 'http' && $scheme != 'https') {
      $result->error = 'Invalid scheme';
      $result->code = -1002;
      return $result;
    }

    $headers = (isset($opts['headers']) && is_array($opts['headers']) ? $opts['headers'] : array());

    // Do some User-Agent magic. First set the default value.
    $user_agent = 'Apigee RESTClient';
    // We must search headers in a case-insensitive manner.
    foreach ($headers as $key => $val) {
      if (strtolower($key) == 'user-agent') {
        // Override the default if set by this class's client.
        $user_agent = $val;
        unset($headers[$key]);
      }
    }

    // Build our URL. Strip out any username/password stuff, and make sure
    // the URL has all the necessary parts.
    $path = isset($uri['path']) ? $uri['path'] : '/';
    if (isset($uri['query'])) {
      $path .= '?' . $uri['query'];
    }
    $host = $uri['host'];
    $default_port = ($scheme == 'http' ? 80 : 443);
    if (isset($uri['port']) && $uri['port'] != $default_port) {
      $host .= ':' . $uri['port'];
    }
    $url = $scheme . '://' . $host . $path;

    $method = isset($opts['method']) ? strtoupper($opts['method']) : 'GET';
    $payload = isset($opts['payload']) ? $opts['payload'] : '';
    if (strlen($payload) > 0 || $method == 'POST' || $method == 'PUT') {
      $headers['Content-Length'] = strlen($payload);
    }

    // Set some basic options.
    $curl_options = array(
      CURLOPT_RETURNTRANSFER => TRUE, // Makes curl_exec() return a string.
      CURLOPT_URL => $url,
      CURLINFO_HEADER_OUT => TRUE, // Must be present to get request headers
      CURLOPT_USERAGENT => $user_agent,
      CURLOPT_SSL_VERIFYPEER => FALSE, // Similar to cmd-line curl's -k option
      CURLOPT_HTTPHEADER => $headers
    );

    if (isset($uri['user']) && isset($uri['pass'])) {
      $curl_options[CURLOPT_HTTPAUTH] = self::parse_auth_method($opts['auth_method']);
      $curl_options[CURLOPT_USERPWD] = $uri['user'] . ':' . $uri['pass'];
    }

    switch ($method) {
      case 'GET':
        $curl_options[CURLOPT_HTTPGET] = TRUE;
        break;
      case 'POST':
        $curl_options[CURLOPT_POST] = TRUE;
        $curl_options[CURLOPT_POSTFIELDS] = $payload;
        break;
      case 'PUT':
        $curl_options[CURLOPT_PUT] = TRUE;
        $curl_options[CURLOPT_INFILE] = $payload;
        $curl_options[CURLOPT_INFILESIZE] = strlen($payload);
        break;
      default: // DELETE, HEAD, etc.
        $curl_options[CURLOPT_CUSTOMREQUEST] = 'DELETE';
        break;
    }
    if (($method == 'POST' || $method == 'PUT') && empty($payload)) {
      // PHP's cURL implementation will add a content-type header even if
      // the payload is empty. This is known to break certain obscure KMS
      // invocations. Therefore we must explicitly unset content-type in
      // this case.
      $curl_options[CURLOPT_HTTPHEADER]['Content-Type'] = '';
    }

    try {
      $ch = curl_init();
      curl_setopt_array($ch, $curl_options);

      $result->data = curl_exec($ch);
      $info = curl_getinfo($ch);
      $result->code = $info['http_code'];
      $result->request = $info['request_header'];
      $result->headers['content-type'] = $info['content_type'];
      $result->headers['content-length'] = strlen($result->data);

      $result->status_message = self::get_status_message($result->code);

      // Handle non-200-class statuses.
      // We should never get a 100-class status, since it is an intermediate
      // status occasionally sent while the server prepares to send content.
      $status_class = floor($result->code / 100);
      // We might conceivably get a $result->code of zero, indicating cURL
      // couldn't connect.
      if ($status_class < 2) {
        $result->error = 'Connection failure';
      }
      elseif ($status_class > 3) {
        $result->error = $result->status_message;
      }
    }
    catch (\Exception $e) {
      $result->code = -abs($e->getCode());
      $result->error = $e->getMessage();
    }
    return $result;
  }

  /**
   * Parse string representation of auth type into a CURLAUTH_* constant.
   *
   * If auth type cannot be parsed, it defaults to CURLAUTH_BASIC.
   *
   * @param string $auth_type
   * @return int
   */
  private static function parse_auth_method($auth_type) {
    if (is_int($auth_type) && in_array($auth_type, array(CURLAUTH_ANYSAFE, CURLAUTH_ANY, CURLAUTH_NTLM, CURLAUTH_GSSNEGOTIATE, CURLAUTH_DIGEST, CURLAUTH_BASIC))) {
      return $auth_type;
    }

    switch (strtolower($auth_type)) {
      case 'basic':
        return CURLAUTH_BASIC;
      case 'digest':
        return CURLAUTH_DIGEST;
      case 'gss negotiate':
      case 'gssnegotiate':
        return CURLAUTH_GSSNEGOTIATE;
      case 'ntlm':
        return CURLAUTH_NTLM;
      case 'any':
        return CURLAUTH_ANY;
      case 'any safe':
      case 'anysafe':
        return CURLAUTH_ANYSAFE;
    }
    return CURLAUTH_BASIC;
  }

  /**
   * Given a status code, returns the proper human-readable message which
   * corresponds to that code.
   *
   * @param int $code
   * @return string
   */
  private static function get_status_message($code) {
    static $responses = array(
      100 => 'Continue',
      101 => 'Switching Protocols',
      102 => 'Processing', // WebDAV

      200 => 'OK',
      201 => 'Created',
      202 => 'Accepted',
      203 => 'Non-Authoritative Information',
      204 => 'No Content',
      205 => 'Reset Content',
      206 => 'Partial Content',
      207 => 'Multi-Status', // WebDAV
      208 => 'Already Reported', // WebDAV
      226 => 'IM Used',

      300 => 'Multiple Choices',
      301 => 'Moved Permanently',
      302 => 'Found',
      303 => 'See Other',
      304 => 'Not Modified',
      305 => 'Use Proxy',
      306 => 'Switch Proxy',
      307 => 'Temporary Redirect',
      308 => 'Permanent Redirect',

      400 => 'Bad Request',
      401 => 'Unauthorized',
      402 => 'Payment Required',
      403 => 'Forbidden',
      404 => 'Not Found',
      405 => 'Method Not Allowed',
      406 => 'Not Acceptable',
      407 => 'Proxy Authentication Required',
      408 => 'Request Time-out',
      409 => 'Conflict',
      410 => 'Gone',
      411 => 'Length Required',
      412 => 'Precondition Failed',
      413 => 'Request Entity Too Large',
      414 => 'Request-URI Too Large',
      415 => 'Unsupported Media Type',
      416 => 'Requested Range Not Satisfiable',
      417 => 'Expectation Failed',
      418 => 'I\'m a teapot', // RFC 2324 ;-)
      420 => 'Enhance Your Calm', // Twitter ;-)
      422 => 'Unprocessable Entity', // WebDAV
      423 => 'Locked', // WebDAV
      424 => 'Failed Dependency', // WebDAV
      425 => 'Unordered Collection',
      426 => 'Upgrade Required',
      428 => 'Precondition Required',
      429 => 'Too Many Requests',
      431 => 'Request Header Fields Too Large',
      444 => 'No Response', // nginx
      449 => 'Retry With', // Microsoft
      450 => 'Blocked By Parental Controls', // Microsoft
      451 => 'Unavailable for Legal Reasons',
      494 => 'Request Header Too Large', // nginx
      495 => 'Cert Error', // nginx
      496 => 'No Cert', // nginx
      497 => 'HTTP to HTTPS', // nginx
      499 => 'Client Closed Request', // nginx

      500 => 'Internal Server Error',
      501 => 'Not Implemented',
      502 => 'Bad Gateway',
      503 => 'Service Unavailable',
      504 => 'Gateway Timeout',
      505 => 'HTTP Version not supported',
      506 => 'Variant Also Negotiates',
      507 => 'Insufficient Storage', // WebDAV
      508 => 'Loop Detected', // WebDAV
      509 => 'Bandwidth Limit Exceeded', // apache?
      510 => 'Not Extended',
      511 => 'Network Authentication Required',
      598 => 'Network read timeout error', // Microsoft
      599 => 'Network connect timeout error', // Microsoft
    );

    if (!isset($responses[$code])) {
      // According to RFC 2616, all unknown HTTP codes must be treated the same
      // as the base code in their class.
      $code = floor($code / 100) * 100;
    }
    if (!isset($responses[$code])) {
      // Something is seriously screwy; treat it as an internal server error.
      $code = 500;
    }
    return $responses[$code];
  }
}
