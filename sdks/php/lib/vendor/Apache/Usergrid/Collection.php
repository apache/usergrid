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
 * Allows CRUD operations on Usergrid Collections.
 *
 * @author Daniel Johnson <djohnson@apigee.com>
 * @author Rod Simpson <rod@apigee.com>
 * @since 26-Apr-2013
 */

namespace Apache\Usergrid;
require_once(dirname(__FILE__) . '/Exceptions.php');

class Collection {

  /**
   * @var \Apache\Usergrid\Client
   */
  private $client;
  /**
   * @var string
   */
  private $type;
  /**
   * @var array
   */
  private $qs;
  /**
   * @var array
   */
  private $list;
  /**
   * @var int
   */
  private $iterator;
  /**
   * @var array
   */
  private $previous;
  /**
   * @var bool|int
   */
  private $next;
  /**
   * @var null|int
   */
  private $cursor;

  /**
   * @var string|string
   */
  private $json = '';

  /**
   * Object constructor.
   *
   * @param \Apache\Usergrid\Client $client
   * @param string $type
   * @param array $qs
   */
  public function __construct(Client $client, $type, $qs = array()) {
    $this->client = $client;
    $this->type = $type;
    $this->qs = $qs;

    $this->list = array();
    $this->iterator = -1;

    $this->previous = array();
    $this->next = FALSE;
    $this->cursor = NULL;

    $this->fetch();
  }

  public function get_json() {
    return $this->json;
  }

  public function set_json($json) {
    $this->json = $json;
  }

  /**
   * @return string
   */
  public function get_type() {
    return $this->type;
  }

  /**
   * @param string $type
   */
  public function set_type($type) {
    $this->type = $type;
  }

  /**
   * @return \Apache\Usergrid\Response
   */
  public function fetch() {
    if ($this->cursor) {
      $this->qs['cursor'] = $this->cursor;
    }
    elseif (array_key_exists('cursor', $this->qs)) {
      unset($this->qs['cursor']);
    }
    $response = $this->client->get($this->type, $this->qs);
    if ($response->get_error()) {
      $this->client->write_log('Error getting collection.');
    }
    else {
      $this->set_json($response->get_json());
      $response_data = $response->get_data();
      $cursor = (isset($response_data['cursor']) ? $response_data['cursor'] : NULL);
      $this->save_cursor($cursor);
      if (!empty($response_data['entities'])) {
        $this->reset_entity_pointer();
        $count = count($response_data['entities']);
        $this->list = array();
        for ($i = 0; $i < $count; $i++) {
          $entity_data = $response_data['entities'][$i];
          if (array_key_exists('uuid', $entity_data)) {
            $entity = new Entity($this->client, $entity_data);
            $entity->set('type', $this->type);

            $this->list[] = $entity;
          }
        }
      }
    }
    return $response;
  }

  /**
   * @param array $entity_data
   * @return \Apache\Usergrid\Entity
   */
  public function add_entity($entity_data) {
    $entity = $this->client->create_entity($entity_data);
    if ($entity) {
      $this->list[] = $entity;
    }
    return $entity;
  }

  /**
   * @param \Apache\Usergrid\Entity $entity
   * @return \Apache\Usergrid\Response
   */
  public function destroy_entity(Entity $entity) {
    $response = $entity->destroy();
    if ($response->get_error()) {
      $this->client->write_log('Could not destroy entity.');
    }
    else {
      $response = $this->fetch();
    }
    return $response;
  }

  /**
   * @param string $uuid
   * @return \Apache\Usergrid\Response|bool
   * @throws \Apache\Usergrid\UGException
   */
  public function get_entity_by_uuid($uuid) {
    if (!Client::is_uuid($uuid)) {
      if ($this->client->are_exceptions_enabled()) {
        throw new UGException("Invalid UUID $uuid");
      }
      return FALSE;
    }
    $entity = new Entity($this->client, array('type' => $this->type, 'uuid' => $uuid));
    return $entity->fetch();
  }

  /**
   * @return \Apache\Usergrid\Entity|null
   */
  public function get_first_entity() {
    return (count($this->list) > 0 ? $this->list[0] : NULL);
  }

  /**
   * @return \Apache\Usergrid\Entity|null
   */
  public function get_last_entity() {
    return (count($this->list) > 0 ? $this->list[count($this->list) - 1] : NULL);
  }

  /**
   * @return bool
   */
  public function has_next_entity() {
    $next = $this->iterator + 1;
    return ($next >= 0 && $next < count($this->list));
  }

  /**
   * @return bool
   */
  public function has_prev_entity() {
    $prev = $this->iterator - 1;
    return ($prev >= 0 && $prev < count($this->list));
  }

  /**
   * @return \Apache\Usergrid\Entity|null
   */
  public function get_next_entity() {
    if ($this->has_next_entity()) {
      $this->iterator++;
      return $this->list[$this->iterator];
    }
    return NULL;
  }

  /**
   * @return \Apache\Usergrid\Entity|null
   */
  public function get_prev_entity() {
    if ($this->has_prev_entity()) {
      $this->iterator--;
      return $this->list[$this->iterator];
    }
    return NULL;
  }

  public function reset_entity_pointer() {
    $this->iterator = -1;
  }

  public function save_cursor($cursor) {
    $this->next = $cursor;
  }

  public function reset_paging() {
    $this->previous = array();
    $this->next = FALSE;
    $this->cursor = NULL;
  }

  public function has_next_page() {
    return (bool) $this->next;
  }

  public function has_prev_page() {
    return (count($this->previous) > 0);
  }

  /**
   * @return \Apache\Usergrid\Response|bool
   */
  public function get_next_page() {
    if ($this->has_next_page()) {
      array_push($this->previous, $this->cursor);
      $this->cursor = $this->next;
      $this->list = array();
      return $this->fetch();
    }
    return FALSE;
  }

  /**
   * @return \Apache\Usergrid\Response|bool
   */
  public function get_prev_page() {
    if ($this->has_prev_page()) {
      $this->next = FALSE;
      $this->cursor = array_pop($this->previous);
      $this->list = array();
      return $this->fetch();
    }
    return FALSE;
  }
  public function serialize(){
    $data = array();
    $data->type = $this->type;
    $data->qs = $this->qs;
    $data->iterator = $this->iterator;
    $data->previous = $this->previous;
    $data->next = $this->next;
    $data->cursor = $this->cursor;
    $data->list = array();
    $this->reset_entity_pointer();
    while ($this->has_next_entity()) {
        $entity = $this->get_next_entity();
        array_push($data->list, $entity->get_json());
    }
    return json_encode($data);
  }

}
