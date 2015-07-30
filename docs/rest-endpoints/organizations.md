# organizations

<!-- DO NOT EDIT THIS GENERATED FILE -->
<table class='usergrid-table rest-endpoints-table'>
  <tr>
    <th>End-Point</th>
    <th>Method</th>
    <th>Description</th>
    <th>Detail</th>
  </tr>
  <tr>
    <td>/management/organizations|orgs</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Create an organization</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/management/organizations|orgs/{org_name}|{uuid}</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Retrieve an organization</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/management/organizations|orgs/{org_name}|{uuid}/ activate?token={token}&confirm={confirm_email}</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Activate an organization</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/management/organizations|orgs/{org_name}|{uuid}/ reactivate</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Reactivate an organization</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/management/organizations|orgs/{org_name}|{uuid}/ credentials</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Generate organization client credentials</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/management/organizations|orgs/{org_name}|{uuid}/ credentials</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Retrieve organization client credentials</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/management/organizations|orgs/{org_name}|{uuid}/ feed</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Retrieve an organization's activity feed</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/management/organizations|orgs/{org_name}|{org_uuid}/ apps</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Create an organization application</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/management/organizations|orgs/{org_name}|{uuid}/ applications|apps/{app_name}|{uuid}/ credentials</td>
    <td>POST</td>
    <td>application/json</td>
    <td>Generate credentials for an organization application</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/management/organizations|orgs/{org_name}|{uuid}/ applications|apps/ {app_name}|{uuid}/credentials</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Get credentials for an organization application</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/management/organizations|orgs/{org_name}|{uuid}/ applications|apps</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Get the applications in an organization</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/management/organizations|orgs/{org_name}|{org_uuid}/ users/{username|email|uuid}</td>
    <td>PUT</td>
    <td>application/json</td>
    <td>Adding an admin user to an organization</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/management/organizations|orgs/{org_name}|{org_uuid}/ users</td>
    <td>GET</td>
    <td>application/json</td>
    <td>Getting the admin users in an organization</td>
    <td>Detail</td>
  </tr>
  <tr>
    <td>/management/organizations|orgs/{org_name}|{org_uuid}/ users/{username|email|uuid}</td>
    <td>DELETE</td>
    <td>application/json</td>
    <td>Removing an admin user from an organization</td>
    <td>Detail</td>
  </tr>
</table>
