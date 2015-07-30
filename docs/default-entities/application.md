# Application

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
    <td>Type of entity, in this case 'application'</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>name</td>
    <td>string</td>
    <td>Optional. Application name</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>created</td>
    <td>long</td>
    <td>UTC timestamp in milliseconds of when the entity was created</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>modified</td>
    <td>long</td>
    <td>UTC timestamp in milliseconds of when the entity was last modified</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>accesstokenttl</td>
    <td>long</td>
    <td>Optional. Time to live value for an access token obtained within the application</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>organizationName</td>
    <td>string</td>
    <td>Name of the organization the application belongs to</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>applicationName</td>
    <td>string</td>
    <td>Name of the application</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>title</td>
    <td>string</td>
    <td>Optional. Application title</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>description</td>
    <td>string</td>
    <td>Optional. Application description</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>activated</td>
    <td>boolean</td>
    <td>Optional. Whether application is activated</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>disabled</td>
    <td>boolean</td>
    <td>Optional. Whether application is administratively disabled</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>allowOpenRegistration</td>
    <td>boolean</td>
    <td>Optional. Whether application allows any user to register</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>registrationRequiresEmailConfirmation</td>
    <td>boolean</td>
    <td>Optional. Whether registration requires email confirmation</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>registrationRequiresAdminApproval</td>
    <td>boolean</td>
    <td>Optional. Whether registration requires admin approval</td>
  </tr>
  <tr class='ug-even usergrid-table'>
    <td>notify_admin_of_new_users</td>
    <td>boolean</td>
    <td>Optional. Whether application admins should be notified of new users</td>
  </tr>
  <tr class='ug-odd usergrid-table'>
    <td>metadata</td>
    <td>object</td>
    <td>A nested, JSON-formatted object that provides a 'collections' object with the relative paths to all entity collections associated with the application. The following collections are included in metadata by default: users: Path to retrieve the /users collection groups: Path to retrieve the /groups collection folders: Path to retrieve the /folders collection events: Path to retrieve the /events collection assets: Path to retrieve the /assets collection activities: Path to retrieve the /activities collection devices: Path to retrieve the /devices collection notifiers: Path to retrieve the /notifiers collection notifications: Path to retrieve the /notifications collection receipts: Path to retrieve the /receipts collection</td>
  </tr>
</table>
