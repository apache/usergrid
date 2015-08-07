# Creating a new application

## Creating an application
You can use the admin portal to create applications. An application represents the data associated with your app. Through an application, you handle the entities associated with your app, including users, devices, events, and so on.

To create a new application with the admin portal:

1. In the admin portal, from the dropdown menu at the far top left, select the organization to which you will add the new application.
1. Click the ADD NEW APP button, located at the top of the screen, next to the application drop-down.
1. In the dialog box, enter a new application name, then click the Create button. Your new application will automatically be selected in the applications drop-down menu.

Applications can also be created programatically with a ``POST`` request to the API. For more, see [Application](../orgs-and-apps/application.html).

## Securing an application
If this is going to be a production application, be sure to configure security roles that provide only the level of access your users will need. For more on security, see [Security Best Practices](../security-and-auth/securing-your-app.html)
.