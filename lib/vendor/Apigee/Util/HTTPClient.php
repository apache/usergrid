<?php
/**
 * @file
 * Provides a static method to make HTTP requests.
 *
 * @author Daniel Johnson <djohnson@apigee.com>
 * @since 26-Apr-2013
 */

namespace Apigee\Util;

class HTTPClient {

  /**
   * This is adapted from drupal_http_request().
   *
   * @param $url
   * @param $opts
   * @return \Apigee\Util\HTTPResponse
   */
  public static function exec($url, $opts) {

    $result = new HTTPResponse();

    $opts += array(
      'headers' => array(),
      'method' => 'GET',
      'data' => NULL,
      'max_redirects' => 3,
      'timeout' => 30.0,
      'context' => NULL,
    );

    $uri = @parse_url($url);
    if (!$uri) {
      $result->error = 'missing scheme';
      $result->code = -1002;
      return $result;
    }
    $scheme = (isset($uri['scheme']) ? strtolower($uri['scheme']) : 'http');
    if ($scheme != 'http' && $scheme != 'https') {
      $result->error = 'Invalid scheme';
      $result->code = -1002;
      return $result;
    }

    $headers = (isset($opts['headers']) && is_array($opts['headers']) ? $opts['headers'] : array());
    if (!isset($headers['User-Agent'])) {
      $headers['User-Agent'] = 'Apigee HTTPClient';
    }

    if ($scheme == 'http') {
      $port = isset($uri['port']) ? $uri['port'] : 80;
      $socket = 'tcp://' . $uri['host'] . ':' . $port;
      $headers['Host'] = $uri['host'] . ($port != 80 ? ':' . $port : '');
    }
    else {
      // Note: Only works when PHP is compiled with OpenSSL support.
      $port = isset($uri['port']) ? $uri['port'] : 443;
      $socket = 'ssl://' . $uri['host'] . ':' . $port;
      $headers['Host'] = $uri['host'] . ($port != 443 ? ':' . $port : '');
    }

    $fp = @stream_socket_client($socket, $errno, $errstr, $opts['timeout']);
    if (!$fp) {
      $message = trim($errstr);
      if (empty($message)) {
        $message = 'Error opening socket ' . $socket;
      }
      $result->code = -$errno;
      $result->error = $message;
      return $result;
    }
    $path = isset($uri['path']) ? $uri['path'] : '/';
    if (isset($uri['query'])) {
      $path .= '?' . $uri['query'];
    }
    $method = isset($opts['method']) ? $opts['method'] : 'GET';
    $payload = isset($opts['payload']) ? $opts['payload'] : '';
    if (strlen($payload) > 0 || $method == 'POST' || $method == 'PUT') {
      $headers['Content-Length'] = strlen($payload);
    }
    if (isset($uri['user']) && isset($uri['pass'])) {
      $headers['Authorization'] = 'Basic ' . base64_encode($uri['user'] . ':' . $uri['pass']);
    }

    $request = $method . ' ' . $path . " HTTP/1.0\r\n";
    foreach ($headers as $name => $value) {
      $request .= $name . ': ' . trim($value) . "\r\n";
    }
    $request .= "\r\n" . $payload;

    $result->request = $request;

    stream_set_timeout($fp, floor($opts['timeout']));
    fwrite($fp, $request);

    // Fetch response. Due to PHP bugs like http://bugs.php.net/bug.php?id=43782
    // and http://bugs.php.net/bug.php?id=46049 we can't rely on feof(), but
    // instead must invoke stream_get_meta_data() each iteration.
    $info = stream_get_meta_data($fp);
    $alive = !$info['eof'] && !$info['timed_out'];
    $response = '';

    $start_time = microtime(TRUE);

    while ($alive) {
      // Calculate how much time is left of the original timeout value.
      $timeout = $opts['timeout'] - (microtime(TRUE) - $start_time);
      if ($timeout <= 0) {
        $result->code = -1;
        $result->error = 'request timed out';
        return $result;
      }
      stream_set_timeout($fp, floor($timeout), floor(1000000 * fmod($timeout, 1)));
      $chunk = fread($fp, 1024);
      $response .= $chunk;
      $info = stream_get_meta_data($fp);
      $alive = !$info['eof'] && $chunk;
    }
    fclose($fp);

    // Parse response headers from the response body.
    // Be tolerant of malformed HTTP responses that separate header and body with
    // \n\n or \r\r instead of \r\n\r\n.
    list($response, $result->data) = preg_split("/\r\n\r\n|\n\n|\r\r/", $response, 2);
    $response = preg_split("/\r\n|\n|\r/", $response);

    // Parse the response status line.
    list($protocol, $code, $status_message) = explode(' ', trim(array_shift($response)), 3);
    $result->protocol = $protocol;
    $result->status_message = $status_message;

    $result->headers = array();

    // Parse the response headers.
    while ($line = trim(array_shift($response))) {
      list($name, $value) = explode(':', $line, 2);
      $name = strtolower($name);
      //$name = strtolower($name);
      if (isset($result->headers[$name]) && $name == 'set-cookie') {
        // RFC 2109: the Set-Cookie response header comprises the token Set-
        // Cookie:, followed by a comma-separated list of one or more cookies.
        $result->headers[$name] .= ',' . trim($value);
      }
      else {
        $result->headers[$name] = trim($value);
      }
    }

    $responses = array(
      100 => 'Continue',
      101 => 'Switching Protocols',
      200 => 'OK',
      201 => 'Created',
      202 => 'Accepted',
      203 => 'Non-Authoritative Information',
      204 => 'No Content',
      205 => 'Reset Content',
      206 => 'Partial Content',
      300 => 'Multiple Choices',
      301 => 'Moved Permanently',
      302 => 'Found',
      303 => 'See Other',
      304 => 'Not Modified',
      305 => 'Use Proxy',
      307 => 'Temporary Redirect',
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
      416 => 'Requested range not satisfiable',
      417 => 'Expectation Failed',
      500 => 'Internal Server Error',
      501 => 'Not Implemented',
      502 => 'Bad Gateway',
      503 => 'Service Unavailable',
      504 => 'Gateway Time-out',
      505 => 'HTTP Version not supported',
    );
    // RFC 2616 states that all unknown HTTP codes must be treated the same as the
    // base code in their class.
    if (!isset($responses[$code])) {
      $code = floor($code / 100) * 100;
    }
    $result->code = $code;

    switch ($result->code) {
      case 200: // OK
      case 201: // Created
      case 202: // Accepted
      case 204: // No Content
      case 304: // Not modified
        break;
      case 301: // Moved permanently
      case 302: // Moved temporarily
      case 307: // Moved temporarily
        $location = $result->headers['location'];
        $opts['timeout'] -= (microtime(TRUE) - $start_time);
        if ($opts['timeout'] <= 0) {
          $result->code = -1;
          $result->error = 'request timed out';
          return $result;
        }
        elseif ($opts['max_redirects']) {
          // Redirect to the new location.
          $opts['max_redirects']--;
          if ($opts['max_redirects'] < 1) {
            $result->code = -2;
            $result->error = 'Too many redirects';
            return $result;
          }
          $result = self::exec($location, $opts);
          if (!isset($result->redirect_url)) {
            $result->redirect_url = $location;
          }
        }
        break;
      default:
        $result->error = $status_message;
    }
    return $result;
  }
}