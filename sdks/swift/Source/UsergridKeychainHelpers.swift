//
//  UsergridKeychainHelpers.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 12/21/15.
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

#if os(iOS) || os(tvOS) || os(watchOS)
import UIKit
#endif

private let USERGRID_KEYCHAIN_NAME = "Usergrid"
private let USERGRID_DEVICE_KEYCHAIN_SERVICE = "SharedDevice"
private let USERGRID_CURRENT_USER_KEYCHAIN_SERVICE = "CurrentUser"

private func usergridGenericKeychainItem() -> [String:AnyObject] {
    var keychainItem: [String:AnyObject] = [:]
    keychainItem[kSecClass as String] = kSecClassGenericPassword as String
    keychainItem[kSecAttrAccessible as String] = kSecAttrAccessibleAlways as String
    keychainItem[kSecAttrAccount as String] = USERGRID_KEYCHAIN_NAME
    return keychainItem
}

internal extension UsergridDevice {

    static func deviceKeychainItem() -> [String:AnyObject] {
        var keychainItem = usergridGenericKeychainItem()
        keychainItem[kSecAttrService as String] = USERGRID_DEVICE_KEYCHAIN_SERVICE
        return keychainItem
    }

    static func createNewDeviceKeychainUUID() -> String {

        #if os(watchOS) || os(OSX)
            let usergridUUID = NSUUID().UUIDString
        #elseif os(iOS) || os(tvOS)
            let usergridUUID = UIDevice.currentDevice().identifierForVendor?.UUIDString ?? NSUUID().UUIDString
        #endif

        return usergridUUID
    }

    private static func createNewSharedDevice() -> UsergridDevice {
        var deviceEntityDict = UsergridDevice.commonDevicePropertyDict()
        deviceEntityDict[UsergridEntityProperties.UUID.stringValue] = UsergridDevice.createNewDeviceKeychainUUID()
        let sharedDevice = UsergridDevice(type: UsergridDevice.DEVICE_ENTITY_TYPE, name: nil, propertyDict: deviceEntityDict)
        return sharedDevice
    }

    static func getOrCreateSharedDeviceFromKeychain() -> UsergridDevice {
        var queryAttributes = UsergridDevice.deviceKeychainItem()
        queryAttributes[kSecReturnData as String] = kCFBooleanTrue as Bool
        queryAttributes[kSecReturnAttributes as String] = kCFBooleanTrue as Bool
        var result: AnyObject?
        let status = withUnsafeMutablePointer(&result) { SecItemCopyMatching(queryAttributes, UnsafeMutablePointer($0)) }
        if status == errSecSuccess {
            if let resultDictionary = result as? NSDictionary {
                if let resultData = resultDictionary[kSecValueData as String] as? NSData {
                    if let sharedDevice = NSKeyedUnarchiver.unarchiveObjectWithData(resultData) as? UsergridDevice {
                        return sharedDevice
                    } else {
                        UsergridDevice.deleteSharedDeviceKeychainItem()
                    }
                }
            }
        }

        let sharedDevice = UsergridDevice.createNewSharedDevice()
        UsergridDevice.saveSharedDeviceKeychainItem(sharedDevice)
        return sharedDevice
    }


    static func saveSharedDeviceKeychainItem(device:UsergridDevice) {
        var queryAttributes = UsergridDevice.deviceKeychainItem()
        queryAttributes[kSecReturnData as String] = kCFBooleanTrue as Bool
        queryAttributes[kSecReturnAttributes as String] = kCFBooleanTrue as Bool

        let sharedDeviceData = NSKeyedArchiver.archivedDataWithRootObject(device);

        if SecItemCopyMatching(queryAttributes,nil) == errSecSuccess // Do we need to update keychain item or add a new one.
        {
            let attributesToUpdate = [kSecValueData as String:sharedDeviceData]
            let updateStatus = SecItemUpdate(UsergridDevice.deviceKeychainItem(), attributesToUpdate)
            if updateStatus != errSecSuccess {
                print("Error updating shared device data to keychain!")
            }
        }
        else
        {
            var keychainItem = UsergridDevice.deviceKeychainItem()
            keychainItem[kSecValueData as String] = sharedDeviceData
            let status = SecItemAdd(keychainItem, nil)
            if status != errSecSuccess {
                print("Error adding shared device data to keychain!")
            }
        }
    }

