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

  public function request($request) {

    $method = $request->get_method();
    $endpoint = $request->get_endpoint();
    $body = $request->get_body();
    $query_string_array = $request->get_query_string_array();
    if ($this->get_oauth_token()) {
      $query_string_array['access_token'] = $this->get_oauth_token();
      /* //could also use headers for the token
      xhr.setRequestHeader("Authorization", "Bearer " + self.getToken());
      xhr.withCredentials = true;
      */
    }
    foreach($query_string_array as $key => $value) {
    	$query_string_array[$key] = urlencode($value);
		}
		$query_string = http_build_query($query_string_array);
    $url = '';
    if ($request->get_management_query()){
      $url = $this->url.'/'.$endpoint;
    } else {
      $url = $this->url.'/'.$this->org_name.'/'.$this->app_name.'/'.$endpoint;
    }

    //append params to the path
    if ($query_string) {
      $url .= '?' . $query_string;
    }
    $curl = curl_init($url);

    if ($method == 'POST' || $method == 'PUT' || $method == 'DELETE'){
      curl_setopt($curl, CURLOPT_CUSTOMREQUEST, $method);
    }
    if ($method == 'POST' || $method == 'PUT') {
      $body = json_encode($body);
      curl_setopt($curl, CURLOPT_HTTPHEADER, array ('Content-Length: '.strlen($body), 'Content-Type: application/json'));
      curl_setopt($curl, CURLOPT_POSTFIELDS, $body);
    }


    curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($curl, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($curl, CURLOPT_FOLLOWLOCATION, false);
    curl_setopt($curl, CURLOPT_MAXREDIRS, 10);
    curl_setopt($curl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);


    $response = curl_exec($curl);
    $meta = curl_getinfo($curl);

    curl_close($curl);

    $response_array = @json_decode($response, TRUE);
    $response_obj = new Response();
    $response_obj->set_curl_meta($meta);
    $response_obj->set_data($response_array);

    if ($meta['http_code'] != 200)   {
			//there was an api error
    	$error = $response_array['error'];
    	$description = isset($response_array['error_description'])?$response_array['error_description']:'';
    	$description = isset($response_array['exception'])?$response_array['exception']:$description;
      $this->write_log('Error: '.$meta['http_code'].' error:'.$description);
			$response_obj->set_error(true);
    	$response_obj->set_error_message($description);
    } else {
      $response_obj->set_error(false);
      $response_obj->set_error_message(false);
    }

    return $response_obj;
  }

  public function get($endpoint, $query_string) {

    $request = new Request();
    $request->set_method('GET');
    $request->set_endpoint($endpoint);
    $request->set_query_string_array($query_string);

    $response = $this->request($request);

    return $response;
  }

  public function post($endpoint, $query_string, $body) {

    $request = new Request();
    $request->set_method('POST');
    $request->set_endpoint($endpoint);
    $request->set_query_string_array($query_string);
    $request->set_body($body);

    $response = $this->request($request);

    return $response;
  }

  public function put($endpoint, $queryString, $body) {

    $request = new Request();
    $request->set_method('PUT');
    $request->set_endpoint($endpoint);
    $request->set_query_string_array($queryString);
    $request->set_body($body);

    $response = $this->request($request);

    return $response;
  }

  public function delete($endpoint, $queryString) {
    $request = new Request();
    $request->set_method('DELETE');
    $request->set_endpoint($endpoint);
    $request->set_query_string_array($queryString);

    $response = $this->request($request);

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
    $entity = new Entity($this, $entity_data);
    $response = $entity->fetch();

    $ok_to_save = (
      ($response->error && ('service_resource_not_found' == $response->error || 'no_name_specified' == $response->error ))
      ||
      (!$response->error && array_key_exists('getOnExist', $entity_data) && $entity_data['getOnExist'])
    );

    //var okToSave = (err && 'service_resource_not_found' === data.error || 'no_name_specified' === data.error || 'null_pointer' === data.error) || (!err && getOnExist);

    if ($ok_to_save) {
      $entity->set($entity_data);
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

  public function get_collection($type, $qs = array()) {
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
    $response = $this->POST('token', array(), $body);
    if ($response->error) {
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

  /*
 * A public facing helper method for signing up users
 *
 * @method signup
 * @public
 * @params {string} username
 * @params {string} password
 * @params {string} email
 * @params {string} name
 * @return {object} entity
 */
	public function signup($username, $password, $email, $name) {
		$data = array('type' => 'users', 'username' => $username, 'password'=>$password, 'email'=>$email, 'name'=>$name);
		return $this->create_entity($data);
	}

  public function get_logged_in_user() {
		$data = array('username' => 'me', 'type' => 'users');
		return $this->get_entity($data);
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
