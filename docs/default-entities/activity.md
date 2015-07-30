# Activity

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
    <td>Type of entity, in this case 'activity'</td>
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
    <td>actor</td>
    <td>ActivityObject</td>
    <td>Required. Entity that performed the 'action' of the activity (see JSON Activity Streams 1.0 specification). By default, the UUID of the user who performed the action is recorded as the value of the 'uuid' property of this object.</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>verb</td>
    <td>string</td>
    <td>Required. The action performed by the user (for example, post)</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>published</td>
    <td>long</td>
    <td>Required. UTC timestamp in milliseconds of when the activity was published</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>content</td>
    <td>string</td>
    <td>Optional. Description of the activity</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>icon</td>
    <td>MediaLink</td>
    <td>Optional. Visual representation of a media link resource (see JSON Activity Streams 1.0 specification)</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>category</td>
    <td>string</td>
    <td>Optional. Category used to organize activities</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>published</td>
    <td>long</td>
    <td>Optional. UTC timestamp in milliseconds when the activity was published</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>object</td>
    <td>ActivityObject</td>
    <td>Optional. Object on which the action is performed (see JSON Activity Streams 1.0 specification)</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>title</td>
    <td>string</td>
    <td>Optional. Title or headline for the activity</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>metadata</td>
    <td>object</td>
    <td>A nested, JSON-formatted object that provides the relative path to the activity entity, as well as additional data entities associated with the activity. The following properties are included in metadata: path: Path to retrieve the activity entity, including the UUID of the user entity associated with the activity and the UUID of the activity entity</td>
  </tr>
</table>
