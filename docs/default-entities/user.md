# User

<!-- DO NOT EDIT THIS GENERATED FILE -->

<table class='usergrid-table entities-table'>
  <tr>
    <th>uuid</th>
    <th>UUID</th>
    <th>Unique entity ID</th>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>type</td>
    <td>string</td>
    <td>Type of entity, in this case 'user'</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>created</td>
    <td>long</td>
    <td>UTC timestamp in milliseconds of when the entity was created</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>modified</td>
    <td>long</td>
    <td>UTC timestamp in milliseconds of when the entity was last modified</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>username</td>
    <td>string</td>
    <td>Required. Valid and unique username</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>activated</td>
    <td>boolean</td>
    <td>Whether the user account is activated. Set to 'true' by default when the user is created.</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>metadata</td>
    <td>object</td>
    <td>A nested, JSON-formatted object that provides the relative path to the user entity, as well as additional data entities associated with the user. The following properties are included in metadata: path: Path to retrieve the user entity sets: Nested object that contains the 'rolenames' and 'permissions' properties. rolenames: Deprecated. Use /users/\/roles|username\> instead. Path to retrieve a list of roles associated with the user. permissions: Path to retrieve a list of all permissions directly associated with the user. If the user is associated with a role or group, the list will not include permissions associated with those entities. collections: Nested object that contains paths to data entity collections associated with the user. activities: Activity entities associated with the user devices: Device entities associated with the user feed: A feed of all activities published by the user groups: Group entities associated with the user roles: Role entities associated with the user following: Users that the user is following followers: Users that are following the user</td>
  </tr>
</table>
