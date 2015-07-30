# Users


<!-- DO NOT EDIT THIS GENERATED FILE -->

<table class='usergrid-table rest-endpoints-table'>
  <tr>
    <th>End-Point</th>
    <th>Method</th>
    <th>Description</th>
    <th>Detail</th>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/{org_id}/{app_id}/users</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Create a user in the users collection</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>/{org_id}/{app_id}/users/{user}/ password</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Set a user's password or reset the user's existing password</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/{org_id}/{app_id}/users/ {uuid|username|email_address}</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Retrieve a user</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>/{org_id}/{app_id}/users/ {uuid|username}</td>
    <td>PUT</td>
    <td>application/json</td>
    <td>Update a user</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/{org_id}/{app_id}/users/{uuid|username}</td>
    <td>DELETE</td>
    <td>application/json</td>
    <td>Delete a user</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>/{org_id}/{app_id}/users?{query}</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Query to get users</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/{org_id}/{app_id}/groups/ {uuid|groupname}/users/{uuid|username}</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Add a user to a group</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>/{org_id}/{app_id}/{collection}/ {first_entity_id}/{relationship}/ {second_entity_id} or /{org_id}/{app_id}/{collection}/ {first_entity_id}/{relationship}/ {second_entity_type}/{second_entity_id}</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Add a user to a collection or create a connection</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/{org_id}/{app_id}/{collection}/ {first_entity_id}/{relationship}/ {second_entity_id} or /{org_id}/{app_id}/{collection}/ {first_entity_id}/{relationship}/ {second_entity_type}/{second_entity_id}</td>
    <td>DELETE</td>
    <td>application/json</td>
    <td>Remove a user from a collection or delete a connection</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>/{org_id}/{app_id}/users/{uuid|username}/ {relationship}?{query}</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Query a user's collections or connections</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/{org_id}/{app_id}/users/ {uuid|username}/feed</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Get a user's feed</td>
    <td>Detail</td>
  </tr>
</table>
