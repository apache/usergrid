<?php

class UsergridRequest {
  public $method ='';
  public $endpoint = '';
  public $queryStringArray = ''; //an array of key value pairs to be appended as the query string
  public $body = '';
  public $mQuery = false;

  public function __construct() {


  }
  public function setMethod($in){
    if ($in !== 'GET' && $in !== 'POST' && $in !== 'PUT' && $in !== 'DELETE') {
      throw new Exception('Unknown method type');
    }
    $this->method = $in;
  }
  public function getMethod(){
    return $this->method;
  }

  public function setEndpoint($in){
    $this->endpoint = $in;
  }
  public function getEndpoint(){
    return $this->endpoint;
  }

  public function setQueryStringArray($in){
    $this->queryStringArray = $in;
  }
  public function getQueryStringArray(){
    return $this->queryStringArray;
  }

  public function setBody($in){
    $this->body = $in;
  }
  public function getBody(){
    return $this->body;
  }

  public function setMQuery($in){
    //ensure we have a bool
    if ($in) {
      $this->mQuery = true;
    }
    $this->mQuery = false;
  }
  public function getMQuery(){
    return $this->mQuery;
  }

}


class UsergridClient {

  protected $url = 'https://api.usergrid.com';

  //Find your Orgname and Appname in the Admin portal (http://apigee.com/usergrid)
  public $orgName = '';
  public $appName = '';

  public $logging = false;
  public $callTimeout = 30000; //default to 30 seconds

  public $token = '';


  public $curl_opts = array(
    CURLOPT_RETURNTRANSFER => true,  // return result instead of echoing
    CURLOPT_SSL_VERIFYPEER => false, // stop cURL from verifying the peer's certificate
    CURLOPT_FOLLOWLOCATION => false,  // follow redirects, Location: headers
    CURLOPT_MAXREDIRS      => 10     // but dont redirect more than 10 times
  );


  public function __construct($orgName, $appName) {
    //make sure they have cURL installed
    if (!function_exists('curl_init')) {
      throw new Exception('You must install the cURL module to use this SDK.');
    }

    $this->orgName = $orgName;
    $this->appName = $appName;

    /*
    // only enable CURLOPT_FOLLOWLOCATION if safe_mode and open_base_dir are not in use
    if(ini_get('open_basedir') == '' && strtolower(ini_get('safe_mode')) == 'off') {
      $this->curl_opts['CURLOPT_FOLLOWLOCATION'] = true;
    }
    */



    /*
    $this->curl_opts[CURLOPT_HEADERFUNCTION] = array($this, 'handle_header');
    */
  }

  public function getToken() {
    return $this->token;
  }
  public function setToken($token) {
    $this->token = $token;
  }

  protected function createQueryString($queryStringArray) {

    return '';
  }

  protected function startCurl($method, $endpoint, $queryString){
    $url = '';
    if ($request->getMQuery()){
      $url = $this->url.'/'.$endpoint;
    } else {
      $url = $this->url.'/'.$this->orgName.'/'.$this->appName.'/'.$endpoint;
    }

    //append params to the path
    if ($queryString) {
      $url .= '?'.$queryString;
    }

    $curl = curl_init($url);
    return $curl;

  }

  //method, endpoint, body, querystring
  public function request($request) {

    $method = $request->getMethod();
    $endpoint = $request->getEndpoint();
    $body = $request->getBody();
    $queryStringArray = $request->getQueryStringArray()
    if ($this->getToken()) {
      $queryStringArray['access_token'] = this.getToken();
      /* //could also use headers for the token
      xhr.setRequestHeader("Authorization", "Bearer " + self.getToken());
      xhr.withCredentials = true;
      */
    }
    $queryString = $this->createQueryString($queryStringArray);

    $curl = startCurl($method, $endpoint, $queryString);

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

    echo "<pre>";
    print_r($meta);
    print_r($response);
    echo "</pre>";

  //  $this->checkLastResponseForError();

    return $body;


    /*

  xhr.onload = function(response) {
    //call timing, get time, then log the call
    self._end = new Date().getTime();
    if (self.logging) {
      console.log('success (time: ' + self.calcTimeDiff() + '): ' + method + ' ' + uri);
    }
    //call completed
    clearTimeout(timeout);
    //decode the response
    response = JSON.parse(xhr.responseText);
    if (xhr.status != 200)   {
      //there was an api error
      var error = response.error;
      var error_description = response.error_description;
      if (self.logging) {
        console.log('Error ('+ xhr.status +')(' + error + '): ' + error_description )
      }
      if ( (error == "auth_expired_session_token") ||
        (error == "unauthorized")   ||
        (error == "auth_missing_credentials")   ||
        (error == "auth_invalid")) {
        //this error type means the user is not authorized. If a logout function is defined, call it
        //if the user has specified a logout callback:
        if (typeof(self.logoutCallback) === 'function') {
          return self.logoutCallback(true, response);
        }
      }
      if (typeof(callback) === 'function') {
        callback(true, response);
      }
    } else {
      if (typeof(callback) === 'function') {
        callback(false, response);
      }
    }
  };

  var timeout = setTimeout(
    function() {
      xhr.abort();
      if (self._callTimeoutCallback === 'function') {
        self._callTimeoutCallback('API CALL TIMEOUT');
      } else {
        self.callback('API CALL TIMEOUT');
      }
    },
    self._callTimeout); //set for 30 seconds

  if (this.logging) {
    console.log('calling: ' + method + ' ' + uri);
  }
  if (this.buildCurl) {
    var curlOptions = {
      uri:uri,
      body:body,
      method:method
    }
    this.buildCurlCall(curlOptions);
  }
  this._start = new Date().getTime();
  xhr.send(body);

    */


  }

