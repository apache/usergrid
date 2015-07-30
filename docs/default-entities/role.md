# role

<!-- DO NOT EDIT THIS GENERATED FILE -->
<table class='usergrid-table rest-endpoints-table'>
  <tr>
    <th>uuid</th>
    <th>UUID</th>
    <th>Unique entity ID</th>
  </tr>
  <tr>
    <td>type</td>
    <td>string</td>
    <td>Type of entity, in this case 'role'</td>
  </tr>
  <tr>
    <td>name</td>
    <td>string</td>
    <td>Optional. Unique name that identifies the role</td>
  </tr>
  <tr>
    <td>created</td>
    <td>long</td>
    <td>UTC timestamp in milliseconds of when the entity was created</td>
  </tr>
  <tr>
    <td>modified</td>
    <td>long</td>
    <td>UTC timestamp in milliseconds of when the entity was last modified</td>
  </tr>
  <tr>
    <td>roleName</td>
    <td>string</td>
    <td>Identical to the value of the 'name' property by default</td>
  </tr>
  <tr>
    <td>title</td>
    <td>string</td>
    <td>Identical to the value of the 'name' property by default</td>
  </tr>
  <tr>
    <td>inactivity</td>
    <td>string</td>
    <td>The amount of time, in milliseconds, that a user or group associated with the role can be inactive before they lose the permissions associated with that role. By default, 'inactivity' is set to 0 so that the user/group never loses the role.</td>
  </tr>
  <tr>
    <td>metadata</td>
    <td>object</td>
    <td>A nested, JSON-formatted object that provides the relative path to the role entity, as well as additional data entities associated with the role. The following properties are included in metadata: path: Path to retrieve the role entity sets: Nested object that contains the 'permissions' property. permissions: Path to retrieve a list of all permissions associated with the role. collections: Nested object that contains paths to data entity collections associated with the role. groups: Group entities associated with the role users: User entities associated with the role</td>
  </tr>
</table>
