##Overview
This is an open-source Javascript-based console application for Usergrid called the *admin portal*. The admin portal is the primary administrative user interface for working with Usergrid.

The admin portal source code is fully open source and forkable. You can easily extend the admin portal, embed it into your own applications, or inspect the code to learn best practices associated with the Usergrid API.

The admin portal source is in the Usergrid repo here:

<https://github.com/usergrid/usergrid>

##About the admin portal
Use the admin portal for administrative operations, including:

* Create new organizations and applications.
* View information about the current organization, such as its applications, administrators, and credentials.
* Display information about an application, such as its users, groups, collections, and roles.
* Create and modify roles to manage access to your data.
* View and modify your data, with full support for users, groups, and custom entities and collections.
* Generate and access credentials for API access.

##Navigating the admin portal

The admin portal interface displays a variety of pages that display information and enable you to perform management
actions. These include:

* Account home
* Application dashboard
* Users
* Groups
* Roles
* Activities
* Collections
* Analytics
* Properties
* Shell

You can display any of these pages by clicking its respective item in the left sidebar menu of the admin portal.

###Account Home
When you log in to the admin portal, you are presented with a home page for managing the applications and data for your organization.

The home page displays:

* Applications associated with the currently selected organization
* Administrators that are part of that organization
* API credentials for the organization
* Activities performed recently by administrators
* A menu for building, organizing, and managing application content

###Application dashboard
The Application Dashboard shows a variety of statistical data for the selected application. You can see the activity level, the total number of entities, and other vital statistics for monitoring application health as well as quota limits.

###Users
The Users page lists the user entities created in the current application. You can add or delete users. You can also edit various properties of a user entity such as the user's name or address.

###Groups
The Groups page lists the groups created in the current application. You can add or delete groups. You can also edit some properties of a group such as the group's display name.

###Roles
The Roles page lists the roles defined for the current application. You can add or delete roles. You can also specify and update the permissions for a role.

###Activities
The Activities page lists the activities posted in an application. You can view when the activity was posted, who posted the activity, and the content of the activity. You can also search for activities by content or actor.

###Collections
The Collections page lists the collections created in the current application. You can also search for, add, update, or deleted collections.

###Analytics
Use this page to collect and analyze Usergrid usage data such as the number of times a particular collection has been accessed over a period of time.
You can specify parameters for data collection, including what data points you'd like to collect, over what time period, and at what resolution.
When you click the Generate button, the results are displayed in tabular form and graphically in the lower portion of the page.

###Properties
The Properties page lists the credentials (Client ID and Client Secret) for the current application. You can regenerate credentials for the application from this page.

###Shell
The Shell page gives you a simple way to get started using the Usergrid API. It provides a command-line environment within your web browser for trying out Usergrid API calls interactively.

##Displaying API calls as cURL commands
You can display the equivalent cURL syntax for each API call that is made through the Admin portal. The calls are displayed in the console area of any of the following browsers: Chrome, Internet Explorer (in the debugger), Firefox (in Firebug), and Safari.

More information on cURL can be found here:

<http://curl.haxx.se/>

You can also use the Usergrid Command Line (ugc) for terminal access to the Usergrid API. ugc provides simplified access to Usergrid. For more about ugc, see the Usergrid repo:

<https://github.com/usergrid/usergrid>

##Deploying or Developing

If you are just deploying:

1. Install Node.js from http://nodejs.org/download/.
2. Install Grunt with `sudo npm install grunt-cli -g`
3. From the root directory, run `./build.sh`.
4. This will create a directory in the root called dist. In dist is a zip file called appsvc-ui.zip. Unzip and deploy to your favorite web server.

If you are developing:

1. From the root directory, run `./build.sh`.
2. To monitor and build the performance code => run `grunt --gruntfile Perf-Gruntfile.js dev;`. This will need to continue running in terminal as you are developing.
3. To monitor and build the portal code base => run `grunt dev;`. This will open a browser with http://localhost:3000/index-debug.html.
4. To debug in the browser go to http://localhost:3000/index-debug.html; http://localhost:3000/ will point to the compressed files.
5. If the libraries get out of sync, run `./build.sh` again and this will run grunt build in the background.

If you want to run the e2e tests:

- From the root directory, run `./build.sh e2e`.


To version open a terminal and run 'npm version x.x.x' this will add a tag and increment the package.json.

##Unit Tests
[Unit Tests](UnitTests.md)

## Contributing
We welcome your enhancements!

Like [Usergrid](https://github.com/apigee/usergrid-node-module), the admin portal is open source and licensed under the Apache License, Version 2.0.

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push your changes to the upstream branch (`git push origin my-new-feature`)
5. Create new Pull Request (make sure you describe what you did and why your mod is needed)

## Usergrid is open source
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
