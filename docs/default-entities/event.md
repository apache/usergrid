# event

<!-- DO NOT EDIT THIS GENERATED FILE -->
<table class='usergrid-table rest-endpoints-table'>
  <tr>
    <th>uuid</th>
    <th>UUID</th>
    <th>Unique entity ID</th>
  </tr>
  <tr>
    <td>type</td>
    <td>String</td>
    <td>Type of entity, in this case 'event'</td>
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
    <td>timestamp</td>
    <td>long</td>
    <td>Required. UTC timestamp in milliseconds of when the application event occurred</td>
  </tr>
  <tr>
    <td>user</td>
    <td>UUID</td>
    <td>Optional. UUID of application user that posted the event</td>
  </tr>
  <tr>
    <td>group</td>
    <td>UUID</td>
    <td>Optional. UUID of application group that posted the event</td>
  </tr>
  <tr>
    <td>category</td>
    <td>string</td>
    <td>Optional. Category used for organizing similar events</td>
  </tr>
  <tr>
    <td>counters</td>
    <td>map</td>
    <td>Optional. Counter used for tracking number of similar events</td>
  </tr>
  <tr>
    <td>message</td>
    <td>string</td>
    <td>Optional. Message describing event. Will be null if no message is specified</td>
  </tr>
  <tr>
    <td>metadata</td>
    <td>object</td>
    <td>A nested, JSON-formatted object that provides the relative path to the event entity, as well as additional data entities associated with the event. The following properties are included in metadata: path: Path to retrieve the event entity</td>
  </tr>
</table>
