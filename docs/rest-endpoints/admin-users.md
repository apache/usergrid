# Admin-users


<!-- DO NOT EDIT THIS GENERATED FILE -->

<table class='usergrid-table rest-endpoints-table'>
  <tr>
    <th>End-Point</th>
    <th>Method</th>
    <th>Content-type</th>
    <th>Description</th>
    <th>Detail</th>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/management/users</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Create an admin user</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>/management/users/{user|username|email|uuid}</td>
    <td>PUT</td>
    <td>application/json</td>
    <td>Update an admin user</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/management/users/{user|username|email|uuid}</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Get an admin user</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>/management/users/{user|username|email|uuid}/ password</td>
    <td>PUT</td>
    <td>application/json</td>
    <td>Set an admin user's password</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/managementusers/resetpw</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Initiate the reset of an admin user's password</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>/management/users/resetpw</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Complete the reset of an admin user's password</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/management/users/{user|username|email|uuid}/activate? token={token}&confirm={confirm_email}</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Activate an admin user</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>/management/users/{user|username|email|uuid}/reactivate</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Reactivate an admin user</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>/management/users/{user|username|email|uuid}/feed</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Get an admin user's feed</td>
    <td>Detail</td>
  </tr>
</table>
