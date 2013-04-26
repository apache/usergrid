<?php
/**
 * @file
 * Allows CRUD operations on Usergrid Entities, including Users.
 *
 * @author Daniel Johnson <djohnson@apigee.com>
 * @since 26-Apr-2013
 */

namespace Apigee\Usergrid;

class Entity {

  private $client;
  private $data;

  public function __construct(Client $client, $data = array()) {
    $this->client = $client;
    $this->data = $data;
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

    $response = $this->client->request($type, $method, $data);

    if ($response->error) {
      $this->client->write_log('Could not save entity.');
    }
    else {
      if (!empty($response->data['entities'])) {
        $this->set($response->data['entities'][0]);
      }
      $need_password_change = (
        $this->get('type') == 'user'
          && !empty($entity_data['oldPassword'])
          && !empty($entity_data['newPassword'])
      );
      if ($need_password_change) {
        $pw_data = array(
          'oldpassword' => $entity_data['oldPassword'],
          'newpassword' => $entity_data['newPassword']
        );
        $response = $this->client->request("$type/password", 'PUT', $pw_data);
        if ($response->error) {
          $this->client->write_log('Could not update user\'s password.');
        }
        $this->set('oldpassword', NULL);
        $this->set('newpassword', NULL);
      }
    }
    return $response;
  }

  public function fetch() {
    $type = $this->get('type');
    $uuid = $this->get('uuid'); // may be NULL
    if (!empty($uuid)) {
      $type .= "/$uuid";
    }
    else {
      if ($type == 'user') {
        $username = $this->get('username');
        if (!empty($username)) {
          $type .= "/$username";
        }
        else {
          $error = 'no_name_specified';
          $this->client->write_log($error);
          return (object)array('error' => $error, 'data' => array());
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
          return (object)array('error' => $error, 'data' => array());
        }

      }
    }
    $response = $this->client->request($type);
    if ($response->error) {
      $this->client->write_log('Could not get entity.');
    }
    else {
      $data = $response->data;
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
    $type = $this->get('type');
    $uuid = $this->get('uuid');
    if (Client::is_uuid($uuid)) {
      $type .= "/$uuid";
    }
    else {
      $error = 'Error trying to delete object: No UUID specified.';
      $this->client->write_log($error);
      return (object)array('error' => $error, 'data' => array());
    }

    $response = $this->client->request($type, 'DELETE');
    if ($response->error) {
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