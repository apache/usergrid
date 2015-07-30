# collections

<!-- DO NOT EDIT THIS GENERATED FILE -->
<table class='usergrid-table rest-endpoints-table'>
  <tr>
    <th>End-Point</th>
    <th>Method</th>
    <th>Content-type</th>
    <th>Description</th>
    <th>Detail</th>
  </tr>
  <tr>
    <td>/{org_id}/{app_id}/</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Retrieve all collections</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/{org_id}/{app_id}/{collection}</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Create a new entity or collection</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/{org_id}/{app_id}/{collection}/ {uuid|name}</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Retrieve an entity</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/{org_id}/{app_id}/{collection}/ {uuid|name}</td>
    <td>PUT</td>
    <td>application/json</td>
    <td>Update an entity</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/{org_id}/{app_id}/{collection}/ {uuid|name}</td>
    <td>DELETE</td>
    <td>application/json</td>
    <td>Delete an entity</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/{org_id}/{app_id}/{collection}?{query}</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Query a collection</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/{org_id}/{app_id}/{collection}?{query}</td>
    <td>PUT</td>
    <td>application/json</td>
    <td>Update a collection by query</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/{org_id}/{app_id}/{collection}/{entity_id}/ {relationship}?{query}</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Query an entity's collections or connections</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/{org_id}/{app_id}/{collection}/ {first_entity_id}/{relationship}/ {second_entity_id} or /{org_id}/{app_id}/{collection}/ {first_entity_id}/{relationship}/ {second_entity_type}/{second_entity_id}</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Add an entity to a collection or create a connection</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/{org_id}/{app_id}/{collection}/ {first_entity_id}/{relationship}/ {second_entity_id} or /{org_id}/{app_id}/{collection}/ {first_entity_id}/{relationship}/ {second_entity_type}/{second_entity_id}</td>
    <td>DELETE</td>
    <td>application/json</td>
    <td>Remove an entity from a collection or delete a connection</td>
    <td>Detail</td>
  </tr>
</table>
