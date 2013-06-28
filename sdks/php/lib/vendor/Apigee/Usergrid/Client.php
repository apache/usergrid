<?php
/**
 * @file
 * Basic class for accessing Usergrid functionality.
 *
 * @author Daniel Johnson <djohnson@apigee.com>
 * @author Rod Simpson <rod@apigee.com>
 * @since 26-Apr-2013
 */

namespace Apigee\Usergrid;

require_once(dirname(__FILE__) . '/Exceptions.php');

define('AUTH_CLIENT_ID', 'CLIENT_ID');
define('AUTH_APP_USER', 'APP_USER');
define('AUTH_NONE', 'NONE');

class Client {

  const SDK_VERSION = '0.1';

  /**
   * Usergrid endpoint
   * @var string
   */
  private $url = 'http://api.usergrid.com';

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
   * @var bool
   */
  private $use_exceptions = FALSE;

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
   * @var string
   */
  private $client_id;

  /**
   * @var string
   */
  private $client_secret;

  /**
   * @var string
   */
  private $auth_type = AUTH_APP_USER;

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

  /**
   * Returns OAuth token if it has been set; else NULL.
   *
   * @return string|NULL
   */
  public function get_oauth_token() {
    return $this->oauth_token;
  }

  /**
   * Sets OAuth token.
   *
   * @param string $token
   */
  public function set_oauth_token($token) {
    $this->oauth_token = $token;
  }

  /**
   * Returns the authorization type in use.
   *
   * @return string
   */
  public function get_auth_type() {
    return $this->auth_type;
  }

  /**
   * Sets (and validates) authorization type in use. If an invalid auth_type is
   * passed, AUTH_APP_USER is assumed.
   *
   * @param $auth_type
   * @throws UGException
   */
  public function set_auth_type($auth_type) {
    if ($auth_type == AUTH_APP_USER || $auth_type == AUTH_CLIENT_ID || $auth_type == AUTH_NONE) {
      $this->auth_type = $auth_type;
    }
    else {
      $this->auth_type = AUTH_APP_USER;
      if ($this->use_exceptions) {
        throw new UGException('Auth type is not valid');
      }
    }
  }

  /**
   * Returns client_id.
   *
   * @return string
   */
  public function get_client_id() {
    return $this->client_id;
  }

  /**
   * Sets the client ID.
   *
   * @param string $id
   */
  public function set_client_id($id) {
    $this->client_id = $id;
  }

  /**
   * Gets the client secret.
   *
   * @return string
   */
  public function get_client_secret() {
    return $this->client_secret;
  }

  /**
   * Sets the client secret. You should have received this information when
   * you registered your UserGrid/Apigee account.
   *
   * @param $secret
   */
  public function set_client_secret($secret) {
    $this->client_secret = $secret;
  }

  /**
   * When set to TRUE, a curl command-line string will be generated. This may
   * be useful when debugging.
   *
   * @param bool $bool
   */
  public function enable_build_curl($bool = TRUE) {
    $this->build_curl = (bool) $bool;
  }

  /**
   * Returns TRUE if curl command-line building is enabled, else FALSE.
   *
   * @return bool
   */
  public function is_build_curl_enabled() {
    return $this->build_curl;
  }

  /**
   * Enables/disables use of exceptions when errors are encountered.
   *
   * @param bool $bool
   */
  public function enable_exceptions($bool = TRUE) {
    $this->use_exceptions = (bool) $bool;
  }

  /**
   * @return bool
   */
  public function are_exceptions_enabled() {
    return $this->use_exceptions;
  }

  /**
   * Sets the callback for logging functions.
   *
   * @param Callable|NULL $callback
   * @throws UGException
   * @see write_log()
   */
  public function set_log_callback($callback = NULL) {
    if (!empty($callback) && !is_callable($callback)) {
      if ($this->use_exceptions) {
        throw new UGException('Log Callback is not callable.');
      }
      $this->log_callback = NULL;
    }
    else {
      $this->log_callback = (empty($callback) ? NULL : $callback);
    }
  }

  /**
   * Sets the timeout for HTTP calls in seconds. Internally this is stored in
   * milliseconds.
   *
   * @param int|float $seconds
   */
  public function set_call_timeout($seconds = 30) {
    $this->call_timeout = intval($seconds * 1000);
  }

  /**
   * Gets timeout for HTTP calls in seconds. May return fractional parts.
   *
   * @return float
   */
  public function get_call_timeout() {
    return $this->call_timeout / 1000;
  }