  public function get($endpoint, $queryString) {

    $request = new UsergridRequest();
    $request->setMethod('GET');
    $request->setEndpoint($endpoint);
    $request->setQueryStringArray($queryString);

    $response = $this->request($request);

    return $response;
  }

  public function post($endpoint, $queryString, $body) {

    $request = new UsergridRequest();
    $request->setMethod('POST');
    $request->setEndpoint($endpoint);
    $request->setQueryStringArray($queryString);
    $request->setBody($body);

    $response = $this->request($request);

    return $response;
  }

  public function put($endpoint, $queryString, $body) {

    $request = new UsergridRequest();
    $request->setMethod('PUT');
    $request->setEndpoint($endpoint);
    $request->setQueryStringArray($queryString);
    $request->setBody($body);

    $response = $this->request($request);

    return $response;
  }

  public function delete($endpoint, $queryString) {
    $request = new UsergridRequest();
    $request->setMethod('DELETE');
    $request->setEndpoint($endpoint);
    $request->setQueryStringArray($queryString);

    $response = $this->request($request);

    return $response;
  }


  private function handle_header($ch, $str) {
    if (preg_match('/([^:]+):\s(.+)/m', $str, $match) ) {
      $this->last_headers[strtolower($match[1])] = trim($match[2]);
    }
    return strlen($str);
  }



  protected function checkLastResponseForError() {
    if ( !$this->throw_exceptions)
      return;

    $meta = $this->last_response['meta'];
    $body = $this->last_response['body'];

    if (!$meta)
      return;

    $err = null;
    switch ($meta['http_code']) {
      case 400:
        throw new Pest_BadRequest($this->processError($body));
        break;
      case 401:
        throw new Pest_Unauthorized($this->processError($body));
        break;
      case 403:
        throw new Pest_Forbidden($this->processError($body));
        break;
      case 404:
        throw new Pest_NotFound($this->processError($body));
        break;
      case 405:
        throw new Pest_MethodNotAllowed($this->processError($body));
        break;
      case 409:
        throw new Pest_Conflict($this->processError($body));
        break;
      case 410:
        throw new Pest_Gone($this->processError($body));
        break;
      case 422:
        // Unprocessable Entity -- see http://www.iana.org/assignments/http-status-codes
        // This is now commonly used (in Rails, at least) to indicate
        // a response to a request that is syntactically correct,
        // but semantically invalid (for example, when trying to
        // create a resource with some required fields missing)
        throw new Pest_InvalidRecord($this->processError($body));
        break;
      default:
        if ($meta['http_code'] >= 400 && $meta['http_code'] <= 499)
          throw new Pest_ClientError($this->processError($body));
        elseif ($meta['http_code'] >= 500 && $meta['http_code'] <= 599)
          throw new Pest_ServerError($this->processError($body));
        elseif (!$meta['http_code'] || $meta['http_code'] >= 600) {
          throw new Pest_UnknownResponse($this->processError($body));
        }
    }
  }
}



class Pest_Exception extends Exception { }
class Pest_UnknownResponse extends Pest_Exception { }

/* 401-499 */ class Pest_ClientError extends Pest_Exception {}
/* 400 */ class Pest_BadRequest extends Pest_ClientError {}
/* 401 */ class Pest_Unauthorized extends Pest_ClientError {}
/* 403 */ class Pest_Forbidden extends Pest_ClientError {}
/* 404 */ class Pest_NotFound extends Pest_ClientError {}
/* 405 */ class Pest_MethodNotAllowed extends Pest_ClientError {}
/* 409 */ class Pest_Conflict extends Pest_ClientError {}
/* 410 */ class Pest_Gone extends Pest_ClientError {}
/* 422 */ class Pest_InvalidRecord extends Pest_ClientError {}

/* 500-599 */ class Pest_ServerError extends Pest_Exception {}

?>