<?php
/**
 * @file
 * Allows CRUD operations on Usergrid Collections.
 *
 * @author Daniel Johnson <djohnson@apigee.com>
 * @author Rod Simpson <rod@apigee.com>
 * @since 26-Apr-2013
 */

namespace Apigee\Usergrid;
require_once(dirname(__FILE__) . '/Exceptions.php');

class Collection {

  /**
   * @var \Apigee\Usergrid\Client
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
   * @param \Apigee\Usergrid\Client $client
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
   * @return \Apigee\Usergrid\Response
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
   * @return \Apigee\Usergrid\Entity
   */
  public function add_entity($entity_data) {
    $entity = $this->client->create_entity($entity_data);
    if ($entity) {
      $this->list[] = $entity;
    }
    return $entity;
  }

  /**
   * @param \Apigee\Usergrid\Entity $entity
   * @return \Apigee\Usergrid\Response
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
   * @return \Apigee\Usergrid\Response|bool
   * @throws \Apigee\Usergrid\UGException
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
   * @return \Apigee\Usergrid\Entity|null
   */
  public function get_first_entity() {
    return (count($this->list) > 0 ? $this->list[0] : NULL);
  }

  /**
   * @return \Apigee\Usergrid\Entity|null
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
   * @return \Apigee\Usergrid\Entity|null
   */
  public function get_next_entity() {
    if ($this->has_next_entity()) {
      $this->iterator++;
      return $this->list[$this->iterator];
    }
    return NULL;
  }

  /**
   * @return \Apigee\Usergrid\Entity|null
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
   * @return \Apigee\Usergrid\Response|bool
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
   * @return \Apigee\Usergrid\Response|bool
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

}