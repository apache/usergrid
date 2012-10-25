##Overview
Apigee provides a Node.js SDK that simplifies the process of making API calls to App Services from within Node.js. The Apigee App Services Node.js SDK is available as an open-source project in github and we welcome your contributions and suggestions. The repository is located at:

<https://github.com/apigee/usergrid-node-js-sdk>

To find out more about Apigee App Services, see:

<http://apigee.com/about/developers>

To view the Apigee App Services documentation, see:

<http://apigee.com/docs/usergrid/>

##Getting started
The SDK consists of one primary JavaScript file, located in the project at:

  /sdk/usergrid.appSDK.js
  
With a dependency on:
  
  /SDK/XMLHttpRequest.js

Include this file at the top of your HTML file (in between the head tags):

  <script src="sdk/usergrid.appSDK.js" type="text/javascript"></script>

After you do this, you're ready to start building entities and collections to drive your app and model your data.

A minified version of the file is located here:

  /sdk/usergrid.appSDK.min.js

# Contributing
We welcome your enhancements!

Like [Usergrid](https://github.com/apigee/usergrid-stack), the Usergrid Javascript SDK is open source and licensed under the Apache License, Version 2.0.

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push your changes to the upstream branch (`git push origin my-new-feature`)
5. Create new Pull Request (make sure you describe what you did and why your mod is needed)

##Sample apps
The SDK project includes two simple apps.  

The first is a simple app called Dogs that creates a list of dogs.   The app uses App Services to retrieve a collection of dog entities. The app illustrates how to page through the results, and how to create a new entity.

The second is an app that exercises the 4 REST methods of the api: GET, POST, PUT, and DELETE.  These two apps provide different functionality and will help you learn how to use the Javascript SDK to make your own amazing apps!

For a more complex sample app, check out the Messagee app:

<https://github.com/apigee/usergrid-sample-html5-messagee>



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