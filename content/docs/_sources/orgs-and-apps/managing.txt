# Organization & application management
Your application can use the App Services API to request a variety of management operations on App Services resources. For example, your application can request an access token to use in operations on entities and collections. Or it can create an organization to contain the applications, entities, and collections for a company, team, or project.

Your application makes requests through the API using HTTP methods such as GET, POST, PUT, and DELETE, and specifies the pertinent resource URL. For management operations, the URL begins with ``/management/``. See [Using the API](../getting-started/using-the-api.html) for general usage information, such as how to construct an API request.

The following table lists and describes resources accessible through the App Services API on which your application can perform management operations. Click on a resource for further details about the resource and its methods.

<table class="usergrid-table">
<tr>
  <th>
  Resource
  </th>
  <th>
  Description
  </th>
</tr>
<tr>
  <td>
  [Access Token](../security-and-auth/authenticating-users-and-application-clients.html)
  </td>
  <td>
  Carries the credentials and authorization information needed to access other resources through the Usergrid API.
  </td>
</tr>
<tr>
  <td>
  [Client Authorization](../security-and-auth/authenticating-api-requests.html)
  </td>
  <td>
  Authorizes the client.
  </td>
</tr>
<tr>
  <td>
  [Organization](organization.html)
  </td>
  <td>
  The highest level structure in the Usergrid data hierarchy.
  </td>
</tr>
<tr>
  <td>
  [Admin User](adminuser.html)
  </td>
  <td>
  A user that has full access to perform any operation on all organization accounts of which the user is a member.
  </td>
</tr>
</table>

