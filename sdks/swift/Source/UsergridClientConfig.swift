//
//  UsergridClientConfig.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 10/5/15.
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

/**
`UsergridClientConfig` is used when initializing `UsergridClient` objects.

The `UsergridClientConfig` is meant for further customization of `UsergridClient` objects when needed.
*/
public class UsergridClientConfig : NSObject, NSCoding {

    // MARK: - Instance Properties -

    /// The organization identifier.
    public var orgId : String

    /// The application identifier.
    public var appId : String

    /// The base URL that all calls will be made with.
    public var baseUrl: String = UsergridClient.DEFAULT_BASE_URL

    /// The `UsergridAuthMode` value used to determine what type of token will be sent, if any.
    public var authMode: UsergridAuthMode = .User

    /// Whether or not the `UsergridClient` current user will be saved and restored from the keychain.
    public var persistCurrentUserInKeychain: Bool = true

    /**
    The application level `UsergridAppAuth` object.
    
    Note that you still need to call the authentication methods within `UsergridClient` once it has been initialized.
    */
    public var appAuth: UsergridAppAuth?

    // MARK: - Initialization -

    /**
    Designated initializer for `UsergridClientConfig` objects.

    - parameter orgId: The organization identifier.
    - parameter appId: The application identifier.

    - returns: A new instance of `UsergridClientConfig`.
    */
    public init(orgId: String, appId: String) {
        self.orgId = orgId
        self.appId = appId
    }

    /**
    Convenience initializer for `UsergridClientConfig`.

    - parameter orgId:   The organization identifier.
    - parameter appId:   The application identifier.
    - parameter baseUrl: The base URL that all calls will be made with.

    - returns: A new instance of `UsergridClientConfig`.
    */
    public convenience init(orgId: String, appId: String, baseUrl:String) {
        self.init(orgId:orgId,appId:appId)
        self.baseUrl = baseUrl
    }

    /**
    Convenience initializer for `UsergridClientConfig`.

    - parameter orgId:                          The organization identifier.
    - parameter appId:                          The application identifier.
    - parameter baseUrl:                        The base URL that all calls will be made with.
    - parameter authMode:                       The `UsergridAuthMode` value used to determine what type of token will be sent, if any.
    - parameter persistCurrentUserInKeychain:   Whether or not the `UsergridClient` current user will be saved and restored from the keychain.
    - parameter appAuth:                        The application level `UsergridAppAuth` object.

    - returns: A new instance of `UsergridClientConfig`.
    */
    public convenience init(orgId: String, appId: String, baseUrl:String, authMode:UsergridAuthMode, persistCurrentUserInKeychain: Bool = true, appAuth:UsergridAppAuth? = nil) {
        self.init(orgId:orgId,appId:appId,baseUrl:baseUrl)
        self.persistCurrentUserInKeychain = persistCurrentUserInKeychain
        self.authMode = authMode
        self.appAuth = appAuth
    }

    // MARK: - NSCoding -

    /**
    NSCoding protocol initializer.

    - parameter aDecoder: The decoder.

    - returns: A decoded `UsergridUser` object.
    */
    public required init?(coder aDecoder: NSCoder) {
        guard   let appId = aDecoder.decodeObjectForKey("appId") as? String,
                let orgId = aDecoder.decodeObjectForKey("orgId") as? String,
                let baseUrl = aDecoder.decodeObjectForKey("baseUrl") as? String
        else {
            self.appId = ""
            self.orgId = ""
            super.init()
            return nil
        }
        self.appId = appId
        self.orgId = orgId
        self.baseUrl = baseUrl
        self.appAuth = aDecoder.decodeObjectForKey("appAuth") as? UsergridAppAuth
        self.persistCurrentUserInKeychain = aDecoder.decodeBoolForKey("persistCurrentUserInKeychain") ?? true
        self.authMode = UsergridAuthMode(rawValue:aDecoder.decodeIntegerForKey("authMode")) ?? .None
        super.init()
    }

    /**
     NSCoding protocol encoder.

     - parameter aCoder: The encoder.
     */
    public func encodeWithCoder(aCoder: NSCoder) {
        aCoder.encodeObject(self.appId, forKey: "appId")
        aCoder.encodeObject(self.orgId, forKey: "orgId")
        aCoder.encodeObject(self.baseUrl, forKey: "baseUrl")
        aCoder.encodeObject(self.appAuth, forKey: "appAuth")
        aCoder.encodeBool(self.persistCurrentUserInKeychain, forKey: "persistCurrentUserInKeychain")
        aCoder.encodeInteger(self.authMode.rawValue, forKey: "authMode")
    }
}
