//
//  UsergridDevice.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 10/23/15.
//
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 */

import Foundation

#if !os(OSX)
import UIKit
#endif

#if os(watchOS)
import WatchKit
#endif
 
/**
`UsergridDevice` is an `UsergridEntity` subclass that encapsulates information about the current device as well as stores information about push tokens and Usergrid notifiers.

To apply push tokens for Usergrid notifiers use the `UsergridClient.applyPushToken` method.
*/
public class UsergridDevice : UsergridEntity {

    /// The `UsergridDevice` type.
    static let DEVICE_ENTITY_TYPE = "device"

    // MARK: - Instance Properties -

    /// Property helper method for the `UsergridDevice` objects device model.
    public var model: String { return super[UsergridDeviceProperties.Model.stringValue] as! String }

    /// Property helper method for the `UsergridDevice` objects device platform.
    public var platform: String { return super[UsergridDeviceProperties.Platform.stringValue] as! String }

    /// Property helper method for the `UsergridDevice` objects device operating system version.
    public var osVersion: String { return super[UsergridDeviceProperties.OSVersion.stringValue] as! String }

    /// The shared instance of `UsergridDevice`.
    public static var sharedDevice: UsergridDevice = UsergridDevice.getOrCreateSharedDeviceFromKeychain()

    // MARK: - Initialization -

    /**
    Designated Initializer for `UsergridDevice` objects
    
    Most likely you will never need to create seperate instances of `UsergridDevice`.  Use of `UsergridDevice.sharedInstance` is recommended.

    - returns: A new instance of `UsergridDevice`.
    */
    public init() {
        super.init(type: UsergridDevice.DEVICE_ENTITY_TYPE, propertyDict: UsergridDevice.commonDevicePropertyDict())
    }

    /**
     The required public initializer for `UsergridEntity` subclasses.

     - parameter type:         The type associated with the `UsergridEntity` object.
     - parameter name:         The optional name associated with the `UsergridEntity` object.
     - parameter propertyDict: The optional property dictionary that the `UsergridEntity` object will start out with.

     - returns: A new `UsergridDevice` object.
     */
    required public init(type:String, name:String? = nil, propertyDict:[String:AnyObject]? = nil) {
        super.init(type: type, name: name, propertyDict: propertyDict)
    }

    // MARK: - NSCoding -

    /**
    NSCoding protocol initializer.

    - parameter aDecoder: The decoder.

    - returns: A decoded `UsergridUser` object.
    */
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }

    /**
     NSCoding protocol encoder.

     - parameter aCoder: The encoder.
     */
    public override func encodeWithCoder(aCoder: NSCoder) {
        super.encodeWithCoder(aCoder)
    }

    /**
     Performs a PUT (or POST if no UUID is found) on the `UsergridDevice` using the shared instance of `UsergridClient`.
     
     If this device is equal to `UsergridDevice.sharedDevice` it will also update the shared device on the keychain.

     - parameter completion: An optional completion block that, if successful, will contain the updated/saved `UsergridEntity` object.
     */
    public override func save(completion: UsergridResponseCompletion? = nil) {
        self.save(Usergrid.sharedInstance, completion: completion)
    }


    /**
     Performs a PUT (or POST if no UUID is found) on the `UsergridDevice`.

     If this device is equal to `UsergridDevice.sharedDevice` it will also update the shared device on the keychain.

     - parameter client:     The client to use when saving.
     - parameter completion: An optional completion block that, if successful, will contain the updated/saved `UsergridEntity` object.
     */
    public override func save(client: UsergridClient, completion: UsergridResponseCompletion? = nil) {
        super.save(client) { (response) in
            if( response.ok ) {
                if( self == UsergridDevice.sharedDevice || self.isEqualToEntity(UsergridDevice.sharedDevice)) {
                    UsergridDevice.saveSharedDeviceToKeychain()
                }
            }
            completion?(response:response)
        }
    }

    /**
     Saves the `UsergridDevice.sharedDevice` to the keychain.
     */
    public static func saveSharedDeviceToKeychain() {
        UsergridDevice.saveSharedDeviceKeychainItem(UsergridDevice.sharedDevice)
    }

    /**
    Subscript for the `UsergridDevice` class. Note that all of the `UsergridDeviceProperties` are immutable.

    - Warning: When setting a properties value must be a valid JSON object.

    - Example usage:
        ```
        let uuid = usergridDevice["uuid"]
        ```
    */
    override public subscript(propertyName: String) -> AnyObject? {
        get {
            return super[propertyName]
        }
        set(propertyValue) {
            if UsergridDeviceProperties.fromString(propertyName) == nil {
                super[propertyName] = propertyValue
            }
        }
    }

    // MARK: - Class Helper Methods -

    /**
    Creates a property dictionary that contains the common properties for `UsergridDevice` objects.

    - returns: A property dictionary with the common properties set.
    */
    public static func commonDevicePropertyDict() -> [String:AnyObject] {
        var commonDevicePropertyDict: [String:AnyObject] = [:]
        commonDevicePropertyDict[UsergridEntityProperties.EntityType.stringValue] = UsergridDevice.DEVICE_ENTITY_TYPE

        #if os(watchOS)
            commonDevicePropertyDict[UsergridDeviceProperties.Model.stringValue] = WKInterfaceDevice.currentDevice().model
            commonDevicePropertyDict[UsergridDeviceProperties.Platform.stringValue] = WKInterfaceDevice.currentDevice().systemName
            commonDevicePropertyDict[UsergridDeviceProperties.OSVersion.stringValue] = WKInterfaceDevice.currentDevice().systemVersion
        #elseif os(iOS) || os(tvOS)
            commonDevicePropertyDict[UsergridDeviceProperties.Model.stringValue] = UIDevice.currentDevice().model
            commonDevicePropertyDict[UsergridDeviceProperties.Platform.stringValue] = UIDevice.currentDevice().systemName
            commonDevicePropertyDict[UsergridDeviceProperties.OSVersion.stringValue] = UIDevice.currentDevice().systemVersion
        #elseif os(OSX)
            commonDevicePropertyDict[UsergridDeviceProperties.Model.stringValue] = "Mac"
            commonDevicePropertyDict[UsergridDeviceProperties.Platform.stringValue] = "OSX"
            commonDevicePropertyDict[UsergridDeviceProperties.OSVersion.stringValue] = NSProcessInfo.processInfo().operatingSystemVersionString
        #endif

        return commonDevicePropertyDict
    }

    // MARK: - Push Token Handling -

    /**
    Sets the push token for the given notifier ID.

    This does not perform any API requests to update on Usergrid, rather it will just set the information in the `UsergridDevice` instance.

    In order to set the push token and perform an API request, use `UsergridClient.applyPushToken`.

    - parameter pushToken:  The push token from Apple.
    - parameter notifierID: The notifier ID.
    */
    internal func applyPushToken(pushToken: NSData, notifierID: String) {
        self[notifierID + USERGRID_NOTIFIER_ID_SUFFIX] = pushToken.description.stringByTrimmingCharactersInSet(NSCharacterSet(charactersInString: "<>")).stringByReplacingOccurrencesOfString(" ", withString: "")
    }
}

private let USERGRID_NOTIFIER_ID_SUFFIX = ".notifier.id"
