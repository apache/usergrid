##Overview
This is an open-source Javascript-based console application for Usergrid called the *admin portal*. The admin portal is the primary administrative user interface for working with Usergrid.

The admin portal source code is fully open source and forkable. You can easily extend the admin portal, embed it into your own applications, or inspect the code to learn best practices associated with the Usergrid API.

The admin portal source is in the Usergrid repo here:

<https://github.com/apache/incubator-usergrid/tree/master/portal>

##About the admin portal
Use the admin portal for administrative operations, including:

* Create new organizations and applications.
* View information about the current organization, such as its applications, administrators, and credentials.
* Display information about an application, such as its users, groups, collections, and roles.
* Create and modify roles to manage access to your data.
* View and modify your data, with full support for users, groups, and custom entities and collections.
* Generate and access credentials for API access.

##Running, Deploying, or Developing

For all cases, edit the "config.js" file in the root of the portal so that it points to your Usergrid:

	Usergrid.overrideUrl = 'https://api.usergrid.com/';

Change 'https://api.usergrid.com/' to the url of your Usergrid.

If you are just running the portal:

1. Install Node.js from http://nodejs.org/download/.
2. From the root directory, run `./build.sh dev`.
3. This will build and run a lightweight server. Naviate to http://localhost:3000

If you are deploying the portal to a server:

1. Install Node.js from http://nodejs.org/download/.
2. From the root directory, run `./build.sh`.
3. Check the /dist/usergrid-portal directory.  This will contain a built copy of the source code.
4. Deploy the contents to your favorite web server.

If you are developing:

1. From the root directory, run `./build.sh dev`.
2. To debug in the browser go to http://localhost:3000/index-debug.html; http://localhost:3000/ will point to the compressed files.
3. If the libraries get out of sync, run `./build.sh` again and this will run "grunt build" in the background.
4. If you then want to update bower and create a distributable copy, run "grunt build-release", check in all the built files to distribute via bower

If you want to run the e2e tests:

- From the root directory, run `./build.sh e2e`.

To version open a terminal and run 'npm version x.x.x' this will add a tag and increment the package.json.

##Using a different api location
1. You can use api from usergrid at any location by navigating to the portal and adding a querystring http://myurl/?api_url=http://someurl
2. Another option is to change the Usergrid.overrideUrl in config.js

##Displaying API calls as cURL commands
You can display the equivalent cURL syntax for each API call that is made through the Admin portal. The calls are displayed in the console area of any of the following browsers: Chrome, Internet Explorer (in the debugger), Firefox (in Firebug), and Safari.

More information on cURL can be found here:

<http://curl.haxx.se/>

You can also use the Usergrid Command Line (ugc) for terminal access to the Usergrid API. ugc provides simplified access to Usergrid. For more about ugc, see the Usergrid repo:

<https://github.com/usergrid/usergrid>

##Unit Tests
[Unit Tests](UnitTests.md)

## Contributing
We welcome your enhancements!

Like [Usergrid](http://usergrid.incubator.apache.org/), the admin portal is open source and licensed under the Apache License, Version 2.0.

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
