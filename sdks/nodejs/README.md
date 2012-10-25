##Overview
Apigee provides a Node.js SDK that simplifies the process of making API calls to App Services from within Node.js. The Apigee App Services Node.js SDK is available as an open-source project in github and we welcome your contributions and suggestions. The repository is located at:

<https://github.com/apigee/usergrid-node-js-sdk>

To find out more about Apigee App Services, see:

<http://apigee.com/about/developers>

To view the Apigee App Services documentation, see:

<http://apigee.com/docs/usergrid/>

##Getting started
After installing Node on your system, navigate to the directory where you put the code repo and run the following command:

  node index.js
  
This will start the node server. If it was successful, you should see the following on the command line:

  Server has started.
  Server running at http://127.0.0.1:8888/

If you do, you will then be able to enter the URL into a browser:

  http://127.0.0.1:8888/
  
This will bring up the All Calls app, which presents you with the option to run any of the standard calls, GET, POST, PUT, or DELETE, as well as make a sample login call.  Default values have been specified for the form fields under each call.

The best way to learn how the code works is to spend some time reviewing the sample project.  Node.js presents a wide array of options when deciding how to set up your application.  We have tried to make this example as simple and clear as possible. 

To get you started, please note that the SDK consists of one primary JavaScript file, located in the project at:

  /sdk/usergrid.appSDK.js
  
With a dependency on:
  
  /SDK/XMLHttpRequest.js

In the root directory, you will see the index.js file.  This is the main entry point of the application.  From there, calls go to the server.js file, and are then routed through the router.js file to the controller.js file, and finally the view.js file is called (whew!).  So the call order is like this:

1. index.js
2. server.js
3. router.js
4. controller.js
5. view.js

The API calls are all triggered in the controller.js file, in the "main" function.  Depending on the querydata parameter, the appropriate function will be called.

##Session Management
One of the first key features of this SDK is session management. Node.js does not implement any type of session management out of the box.  To over come this, we added session management similar to what is used in PHP.  

The SDK uses a combination of local file storage and cookies to keep user data safe, yet highly available.  Each new user is given a new session id, and a file of the same name is created for them.  By default, these files are stored in the /tmp directory.  This value can be overridden if needed:

	sdk.Usergrid.session.set_session_dir('/path/to/your/session/directory');
	
Currently there is no garbage collection for session files (see next section).  We welcome any contributions!

##Possible enhancements
There are a few items that would likely be needed in a production environment:

1.  Cron job to remove old session files. 
2.  Greater precision on filenames of session files.  Using the epoch time to get the millisecond value works great for a demo, but on a high traffic site, it is possible that there could be name collisions.


## Contributing
We welcome your enhancements!

Like [Usergrid](https://github.com/apigee/usergrid-stack), the Usergrid Javascript SDK is open source and licensed under the Apache License, Version 2.0.

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push your changes to the upstream branch (`git push origin my-new-feature`)
5. Create new Pull Request (make sure you describe what you did and why your mod is needed)

##More information
For more information on Apigee App Services, visit <http://apigee.com/about/developers>.

## Copyright
Copyright 2012 Apigee Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.