  /**
   * Sets the callback to be invoked when an HTTP call timeout occurs.
   *
   * @TODO Actually use/invoke this callback. Currently not implemented.
   *
   * @param Callable|null $callback
   * @throws UGException
   */
  public function set_call_timeout_callback($callback = NULL) {
    if (!empty($callback) && !is_callable($callback)) {
      if ($this->use_exceptions) {
        throw new UGException('Call Timeout Callback is not callable.');
      }
      $this->call_timeout_callback = NULL;
    }
    else {
      $this->call_timeout_callback = (empty($callback) ? NULL : $callback);
    }
  }

  /**
   * Sets the callback to be invoked upon logout.
   *
   * @TODO Actually use/invoke this callback. Currently not implemented.
   *
   * @param Callable|null $callback
   * @throws UGException
   */
  public function set_logout_callback($callback = NULL) {
    if (!empty($callback) && !is_callable($callback)) {
      if ($this->use_exceptions) {
        throw new UGException('Logout Callback is not callable.');
      }
      $this->logout_callback = NULL;
    }
    else {
      $this->logout_callback = (empty($callback) ? NULL : $callback);
    }
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
   * Issues a CURL request via HTTP/HTTPS and returns the response.
   *
   * @param Request $request
   * @return Response
   * @throws UG_404_NotFound
   * @throws UG_403_Forbidden
   * @throws UGException
   * @throws UG_500_ServerError
   * @throws UG_401_Unauthorized
   * @throws UG_400_BadRequest
   */
  public function request(Request $request) {

    $method = $request->get_method();
    $endpoint = $request->get_endpoint();
    $body = $request->get_body();
    $query_string_array = $request->get_query_string_array();

    if ($this->get_auth_type() == AUTH_APP_USER) {
      if ($token = $this->get_oauth_token()) {
        $query_string_array['access_token'] = $token;
      }
      else {
        $query_string_array['client_id'] = $this->get_client_id();
        $query_string_array['client_secret'] = $this->get_client_secret();
      }
    }

    foreach ($query_string_array as $key => $value) {
      $query_string_array[$key] = urlencode($value);
    }
    $query_string = http_build_query($query_string_array);
    if ($request->get_management_query()) {
      $url = $this->url . '/' . $endpoint;
    }
    else {
      $url = $this->url . '/' . $this->org_name . '/' . $this->app_name . '/' . $endpoint;
    }

    //append params to the path
    if ($query_string) {
      $url .= '?' . $query_string;
    }
    $curl = curl_init($url);

    if ($method == 'POST' || $method == 'PUT' || $method == 'DELETE') {
      curl_setopt($curl, CURLOPT_CUSTOMREQUEST, $method);
    }
    if ($method == 'POST' || $method == 'PUT') {
      $body = json_encode($body);
      curl_setopt($curl, CURLOPT_HTTPHEADER, array(
        'Content-Length: ' . strlen($body),
        'Content-Type: application/json'
      ));
      curl_setopt($curl, CURLOPT_POSTFIELDS, $body);
    }


    curl_setopt($curl, CURLOPT_RETURNTRANSFER, TRUE);
    curl_setopt($curl, CURLOPT_SSL_VERIFYPEER, FALSE);
    curl_setopt($curl, CURLOPT_FOLLOWLOCATION, FALSE);
    curl_setopt($curl, CURLOPT_MAXREDIRS, 10);
    curl_setopt($curl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);


    $response = curl_exec($curl);
    $meta = curl_getinfo($curl);

    curl_close($curl);

    $response_array = @json_decode($response, TRUE);
    $response_obj = new Response();
    $response_obj->set_curl_meta($meta);
    $response_obj->set_data($response_array);

    if ($meta['http_code'] != 200) {
      //there was an API error
      $error_code = $response_array['error'];
      $description = isset($response_array['error_description']) ? $response_array['error_description'] : '';
      $description = isset($response_array['exception']) ? $response_array['exception'] : $description;
      $this->write_log('Error: ' . $meta['http_code'] . ' error:' . $description);
      $response_obj->set_error(TRUE);
      $response_obj->set_error_code($error_code);
      $response_obj->set_error_message($description);

      if ($this->use_exceptions) {
        switch ($meta['http_code']) {
          case 400:
            throw new UG_400_BadRequest($description, $meta['http_code']);
            break;
          case 401:
            throw new UG_401_Unauthorized($description, $meta['http_code']);
            break;
          case 403:
            throw new UG_403_Forbidden($description, $meta['http_code']);
            break;
          case 404:
            throw new UG_404_NotFound($description, $meta['http_code']);
            break;
          case 500:
            throw new UG_500_ServerError($description, $meta['http_code']);
            break;
          default:
            throw new UGException($description, $meta['http_code']);
            break;
        }
      }

    }
    else {
      $response_obj->set_error(FALSE);
      $response_obj->set_error_message(FALSE);
    }

    return $response_obj;
  }

  /**
   * Performs an HTTP GET operation
   *
   * @param string $endpoint
   * @param array $query_string_array
   * @return Response
   */
  public function get($endpoint, $query_string_array) {

    $request = new Request();
    $request->set_method('GET');
    $request->set_endpoint($endpoint);
    $request->set_query_string_array($query_string_array);

    $response = $this->request($request);

    return $response;
  }

  /**
   * Performs an HTTP POST operation
   *
   * @param string $endpoint
   * @param array $query_string_array
   * @param array $body
   * @return Response
   */
  public function post($endpoint, $query_string_array, $body) {

    $request = new Request();
    $request->set_method('POST');
    $request->set_endpoint($endpoint);
    $request->set_query_string_array($query_string_array);
    $request->set_body($body);

    $response = $this->request($request);

    return $response;
  }

  /**
   * Performs an HTTP PUT operation
   *
   * @param string $endpoint
   * @param array $query_string_array
   * @param array $body
   * @return Response
   */
  public function put($endpoint, $query_string_array, $body) {

    $request = new Request();
    $request->set_method('PUT');
    $request->set_endpoint($endpoint);
    $request->set_query_string_array($query_string_array);
    $request->set_body($body);

    $response = $this->request($request);

    return $response;
  }

  /**
   * Performs an HTTP DELETE operation
   *
   * @param string $endpoint
   * @param array $query_string_array
   * @return Response
   */
  public function delete($endpoint, $query_string_array) {
    $request = new Request();
    $request->set_method('DELETE');
    $request->set_endpoint($endpoint);
    $request->set_query_string_array($query_string_array);

    $response = $this->request($request);

    return $response;
  }

  /**
   * Creates an entity. If no error occurred, the entity may be accessed in the
   * returned object's ->parsed_objects['entity'] member.
   *
   * @param array $entity_data
   * @return \Apigee\Usergrid\Entity
   */
  public function create_entity($entity_data) {
    $entity = new Entity($this, $entity_data);
    $response = $entity->fetch();

    $ok_to_save = (
      ($response->get_error() && ('service_resource_not_found' == $response->get_error_code() || 'no_name_specified' == $response->get_error_code()))
      ||
      (!$response->get_error() && array_key_exists('getOnExist', $entity_data) && $entity_data['getOnExist'])
    );

    if ($ok_to_save) {
      $entity->set($entity_data);
      $response = $entity->save();
      if ($response->get_error()) {
        $this->write_log('Could not create entity.');
        return FALSE;
      }
    }
    elseif ($response->get_error() || array_key_exists('error', $response->get_data())) {
      return FALSE;
    }
    return $entity;
  }

  /**
   * Fetches and returns an entity.
   *
   * @param $entity_data
   * @return \Apigee\Usergrid\Entity|bool
   */
  public function get_entity($entity_data) {
    $entity = new Entity($this, $entity_data);
    $response = $entity->fetch();
    if ($response->get_error()) {
      $this->write_log($response->get_error_message());
      return FALSE;
    }
    return $entity;
  }

  /**
   * Fetches and returns a collection. If the collection does not yet exist,
   * it is created.
   *
   * @param string $type
   * @param array $qs
   * @return \Apigee\Usergrid\Collection
   */
  public function get_collection($type, $qs = array()) {
    $collection = new Collection($this, $type, $qs);
    return $collection;
  }

  /**
   * Creates and returns a user-activity entity. Returns FALSE if such an
   * entity could not be created.
   *
   * @param string $user_identifier
   * @param array $user_data
   * @return \Apigee\Usergrid\Entity|bool
   */
  public function create_user_activity($user_identifier, $user_data) {
    $user_data['type'] = "users/$user_identifier/activities";
    $entity = new Entity($this, $user_data);
    $response = $entity->save();
    return ($response->get_error() ? FALSE : $entity);
  }

  /**
   * Attempts a login. If successful, sets the OAuth token to be used for
   * subsequent calls, and returns a User entity. If unsuccessful, returns
   * FALSE.
   *
   * @param string $username
   * @param string $password
   * @return \Apigee\Usergrid\Entity|bool
   */
  public function login($username, $password) {
    $body = array(
      'username' => $username,
      'password' => $password,
      'grant_type' => 'password'
    );
    $response = $this->POST('token', array(), $body);
    if ($response->get_error()) {
      $user = NULL;
      $error = 'Error trying to log user in.';
      $this->write_log($error);
      if (!$response->get_error()) {
        $response->set_error(TRUE);
        $response->set_error_message($error);
        $response->set_error_code($error);
      }
    }
    else {
      $response_data = $response->get_data();
      $user = new Entity($this, $response_data['user']);
      $this->set_oauth_token($response_data['access_token']);
    }
    return ($user && !$response->get_error() ? $user : FALSE);
  }

  /**
   * Not yet implemented. Logs in via Facebook.
   *
   * @param $fb_token
   */
  public function login_facebook($fb_token) {
    // TODO
  }

  /**
   * A public facing helper method for signing up users
   *
   * @params string $username
   * @params string $password
   * @params string $email
   * @params string $name
   * @return \Apigee\Usergrid\Entity
   */
  public function signup($username, $password, $email, $name) {
    $data = array(
      'type' => 'users',
      'username' => $username,
      'password' => $password,
      'email' => $email,
      'name' => $name
    );
    return $this->create_entity($data);
  }

  /**
   * Returns current user as an entity. If no user is logged in,
   * returns FALSE.
   *
   * @return Entity|bool
   */
  public function get_logged_in_user() {
    $data = array('username' => 'me', 'type' => 'users');
    return $this->get_entity($data);
  }

  /**
   * Determines if a user is logged in.
   *
   * @return bool
   */
  public function is_logged_in() {
    return !empty($this->oauth_token);
  }

  /**
   * Logs current user out.
   * @todo: Invoke logout callback.
   */
  public function log_out() {
    $this->oauth_token = NULL;
  }

  /**
   * Determines if a string is a valid UUID. Note that this function will
   * return FALSE if the UUID is wrapped in curly-braces, Microsoft-style.
   *
   * @param $uuid
   * @return bool
   */
  public static function is_uuid($uuid) {
    static $regex = '/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i';
    if (empty($uuid)) {
      return FALSE;
    }
    return (bool) preg_match($regex, $uuid);
  }

  // TODO: Document the following methods

  public function createNewNotifierApple($name, $environment, $p12Certificate_path) {

    $endpoint = "notifiers";
    $data = array(
      "name" => $name,
      "environment" => $environment,
      "p12Certificat" => $p12Certificate_path,
      "provider" => "apple"
    );
    return $this->post($endpoint, array(), $data);
  }

  public function createNewNotifierAndroid($name, $apiKey) {
    $endpoint = "notifiers";
    $data = array(
      "name" => $name,
      "apiKey" => $apiKey,
      "provider" => "google"
    );
    return $this->post($endpoint, array(), $data);
  }

  public function createNotification() {
    return new Notification();
  }

  public function scheduleNotification(Notification $notification) {
    $notifier_name = $notification->get_notifier_name();
    $message = $notification->get_message();
    $delivery_time = $notification->get_delivery_time();
    $recipients_list = $notification->get_recipients_list();
    $recipient_type = $notification->get_recipient_type();

    //we are trying to make this (where notifierName is the name of the notifier:
    // { "payloads": { notifierName: "msg" }, "deliver":timestamp }
    $body = array('payloads' => array($notifier_name => $message, 'deliver' => $delivery_time));

    switch ($recipient_type) {
      case GROUPS:
        $type = 'groups/';
        break;
      case USERS:
        $type = 'users/';
        break;
      case DEVICES:
        $type = 'devices/';
        break;
      default:
        $type = 'devices/';
        $recipients_list = array(';ql=');
    }

    //schedule notification
    if (count($recipients_list) > 0) {
      foreach ($recipients_list as $recipient) {
        $endpoint = $type . $recipient . '/notifications';
        $result = $this->post($endpoint, array(), $body);
        if ($result->get_error()) {
          $notification->log_error($result->get_error());
        }
      }
    }
    return $notification;
  }


}



