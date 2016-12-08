# iOS SDK

##Version

Current Version: **0.9.2**

Change log:

<https://github.com/apache/usergrid/blob/master/sdks/ios/changelog.md>

##Overview
This open source SDK simplifies writing iOS applications that connect to App Services (Usergrid). The repo is located here:

<https://github.com/apache/usergrid/tree/master/sdks/ios>

You can download the SDK as part of the Usergrid stack here:

* Download as a tar.gz file: <http://usergrid.apache.org/releases/>

To find out more about how to use the iOS SDK within Usergrid, see:

<http://usergrid.apache.org/docs/sdks/ios-new.html>

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
If you are ready to look at a fully functional app that uses this SDK, check out the example Apigee Messagee application. It is a twitter-type app that exercises many parts of the API including: Login / Authentication, GET and POST operations, activities (tweets), and social features such as following relationships. 

The Messagee Xcode project is located here:

<https://github.com/apigee/usergrid-sample-ios-messagee>

##Running Tests

The iOS SDK unit tests are written with Nu. You'll need to install the language itself to run the tests, and use the Nukefile to create a Usergrid.framework. For installation directions go [here](https://github.com/timburks/nu)

To compile the Usergrid SDK as an Objective-C framework simple type `nuke` to build the framework and then `nuke install` to install it in the /Library/Frameworks path.

To run the unit tests written for the SDK use the command `nuke test` this will run all tests.

## Contributing
We welcome your enhancements!

Like [Usergrid](https://github.com/apache/usergrid/), the Usergrid iOS SDK is open source and licensed under the Apache License, Version 2.0.

1. Fork it
2. Create an issue in [JIRA](https://issues.apache.org/jira/browse/USERGRID/)
3. Create your feature branch (`git checkout -b USERGRID-${JIRA_NUMBER}`)
4. Commit your changes (`git commit -am '${JIRA_NUMBER} ${JIRA_TITLE}'`)
5. Push your changes to the upstream branch (`git push origin my-new-feature`)
6. Create new Pull Request (make sure you describe what you did and why your mod is needed)
