usergrid-sample-.net-messagee
=============================
Directions for use

1. Sign up for a free Apigee App Services account
2. In the organization that is created for you, create a user and give that user administrative privileges
3. Create an app in the organization
4. In the app, create as many test users as you like
5. Clone the following branch for the Usergrid sdk: https://github.com/apigee/usergrid-.net-sdk and build it
6. Clone this repository.  Open the Messagee solution file in the top level folder.  Add references to the .dll files located in the UsergridSdk\bin\debug folder to the project
7. Build Messagee
8. Run the solution.  On the main screen choose the Settings menu item and input the admin user credentials, the org name and the application name

At this point, the users for that app should be populated.  Choosing a particular user and then choosing the User Settings menu option will allow you to control who that user is following
Connect a few users and do some posts.  Posts are automatically routed to users who are connected as followers
