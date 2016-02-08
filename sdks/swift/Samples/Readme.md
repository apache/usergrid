#UsergridSDK Sample Apps

The sample apps in this directory are intended to show basic usage of some of the major features of the UsergridSDK.

Each sample application utilizes `Cocoapods` to interact with the UsergridSDK.

##Samples Apps

* **ActivityFeed** - An app that demonstrates a wide variety of operations within the SDK.  This app also contains a companion WatchOS application.  

* **Push** - An app that registers for and sends push notifications. 

##Configuring the Sample Apps

Before running the sample applications you will need to configure each sample application. 

Each sample application should include a source file named `UsergridManager.swift`.  This source file is used to contain interaction with the UsergridSDK within a single source file.  In doing so, the interactions within the sample apps can be easily seen and examined.

Within the `UsergridManager.swift` source there will be at least two different static vars named `ORG_ID` and `APP_ID`.  You will need to configure those values in order to run the applications in your environment.    

Applications which utilize push notifications will require a valid provisioning profile and device for the push services to work correctly.   

## Running the Sample Apps

To run the sample apps, simply open the <SAMPLE APP NAME>.xcworkspace file in Xcode, then run the app.