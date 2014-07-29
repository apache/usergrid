UsersAndGroups Sample App

This sample illustrates how to call and handle response values from JavaScript SDK methods that work with users and groups in an app services application. 

This sample shows:

- How to add users and groups to the application.
- How to list users and groups.
- How to add users to a group.

To get started, do the following:

1. Copy either the apigee.js (easier for debugging) or apigee.min.js into the app's js directory.
2. In index.html, enter your app services org name and app name in the places provided at the top of the file.
3. Open index.html in a browser to use the app.

In order for this sample to work as is, you will need to:

- Change the ORGNAME value in index.html to your app services organization name.
- Ensure that the app services application you're using provides full permission for 
user and group entities. Your sandbox is best. This sample does not feature
authentication.
