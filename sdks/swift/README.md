# UsergridSDK

[![Platform](https://img.shields.io/cocoapods/p/UsergridSDK.svg?style=flat)](http://cocoadocs.org/docsets/UsergridSDK)
[![Cocoapods Compatible](https://img.shields.io/cocoapods/v/UsergridSDK.svg)](https://cocoapods.org/pods/UsergridSDK)

Usergrid SDK written in Swift 

## Requirements

- iOS 8.0+ / Mac OS X 10.11+ / tvOS 9.1+ / watchOS 2.1+
- Xcode 7.1+

## Installation

> **Embedded frameworks require a minimum deployment target of iOS 8 or OS X Mavericks (10.9).**

### CocoaPods

> **CocoaPods 0.39.0+ is required to build the UsergridSDK library.**

To integrate the UsergridSDK into your Xcode project using CocoaPods, specify it in your `Podfile`:

```ruby
platform :ios, '8.0'
use_frameworks!

pod 'UsergridSDK'
```

Then, run the following command:

```bash
$ pod install
```

### Embedded Framework

- Open up Terminal, `cd` into your top-level project directory, and run the following command "if" your project is not initialized as a git repository:

```bash
$ git init
```

- Add UsergridSDK as a git submodule by running the following command:

```bash
$ git submodule add https://github.com/apache/usergrid
```

- Open the `sdks/swift` folder, and drag the `UsergridSDK.xcodeproj` into the Project Navigator of your application's Xcode project.

> It should appear nested underneath your application's blue project icon.

- Select the `UsergridSDK.xcodeproj` in the Project Navigator and verify the deployment target matches that of your application target.
- Next, select your application project in the Project Navigator (blue project icon) to navigate to the target configuration window and select the application target under the "Targets" heading in the sidebar.
- In the tab bar at the top of that window, open the "General" panel.
- Click on the `+` button under the "Embedded Binaries" section.
- Select the `UsergridSDK.framework`.

> The `UsergridSDK.framework` is automatically added as a target dependency, linked framework and embedded framework in a copy files build phase which is all you need to build on the simulator and a device.

## Documentation

The documentation for this library is available [here](http://cocoadocs.org/docsets/UsergridSDK).

## Initialization

The `Usergrid` class acts as a static shared instance manager for the `UsergridClient` class.

> While it is possible to create mutliple instances of the `UsergridClient` class, we recomend using the shared instance where possible.

To initialize the shared instance of you will want to call the following code.  This code usually goes best in the AppDelegate's life cycle functions.

```swift
import UsergridSDK

func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool
    Usergrid.initSharedInstance(orgID: "orgID", appID: "appID")
    return true
}
```

## Communication

- If you **found a bug**, open an issue.
- If you **have a feature request**, open an issue.
- If you **want to contribute**, submit a pull request.
