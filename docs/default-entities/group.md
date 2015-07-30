# Group

<!-- DO NOT EDIT THIS GENERATED FILE -->

<table class='usergrid-table entities-table'>
  <tr>
    <th>uuid</th>
    <th>UUID</th>
    <th>Unique entity UUID</th>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>type</td>
    <td>string</td>
    <td>Type of entity, in this case 'group'</td>
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
    <td>path</td>
    <td>string</td>
    <td>Required. Relative path where the group can be retrieved</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>title</td>
    <td>string</td>
    <td>Optional. Display name for the group entity</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>metadata</td>
    <td>object</td>
    <td>A nested, JSON-formatted object that provides the relative path to the group entity, as well as additional data entities associated with the group. The following properties are included in metadata: path: Path to retrieve the group entity, including the group UUID sets: Nested object that contains the 'rolenames' and 'permissions' properties. rolenames: Path to retrieve a list of roles associated with the group. permissions: Path to retrieve a list of all permissions directly associated with the group. If the group is associated with a role, the list will not include permissions associated with the role entity. collections: Nested object that contains paths to data entity collections associated with the group. activities: Activity entities associated with the group feed: A feed of all activities published by users associated with the group roles: Role entities associated with the group users: User entities associated with the group</td>
  </tr>
</table>
