<?php
/**
 * @file
 * Basic class for accessing Usergrid functionality.
 *
 * @author Daniel Johnson <djohnson@apigee.com>
 * @since 26-Apr-2013
 */

namespace Apigee\Usergrid;

class Client {

  const SDK_VERSION = '0.1';

  /**
   * Usergrid endpoint
   * @var string
   */
  private $uri;

  /**
   * Organization name. Find your in the Admin Portal (http://apigee.com/usergrid)
   * @var string
   */
  private $org_name;

  /**
   * App name. Find your in the Admin Portal (http://apigee.com/usergrid)
   * @var string
   */
  private $app_name;

  /**
   * @var bool
   */
  private $build_curl = FALSE;

  /**
   * @var Callable
   */
  private $log_callback = NULL;

  /**
   * @var int
   */
  private $call_timeout = 30000;

  /**
   * @var Callable
   */
  private $call_timeout_callback = NULL;

  /**
   * @var Callable
   */
  private $logout_callback = NULL;

  /**
   * @var string
   */
  private $oauth_token;


  /**
   * Object constructor
   *
   * @param string $org_name
   * @param string $app_name
   */
  public function __construct($org_name, $app_name) {
    $this->org_name = $org_name;
    $this->app_name = $app_name;
  }

  /* Accessor functions */
  public function get_oauth_token() {
    return $this->oauth_token;
  }
  public function set_oauth_token($token) {
    $this->oauth_token = $token;
  }

  public function enable_build_curl($bool = TRUE) {
    $this->build_curl = (bool)$bool;
  }
  public function is_build_curl_enabled() {
    return $this->build_curl;
  }
  public function set_log_callback($callback = NULL) {
    if (!empty($callback) && !is_callable($callback)) {
      throw new UsergridException('Log Callback is not callable.');
    }
    $this->log_callback = $callback;
  }
  public function set_call_timeout($seconds = 30) {
    $this->call_timeout = $seconds * 1000;
  }
  public function get_call_timeout() {
    return $this->call_timeout / 1000;
  }
  public function set_call_timeout_callback($callback = NULL) {
    if (isset($callback) && !is_callable($callback)) {
      throw new UsergridException('Call Timeout Callback is not callable.');
    }
    $this->call_timeout_callback = $callback;
  }
  public function set_logout_callback($callback = NULL) {
    if (isset($callback) && !is_callable($callback)) {
      throw new UsergridException('Logout Callback is not callable.');
    }
    $this->logout_callback = $callback;
  }
  /* End accessor functions */

  /**
   * If a log callback has been set, invoke it to write a given message.
   *
   * @param $message
   */
  public function write_log($message) {
    if (isset($this->log_callback)) {
      call_user_func($this->log_callback, $message);
    }
  }

  /**
   * Makes an HTTP request
   *
   * @param string $endpoint
   * @param string $method
   * @param array $body
   * @param array $qs
   * @param bool $m_query
   *
   * @return \Apigee\Util\HTTPResponse
   * @throws UsergridException
   */
  public function request($endpoint, $method = 'GET', $body = array(), $qs = array(), $m_query = FALSE) {
    $uri = $this->uri . '/' . ($m_query ? '' : $this->org_name . '/' . $this->app_name . '/') / $endpoint;

    if ($token = $this->get_oauth_token()) {
      $qs['access_token'] = $token;
    }
    if (!empty($qs)) {
      $uri .= '?' . http_build_query($qs);
    }

    $options = array(
      'headers' => array(),
      'method' => $method
    );

    if (!empty($body)) {
      $options['data'] = json_encode($body);
      $options['headers']['Content-Type'] = 'application/json';
      $options['headers']['Content-Length'] = strlen($options['data']);
    }

    $response = \Apigee\Util\HTTPClient::exec($uri, $options);

    $response->data = @json_decode($response->data, TRUE);

    $logout_callback = $this->logout_callback;

    $status = $response->code;

    if (floor($status / 100) != 2) {
      if ($response->data && array_key_exists('error', $response->data)) {
        $error = $response->data['error'];
        $desc = $response->data['description'];
      }
      else {
        $error = $response->status_message;
        $desc = (isset($response->error) ? $response->error : '');
      }

      $this->write_log("Error ($status)($error): $desc");

      if (!empty($logout_callback)) {
        switch ($error) {
          case 'auth_expired_session_token':
          case 'unauthorized':
          case 'auth_missing_credentials':
          case 'auth_invalid':
            call_user_func($logout_callback, $response);
            break;
        }
      }
    }
    if (!isset($response->error)) {
      $response->error = FALSE;
    }
    return $response;
  }

  /**
   * Creates an entity. If no error occurred, the entity may be accessed in the
   * returned object's ->parsed_objects['entity'] member.
   *
   * @param $entity_data
   * @return \Apigee\Usergrid\Entity
   */
  public function create_entity($entity_data) {
    $entity = new Entity($this, $entity_data['options']);
    $response = $entity->fetch();

    $ok_to_save = (
      ($response->error && ('service_resource_not_found' == $response->data['error'] || 'no_name_specified' == $response->data['error'] ))
      ||
      (!$response->error && array_key_exists('getOnExist', $entity_data) && $entity_data['getOnExist'])
    );

    if ($ok_to_save) {
      $entity->set($entity_data['options']);
      $response = $entity->save();
      if ($response->error) {
        $this->write_log('Could not create entity.');
        return FALSE;
      }
    }
    elseif ($response->error || array_key_exists('error', $response->data)) {
      return FALSE;
    }
    return $entity;
  }

  public function get_entity($entity_data) {
    $entity = new Entity($this, $entity_data);
    $response = $entity->fetch();
    if ($response->error || !empty($response->data['error'])) {
      $err_msg = (empty($response->data['error']) ? $response->error : $response->data['error']);
      $this->write_log($err_msg);
      return FALSE;
    }
    return $entity;
  }

  public function create_collection($type, $qs = array()) {
    // TODO: force the creation of a collection if it doesn't exist.
    // This should possibly be done in the Collection constructor, but it may
    // make more sense to do so in a Collection static method.
    $collection = new Collection($this, $type, $qs);
    return $collection;
  }

  public function create_user_activity($user_identifier, $user_data) {
    $user_data['type'] = "users/$user_identifier/activities";
    $entity = new Entity($this, $user_data);
    $response = $entity->save();
    $has_error = ($response->error || array_key_exists('error', $response->data));
    return ($has_error ? FALSE : $entity);
  }

  public function login($username, $password) {
    $body = array(
      'username' => $username,
      'password' => $password,
      'grant_type' => 'password'
    );
    $response = $this->request('token', 'POST', $body);
    $has_error = (floor($response->code / 100) != 2);
    if ($has_error) {
      $user = NULL;
      $error = 'Error trying to log user in.';
      $this->write_log($error);
      if (!$response->error) {
        $response->error = $error;
      }
    }
    else {
      $user = new Entity($this, $response->data['user']);
      $this->set_oauth_token($response->data['access_token']);
    }
    return ($user && !$response->error ? $user : FALSE);
  }

  public function login_facebook($fb_token) {
    // TODO
  }

  public function get_logged_in_user() {
    // TODO
  }

  public function is_logged_in() {
    return !empty($this->oauth_token);
  }

  public function log_out() {
    $this->oauth_token = NULL;
  }

  public static function is_uuid($uuid) {
    static $regex = '/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i';
    if (empty($uuid)) {
      return FALSE;
    }
    return preg_match($regex, $uuid);
  }

}