    static func deleteSharedDeviceKeychainItem() {
        var queryAttributes = UsergridDevice.deviceKeychainItem()
        queryAttributes[kSecReturnData as String] = kCFBooleanFalse as Bool
        queryAttributes[kSecReturnAttributes as String] = kCFBooleanFalse as Bool
        if SecItemCopyMatching(queryAttributes,nil) == errSecSuccess {
            let deleteStatus = SecItemDelete(queryAttributes)
            if deleteStatus != errSecSuccess {
                print("Error deleting shared device data to keychain!")
            }
        }
    }
}

internal extension UsergridUser {

    static func userKeychainItem(client:UsergridClient) -> [String:AnyObject] {
        var keychainItem = usergridGenericKeychainItem()
        keychainItem[kSecAttrService as String] = USERGRID_CURRENT_USER_KEYCHAIN_SERVICE + "." + client.appId + "." + client.orgId
        return keychainItem
    }

    static func getCurrentUserFromKeychain(client:UsergridClient) -> UsergridUser? {
        var queryAttributes = UsergridUser.userKeychainItem(client)
        queryAttributes[kSecReturnData as String] = kCFBooleanTrue as Bool
        queryAttributes[kSecReturnAttributes as String] = kCFBooleanTrue as Bool

        var result: AnyObject?
        let status = withUnsafeMutablePointer(&result) { SecItemCopyMatching(queryAttributes, UnsafeMutablePointer($0)) }
        if status == errSecSuccess {
            if let resultDictionary = result as? NSDictionary {
                if let resultData = resultDictionary[kSecValueData as String] as? NSData {
                    if let currentUser = NSKeyedUnarchiver.unarchiveObjectWithData(resultData) as? UsergridUser {
                        return currentUser
                    }
                }
            }
        }
        return nil
    }

    static func saveCurrentUserKeychainItem(client:UsergridClient, currentUser:UsergridUser) {
        var queryAttributes = UsergridUser.userKeychainItem(client)
        queryAttributes[kSecReturnData as String] = kCFBooleanTrue as Bool
        queryAttributes[kSecReturnAttributes as String] = kCFBooleanTrue as Bool

        if SecItemCopyMatching(queryAttributes,nil) == errSecSuccess // Do we need to update keychain item or add a new one.
        {
            let attributesToUpdate = [kSecValueData as String:NSKeyedArchiver.archivedDataWithRootObject(currentUser)]
            let updateStatus = SecItemUpdate(UsergridUser.userKeychainItem(client), attributesToUpdate)
            if updateStatus != errSecSuccess {
                print("Error updating current user data to keychain!")
            }
        }
        else
        {
            var keychainItem = UsergridUser.userKeychainItem(client)
            keychainItem[kSecValueData as String] = NSKeyedArchiver.archivedDataWithRootObject(currentUser)
            let status = SecItemAdd(keychainItem, nil)
            if status != errSecSuccess {
                print("Error adding current user data to keychain!")
            }
        }
    }

    static func deleteCurrentUserKeychainItem(client:UsergridClient) {
        var queryAttributes = UsergridUser.userKeychainItem(client)
        queryAttributes[kSecReturnData as String] = kCFBooleanFalse as Bool
        queryAttributes[kSecReturnAttributes as String] = kCFBooleanFalse as Bool
        if SecItemCopyMatching(queryAttributes,nil) == errSecSuccess {
            let deleteStatus = SecItemDelete(queryAttributes)
            if deleteStatus != errSecSuccess {
                print("Error deleting current user data to keychain!")
            }
        }
    }
}
