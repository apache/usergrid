# Roles


<!-- DO NOT EDIT THIS GENERATED FILE -->

<table class='usergrid-table rest-endpoints-table'>
  <tr>
    <th>End-Point</th>
    <th>Method</th>
    <th>Description</th>
    <th>Detail</th>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/{org_id}/{app_id}/roles</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Create a new role</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>/{org_id}/{app_id}/roles</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Get the roles in an application</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/{org_id}/{app_id}/roles/{rolename}</td>
    <td>DELETE</td>
    <td>application/json</td>
    <td>Delete a role</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>/{org_id}/{app_id}/roles/ {rolename|role_id}/permissions</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Get permissions for a role</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/{org_id}/{app_id}/roles/ {rolename|role_id}/permissions</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Add permissions to a role</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>{org_id}/{app_id}/roles/ {rolename|role_id}/permissions? permission={grant_url_pattern}</td>
    <td>DELETE</td>
    <td>application/json</td>
    <td>Delete permissions from a role</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/{org_id}/{app_id}/roles/{role_id}/ users/{uuid|username} or /{org_id}/{app_id}/users/ {uuid|username}/roles/{role_id}</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Add a user to a role</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>/{org_id}/{app_id}/roles/{role_id}/ users</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Get the users in a role</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/{org_id}/{app_id}/roles/{role_id}/ users/{uuid|username}</td>
    <td>DELETE</td>
    <td>application/json</td>
    <td>Delete a user from a role</td>
    <td>Detail</td>
  </tr>
</table>
