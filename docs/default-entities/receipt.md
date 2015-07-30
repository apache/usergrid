# receipt

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
    <td>Type of entity, in this case 'notifier'</td>
  </tr>
  <tr>
    <td>name</td>
    <td>string</td>
    <td>Optional. Notifier display name</td>
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
    <td>provider</td>
    <td>string</td>
    <td>Required. Push notification provider: apple or google</td>
  </tr>
  <tr>
    <td>environment</td>
    <td>string</td>
    <td>Required. The environment that corresponds to your app: development or production</td>
  </tr>
  <tr>
    <td>metadata</td>
    <td>object</td>
    <td>A nested, JSON-formatted object that provides the relative path to the notifier entity path: Path to retrieve the notification object</td>
  </tr>
</table>
