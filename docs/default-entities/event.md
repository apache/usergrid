# Event

<!-- DO NOT EDIT THIS GENERATED FILE -->

<table class='usergrid-table entities-table'>
  <tr>
    <th>uuid</th>
    <th>UUID</th>
    <th>Unique entity ID</th>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>type</td>
    <td>String</td>
    <td>Type of entity, in this case 'event'</td>
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
    <td>timestamp</td>
    <td>long</td>
    <td>Required. UTC timestamp in milliseconds of when the application event occurred</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>user</td>
    <td>UUID</td>
    <td>Optional. UUID of application user that posted the event</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>group</td>
    <td>UUID</td>
    <td>Optional. UUID of application group that posted the event</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>category</td>
    <td>string</td>
    <td>Optional. Category used for organizing similar events</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>counters</td>
    <td>map</td>
    <td>Optional. Counter used for tracking number of similar events</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>message</td>
    <td>string</td>
    <td>Optional. Message describing event. Will be null if no message is specified</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>metadata</td>
    <td>object</td>
    <td>A nested, JSON-formatted object that provides the relative path to the event entity, as well as additional data entities associated with the event. The following properties are included in metadata: path: Path to retrieve the event entity</td>
  </tr>
</table>
