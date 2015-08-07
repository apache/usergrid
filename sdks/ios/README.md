# iOS SDK

##Version

Current Version: **0.9.2**

Change log:

<https://github.com/apigee/usergrid-javascript-sdk/blob/master/changelog.md>

##Overview
This open source SDK simplifies writing iOS applications that connect to App Services (Usergrid). The repo is located here:

<https://github.com/apigee/usergrid-ios-sdk>

You can download the SDK here:

* Download as a zip file: <https://github.com/apigee/usergrid-ios-sdk/archive/master.zip>
* Download as a tar.gz file: <https://github.com/apigee/usergrid-ios-sdk/archive/master.tar.gz>


To find out more about App Services, which is Apigee's hosted Usergrid solution, see:

<http://apigee.com/about/developers>

To view the Apigee App Services documentation, see:

<http://apigee.com/docs/app_services>


##Installing
Once you have downloaded the SDK, add the UGAPI folder to your project by dragging and dropping it into your project. 

**Note:** Make sure you check the "Copy items into destination group's folder", and also make sure the appropriate boxes are checked next to "Add to targets".


##Getting started
If you haven't done so, make sure you know your organization name and your application name. Your organization name will be the same as the username you signed up with.  

Within your organization, you can have multiple application namespaces.  By default, an application named "Sandbox" has been created for you to use for testing, but you can also log into the [Admin Portal](http://apigee.com/usergrid) and create an application with any name you want.

Once you have your application name, you will want to create a UGClient object like so: 

	//configure the org and app
	NSString * orgName = @"ApigeeOrg";
	NSString * appName = @"MessageeApp";

	//make new client
	usergridClient = [[UGClient alloc]initWithOrganizationId: orgName withApplicationID: appName];

Now use the usergridClient object to invoke other methods that access the API.  For example, to log a user in:

	[usergridClient logInUser:username password:password];
	UGUser *user = [usergridClient getLoggedInUser];

Or, to create a user:

	UGClientResponse *response = [usergridClient addUser:@"myusername" email:@"email@email.com" name:@"my name" password:@"mypassword"];
    if (response.transactionState == 0) {
    	//user created!
    } 


##Sample Code
If you are ready to look at a fully functional app that uses this SDK, check out our Messagee application.  It is a twitter-type app that exercises many parts of the API including: Login / Authentication, GET and POST operations, activities (tweets), and social features such as following relationships. 

The Messagee Xcode project is located here:

<https://github.com/apigee/usergrid-sample-ios-messagee>

##Running Tests

The iOS SDK unit tests are written with Nu. You'll need to install the language itself to run the tests, and use the Nukefile to create a Usergrid.framework. For installation directions go [here](https://github.com/timburks/nu)

To compile the Usergrid SDK as an Objective-C framework simple type `nuke` to build the framework and then `nuke install` to install it in the /Library/Frameworks path.

To run the unit tests written for the SDK use the command `nuke test` this will run all tests.

## Contributing
We welcome your enhancements!

Like [Usergrid](https://github.com/apigee/usergrid-node-module), the Usergrid Objective-C SDK is open source and licensed under the Apache License, Version 2.0.

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push your changes to the upstream branch (`git push origin my-new-feature`)
5. Create new Pull Request (make sure you describe what you did and why your mod is needed)

##More information
For more information on Apigee App Services, visit <http://apigee.com/about/developers>.


## Copyright
Copyright 2013 Apigee Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
