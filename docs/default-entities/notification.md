# notification

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
    <td>Type of entity, in this case 'notification'</td>
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
    <td>payloads</td>
    <td>string</td>
    <td>Required. The push notifications to be delivered, formatted as key-value pairs of notifier entities and messages (<notifier_name1>:<message1>, <notifier_name2>:<message2>, ...)</td>
  </tr>
  <tr>
    <td>errorMessage</td>
    <td>string</td>
    <td>Error message returned by the notification service (APNs or GCM) if the notification fails entirely</td>
  </tr>
  <tr>
    <td>scheduled</td>
    <td>bool</td>
    <td>Whether the notification is currently scheduled for delivery</td>
  </tr>
  <tr>
    <td>state</td>
    <td>string</td>
    <td>The current delivery status of the notification: FINISHED, SCHEDULED or CANCELED.</td>
  </tr>
  <tr>
    <td>metadata</td>
    <td>object</td>
    <td>A nested, JSON-formatted object that provides the relative path to the notification entity, as well as additional data entities associated with the notification. The following properties are included in metadata: path: Path to retrieve the notification object collections: Nested object that contains paths to data entity collections associated with the notification. queue: Device entities scheduled to receive the push notification receipts: Receipt entities for delivery attempts</td>
  </tr>
</table>
