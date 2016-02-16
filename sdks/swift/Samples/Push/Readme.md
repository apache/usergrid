#Push

## Running the Sample

To run the sample app, simply open the `Push.xcworkspace` file in Xcode.

Two targets in Xcode specific to this application will be available:

- **Push Target**

	This will run the iOS sample application.
	
##Configuring the Sample Apps

Before running the sample applications you will need to configure each sample application. 

Each sample application should include a source file named `UsergridManager.swift`.  This source file is used to contain interaction with the UsergridSDK within a single source file.  In doing so, the interactions within the sample apps can be easily seen and examined.

Within the `UsergridManager.swift` source there will be at least two different static vars named `ORG_ID` and `APP_ID`.  You will need to configure those values in order to run the applications in your environment.    

Applications which utilize push notifications will require a valid provisioning profile and device for the push services to work correctly.   
