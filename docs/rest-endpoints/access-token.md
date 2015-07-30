# Access-token


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
    <td>"/management/token '{"grant_type":"client_credentials","client_id":"{client_id}","client_secret":"{client_secret}"}'"</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Obtain an access token (access type = organization)</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>"/management/token '{"grant_type":"password","username":"{username}",:"password":"{password}"}'"</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Obtain an access token (access type = admin user)</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>"/{org_id}/{app_id}/token '{"grant_type":"client_credentials","client_id":"{client_id}","client_secret":"{client_secret}"}'"</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Obtain an access token (access type = application)</td>
    <td>Detail</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>"/{org_id}/{app_id}/token '{"grant_type":"password","username":"{username}","password":"{password}"}'"</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Obtain an access token (access type = application user)</td>
    <td>Detail</td>
  </tr>
</table>
