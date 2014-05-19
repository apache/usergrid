#!/usr/bin/env php
<?php
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @file
 * Allows CRUD operations on Usergrid Entities, including Users.
 *
 * @author Daniel Johnson <djohnson@apigee.com>
 * @author Rod Simpson <rod@apigee.com>
 * @since 26-Apr-2013
 */

namespace Apache\Usergrid;

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
    $this->set_json($response->get_json());
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
    $this->set_json($response->get_json());
    if ($response->get_error()) {
      $this->client->write_log('Entity could not be deleted.');
    }
    else {
      $this->set(NULL);
    }
    return $response;
  }

  public function connect($connection, $entity) {
    $connectee=Entity::get_entity_id($entity);
    $connecteeType=$entity->get("type");
    if(!$connectee){
      return "Error in connect. No UUID specified for connectee";
    }

    $connector=Entity::get_entity_id($this);
    $connectorType=$this->get("type");
    if(!$connector){
      return "Error in connect. No UUID specified for connector";
    }

    $endpoint = $connectorType.'/'.$connector.'/'.$connection.'/'.$connecteeType.'/'.$connectee;
    $result=$this->client->post($endpoint, array(), array());
    $error=$result->get_error();
    if($error){
      return $result->get_error_message();
    }else{
      return $result->get_data();
    }
  }

  public function disconnect($connection, $entity) {
    $connectee=Entity::get_entity_id($entity);
    $connecteeType=$entity->get("type");
    if(!$connectee){
      return "Error in disconnect. No UUID specified for connectee";
    }

    $connector=Entity::get_entity_id($this);
    $connectorType=$this->get("type");
    if(!$connector){
      return "Error in disconnect. No UUID specified for connector";
    }

    $endpoint = $connectorType.'/'.$connector.'/'.$connection.'/'.$connecteeType.'/'.$connectee;

    $result=$this->client->delete($endpoint, array(), array());
    $error=$result->get_error();
    if($error){
      return $result->get_error_message();
    }else{
      return $result->get_data();
    }
  }

  public static function get_entity_id($entity) {
      $id = false;
      if (Client::is_uuid($entity->get('uuid'))) {
        $id = $entity->get('uuid');
      } else {
        if ($type == 'users') {
          $id = $entity->get('username');
        } else if ($entity->get('name')) {
          $id = $entity->get('name');
        }
      }
      return $id;
  }

  public function get_connections($connection) {
    $connectorType = $this->get('type');
    $connector = Entity::get_entity_id($this);
    if (!$connector) {
      return;
    }

    $endpoint = $connectorType . '/' . $connector . '/' . $connection . '/';
    $result=$this->client->get($endpoint, array());

    $connected_entities = array();

    $response_data = $result->get_data();
    $length        = count($response_data['entities']);
    
    for ($i = 0; $i < $length; $i++) {
      $tmp_entity = $response_data['entities'][$i];
      if ($tmp_entity['type'] == 'user') {
          $connected_entities[$tmp_entity['username']] = $tmp_entity;
      } else {
          $connected_entities[$tmp_entity['name']]     = $tmp_entity;
      }
    }
    $this->set($connection, $connected_entities);
  }

  public function get_connecting($connection) {
    $connectorType = $this->get('type');
    $connector = Entity::get_entity_id($this);
    if (!$connector) {
      return;
    }

    $endpoint = $connectorType. '/' . $connector . '/connecting/' . $connection . '/';
    $result=$this->client->get($endpoint, array());
    return $result->get_data();
  }

}
