# asset

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
    <td>Type of entity, in this case 'asset'</td>
  </tr>
  <tr>
    <td>name</td>
    <td>string</td>
    <td>Optional. Asset name</td>
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
    <td>owner</td>
    <td>UUID</td>
    <td>Required. UUID of the user entity that owns the asset</td>
  </tr>
  <tr>
    <td>path</td>
    <td>string</td>
    <td>Required. Relative path to the asset</td>
  </tr>
  <tr>
    <td>content-type</td>
    <td>string</td>
    <td>MIME media type that describes the asset (see media types)</td>
  </tr>
  <tr>
    <td>metadata</td>
    <td>object</td>
    <td>A nested, JSON-formatted object that provides the relative path to the asset entity, as well as additional data entities associated with the asset. The following properties are included in metadata: path: Path to retrieve the asset entity</td>
  </tr>
</table>
