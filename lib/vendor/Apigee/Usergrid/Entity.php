<?php
/**
 * @file
 * Allows CRUD operations on Usergrid Entities, including Users.
 *
 * @author Daniel Johnson <djohnson@apigee.com>
 * @author Rod Simpson <rod@apigee.com>
 * @since 26-Apr-2013
 */

namespace Apigee\Usergrid;

class Entity {

  private $client;
  private $data;
  private $json;

  public function __construct(Client $client, $data = array(), $json_data='') {
    $this->client = $client;
    $this->data = $data;
    $this->json_data = $json_data;
  }

  public function get_json() {
    return $this->json;
  }

  public function set_json($json) {
    $this->json = $json;
  }

  public function get($field = NULL) {
    if (!empty($field)) {
      return (isset($this->data[$field]) ? $this->data[$field] : NULL);
    }
    return $this->data;
  }

  public function set($key, $value = NULL) {
    if (is_array($key)) {
      foreach ($key as $field => $value) {
        $this->data[$field] = $value;
      }
    }
    elseif (is_string($key)) {
      if (!isset($value)) {
        if (isset($this->data[$key])) {
          unset($this->data[$key]);
        }
      }
      else {
        $this->data[$key] = $value;
      }
    }
    else {
      $this->data = array();
    }
  }

  public function save() {
    $type = $this->get('type');
    $method = 'POST';
    $uuid = $this->get('uuid');
    if (isset($uuid) && Client::is_uuid($uuid)) {
      $method = 'PUT';
      $type .= "/$uuid";
    }
    $data = array();
    $entity_data = $this->get();
    foreach ($entity_data as $key => $val) {
      switch ($key) {
        case 'metadata':
        case 'created':
        case 'modified':
        case 'type':
        case 'activated':
        case 'uuid':
          continue;
          break;
        default:
          $data[$key] = $val;
      }
    }

    if ($method == 'PUT') {
      $response = $this->client->put($type, array(), $data);
    }
    else {
      $response = $this->client->post($type, array(), $data);
    }

    $this->set_json($response->get_json());

    if ($response->get_error()) {
      $this->client->write_log('Could not save entity.');
    }
    else {
      $response_data = $response->get_data();
      if (!empty($response_data['entities'])) {
        $this->set($response_data['entities'][0]);
      }
      $need_password_change = (
        ($this->get('type') == 'user' || $this->get('type') == 'users')
        && !empty($entity_data['oldpassword'])
        && !empty($entity_data['newpassword'])
      );
      if ($need_password_change) {
        $pw_data = array(
          'oldpassword' => $entity_data['oldpassword'],
          'newpassword' => $entity_data['newpassword']
        );
        $response = $this->client->PUT("$type/password", array(), $pw_data);
        if ($response->get_error()) {
          $this->client->write_log('Could not update user\'s password.');
        }
        $this->set('oldpassword', NULL);
        $this->set('newpassword', NULL);
      }
    }
    return $response;
  }

  public function fetch() {
    $response = new Response();
    $type = $this->get('type');
    $uuid = $this->get('uuid'); // may be NULL
    if (!empty($uuid)) {
      $type .= "/$uuid";
    }
    else {
      if ($type == 'user' || $type == 'users') {
        $username = $this->get('username');
        if (!empty($username)) {
          $type .= "/$username";
        }
        else {
          $error = 'no_name_specified';
          $this->client->write_log($error);
          $response->set_error($error);
          $response->set_error_code($error);
          return $response;
        }
      }
      else {
        $name = $this->get('name');
        if (!empty($name)) {
          $type .= "/$name";
        }
        else {
          $error = 'no_name_specified';
          $this->client->write_log($error);
          $response->set_error($error);
          $response->set_error_code($error);
          return $response;
        }
      }
    }
    $response = $this->client->get($type, array());
    if ($response->get_error()) {
      $this->client->write_log('Could not get entity.');
    }
    else {
      $data = $response->get_data();
      if (isset($data['user'])) {
        $this->set($data['user']);
      }
      elseif (!empty($data['entities'])) {
        $this->set($data['entities'][0]);
      }
    }
    return $response;
  }

  public function destroy() {
    $response = new Response();
    $type = $this->get('type');
    $uuid = $this->get('uuid');
    if (Client::is_uuid($uuid)) {
      $type .= "/$uuid";
    }
    else {
      $error = 'Error trying to delete object: No UUID specified.';
      $this->client->write_log($error);
      $response->set_error($error);
      $response->set_error_code($error);
      return $response;
    }

    $response = $this->client->delete($type, array());
    if ($response->get_error()) {
      $this->client->write_log('Entity could not be deleted.');
    }
    else {
      $this->set(NULL);
    }
    return $response;
  }

  public function connect($connection, $entity) {
    // TODO
  }

  public static function get_entity_id($entity) {
    // TODO
  }

  public function get_connections($connection) {
    // TODO
  }

  public function disconnect($connection, $entity) {
    // TODO
  }

}