<?php
/**
 * @file
 * Allows CRUD operations on Usergrid Collections.
 *
 * @author Daniel Johnson <djohnson@apigee.com>
 * @since 26-Apr-2013
 */

namespace Apigee\Usergrid;

class Collection {

  private $client;
  private $type;
  private $qs;

  private $list;
  private $iterator;
  private $previous;
  private $next;
  private $cursor;

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

  public function fetch() {
    if ($this->cursor) {
      $this->qs['cursor'] = $this->cursor;
    }
    elseif (array_key_exists('cursor', $this->qs)) {
      unset($this->qs['cursor']);
    }
    $response = $this->client->request($this->type, 'GET', array(), $this->qs);
    if ($response->error) {
      $this->client->write_log('Error getting collection.');
    }
    else {
      $cursor = (isset($response->data['cursor']) ? $response->data['cursor'] : NULL);
      $this->save_cursor($cursor);
      if (!empty($response->data['entities'])) {
        $this->reset_entity_pointer();
        $count = count($response->data['entities']);
        $this->list = array();
        for ($i = 0; $i < $count; $i++) {
          $entity_data = $response->data['entities'][$i];
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

  public function add_entity($entity_data) {
    $entity = $this->client->create_entity($entity_data);
    if ($entity) {
      $this->list[] = $entity;
    }
    return $entity;
  }

  public function destroy_entity(Entity $entity) {
    $response = $entity->destroy();
    if ($response->error) {
      $this->client->write_log('Could not destroy entity.');
    }
    else {
      $response = $this->fetch();
    }
    return $response;
  }

  public function get_entity_by_uuid($uuid) {
    $entity = new Entity($this->client, array('type' => $this->type, 'uuid' => $uuid));
    return $entity->fetch();
  }

  public function get_first_entity() {
    return (count($this->list) > 0 ? $this->list[0] : NULL);
  }

  public function get_last_entity() {
    return (count($this->list) > 0 ? $this->list[count($this->list) - 1] : NULL);
  }

  public function has_next_entity() {
    $next = $this->iterator + 1;
    return ($next >= 0 && $next < count($this->list));
  }

  public function has_prev_entity() {
    $prev = $this->iterator - 1;
    return ($prev >= 0 && $prev < count($this->list));
  }

  public function get_next_entity() {
    if ($this->has_next_entity()) {
      $this->iterator++;
      return $this->list[$this->iterator];
    }
    return NULL;
  }

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
    return $this->next;
  }

  public function has_prev_page() {
    return (count($this->previous) > 0);
  }

  public function get_next_page() {
    if ($this->has_next_page()) {
      array_push($this->previous, $this->cursor);
      $this->cursor = $this->next;
      $this->list = array();
      return $this->fetch();
    }
    return FALSE;
  }

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