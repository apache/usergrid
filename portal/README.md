##Version

Current Version: **0.11.1**

Change log:

<https://github.com/apigee/usergrid-portal/blob/master/changelog.md> 


##Overview
This is an open-source Javascript-based console application for Usergrid called the *Admin portal*. The Admin portal is the primary administrative user interface for working with Usergrid. 

The Admin portal is available online at <https://apigee.com/usergrid/>. The Admin portal source code is fully open source and forkable. You can easily extend the Admin portal, embed it into your own applications, or inspect the code to learn best practices associated with the Usergrid API.

The Admin portal repo is located here:

<https://github.com/apigee/usergrid-portal>

You can download the Admin portal code here:

* Download as a zip file: <https://github.com/apigee/usergrid-portal/archive/master.zip>
* Download as a tar.gz file: <https://github.com/apigee/usergrid-portal/archive/master.tar.gz>

To find out more about Apigee App Services, a free, hosted version of Usergrid, see:

<http://apigee.com/about/developers>

To view the Apigee App Services documentation, see:

<http://apigee.com/docs/app_services>


##About the Admin portal
Use the Admin portal for administrative operations, including:

* Create new organizations and applications.
* View information about the current organization, such as its applications, administrators, and credentials.
* Display information about an application, such as its users, groups, collections, and roles.
* Create and modify roles to manage access to your data.
* View and modify your data, with full support for users, groups, and custom entities and collections. 
* Generate and access credentials for API access.


##Navigating the Admin portal

The Admin portal interface displays a variety of pages that display information and enable you to perform management
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


You can display any of these pages by clicking its respective item in the left sidebar menu of the Admin portal.

###Account Home
When you log in to the Admin portal, you are presented with a home page for managing the applications and data for your organization. 
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
For details, see Displaying Usergrid API calls as Curl commands:

<http://apigee.com/docs/usergrid/content/displaying-app-services-api-calls-curl-commands>

More information on cURL can be found here:

<http://curl.haxx.se/>

## Contributing
We welcome your enhancements!

Like [Usergrid](https://github.com/apigee/usergrid-node-module), the Admin portal is open source and licensed under the Apache License, Version 2.0.

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push your changes to the upstream branch (`git push origin my-new-feature`)
5. Create new Pull Request (make sure you describe what you did and why your mod is needed)

##More information
For more information on App Services, Apigee's free hosted version of Usergrid, visit <http://apigee.com/about/developers>.

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