//
//  UsergridEnums.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 10/21/15.
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
An enumeration that is used to determine what the `UsergridClient` will use for authorization.
*/
@objc public enum UsergridAuthMode : Int {

    // MARK: - Values -

    /**
    If the API call fails, the activity is treated as a failure with an appropriate HTTP status code.
    */
    case None

    /**
     If a non-expired `UsergridUserAuth` exists in `UsergridClient.currentUser`, this token is used to authenticate all API calls.

     If the API call fails, the activity is treated as a failure with an appropriate HTTP status code (This behavior is identical to authMode=.None).
     */
    case User

    /**
    If a non-expired `UsergridAppAuth` exists in `UsergridClient.appAuth`, this token is used to authenticate all API calls.

    If the API call fails, the activity is treated as a failure with an appropriate HTTP status code (This behavior is identical to authMode=.None).
    */
    case App
}

/**
`UsergridEntity` specific properties keys.  Note that trying to mutate the values of these properties will not be allowed in most cases.
*/
@objc public enum UsergridEntityProperties : Int {

    // MARK: - Values -

    /// Corresponds to the property 'type'
    case EntityType
    /// Corresponds to the property 'uuid'
    case UUID
    /// Corresponds to the property 'name'
    case Name
    /// Corresponds to the property 'created'
    case Created
    /// Corresponds to the property 'modified'
    case Modified
    /// Corresponds to the property 'location'
    case Location

    // MARK: - Methods -

    /**
    Gets the corresponding `UsergridEntityProperties` from a string if it's valid.

    - parameter stringValue: The string value to convert.

    - returns: The corresponding `UsergridEntityProperties` or nil.
    */
    public static func fromString(stringValue: String) -> UsergridEntityProperties? {
        switch stringValue.lowercaseString {
            case ENTITY_TYPE: return .EntityType
            case ENTITY_UUID: return .UUID
            case ENTITY_NAME: return .Name
            case ENTITY_CREATED: return .Created
            case ENTITY_MODIFIED: return .Modified
            case ENTITY_LOCATION: return .Location
            default: return nil
        }
    }

    /// Returns the string value.
    public var stringValue: String {
        switch self {
            case .EntityType: return ENTITY_TYPE
            case .UUID: return ENTITY_UUID
            case .Name: return ENTITY_NAME
            case .Created: return ENTITY_CREATED
            case .Modified: return ENTITY_MODIFIED
            case .Location: return ENTITY_LOCATION
        }
    }

    /**
    Determines if the `UsergridEntityProperties` is mutable for the given entity.

    - parameter entity: The entity to check.

    - returns: If the `UsergridEntityProperties` is mutable for the given entity
    */
    public func isMutableForEntity(entity:UsergridEntity) -> Bool {
        switch self {
            case .EntityType,.UUID,.Created,.Modified: return false
            case .Location: return true
            case .Name: return entity.isUser
        }
    }
}

/**
`UsergridDeviceProperties` specific properties keys.  Note that trying to mutate the values of these properties will not be allowed in most cases.
*/
@objc public enum UsergridDeviceProperties : Int {

    // MARK: - Values -

    /// Corresponds to the property 'deviceModel'
    case Model
    /// Corresponds to the property 'devicePlatform'
    case Platform
    /// Corresponds to the property 'deviceOSVersion'
    case OSVersion

    // MARK: - Methods -

    /**
    Gets the corresponding `UsergridDeviceProperties` from a string if it's valid.

    - parameter stringValue: The string value to convert.

    - returns: The corresponding `UsergridDeviceProperties` or nil.
    */
    public static func fromString(stringValue: String) -> UsergridDeviceProperties? {
        switch stringValue.lowercaseString {
            case DEVICE_MODEL: return .Model
            case DEVICE_PLATFORM: return .Platform
            case DEVICE_OSVERSION: return .OSVersion
            default: return nil
        }
    }

    /// Returns the string value.
    public var stringValue: String {
        switch self {
            case .Model: return DEVICE_MODEL
            case .Platform: return DEVICE_PLATFORM
            case .OSVersion: return DEVICE_OSVERSION
        }
    }
}

/**
`UsergridUser` specific properties keys.
*/
@objc public enum UsergridUserProperties: Int {

    // MARK: - Values -

    /// Corresponds to the property 'name'
    case Name
    /// Corresponds to the property 'username'
    case Username
    /// Corresponds to the property 'password'
    case Password
    /// Corresponds to the property 'email'
    case Email
    /// Corresponds to the property 'age'
    case Age
    /// Corresponds to the property 'activated'
    case Activated
    /// Corresponds to the property 'disabled'
    case Disabled
    /// Corresponds to the property 'picture'
    case Picture

    // MARK: - Methods -

    /**
    Gets the corresponding `UsergridUserProperties` from a string if it's valid.

    - parameter stringValue: The string value to convert.

    - returns: The corresponding `UsergridUserProperties` or nil.
    */
    public static func fromString(stringValue: String) -> UsergridUserProperties? {
        switch stringValue.lowercaseString {
            case ENTITY_NAME: return .Name
            case USER_USERNAME: return .Username
            case USER_PASSWORD: return .Password
            case USER_EMAIL: return .Email
            case USER_AGE: return .Age
            case USER_ACTIVATED: return .Activated
            case USER_DISABLED: return .Disabled
            case USER_PICTURE: return .Picture
            default: return nil
        }
    }

    /// Returns the string value.
    public var stringValue: String {
        switch self {
            case .Name: return ENTITY_NAME
            case .Username: return USER_USERNAME
            case .Password: return USER_PASSWORD
            case .Email: return USER_EMAIL
            case .Age: return USER_AGE
            case .Activated: return USER_ACTIVATED
            case .Disabled: return USER_DISABLED
            case .Picture: return USER_PICTURE
        }
    }
}

/**
`UsergridQuery` specific operators.
*/
@objc public enum UsergridQueryOperator: Int {

    // MARK: - Values -

    /// '='
    case Equal
    /// '>'
    case GreaterThan
    /// '>='
    case GreaterThanEqualTo
    /// '<'
    case LessThan
    /// '<='
    case LessThanEqualTo

    // MARK: - Methods -

    /**
    Gets the corresponding `UsergridQueryOperator` from a string if it's valid.

    - parameter stringValue: The string value to convert.

    - returns: The corresponding `UsergridQueryOperator` or nil.
    */
    public static func fromString(stringValue: String) -> UsergridQueryOperator? {
        switch stringValue.lowercaseString {
            case UsergridQuery.EQUAL: return .Equal
            case UsergridQuery.GREATER_THAN: return .GreaterThan
            case UsergridQuery.GREATER_THAN_EQUAL_TO: return .GreaterThanEqualTo
            case UsergridQuery.LESS_THAN: return .LessThan
            case UsergridQuery.LESS_THAN_EQUAL_TO: return .LessThanEqualTo
            default: return nil
        }
    }

    /// Returns the string value.
    public var stringValue: String {
        switch self {
            case .Equal: return UsergridQuery.EQUAL
            case .GreaterThan: return UsergridQuery.GREATER_THAN
            case .GreaterThanEqualTo: return UsergridQuery.GREATER_THAN_EQUAL_TO
            case .LessThan: return UsergridQuery.LESS_THAN
            case .LessThanEqualTo: return UsergridQuery.LESS_THAN_EQUAL_TO
        }
    }
}

/**
`UsergridQuery` specific sort orders.
*/
@objc public enum UsergridQuerySortOrder: Int {

    // MARK: - Values -

    /// Sort order is ascending.
    case Asc
    /// Sort order is descending.
    case Desc

    // MARK: - Methods -

    /**
    Gets the corresponding `UsergridQuerySortOrder` from a string if it's valid.

    - parameter stringValue: The string value to convert.

    - returns: The corresponding `UsergridQuerySortOrder` or nil.
    */
    public static func fromString(stringValue: String) -> UsergridQuerySortOrder? {
        switch stringValue.lowercaseString {
            case UsergridQuery.ASC: return .Asc
            case UsergridQuery.DESC: return .Desc
            default: return nil
        }
    }

    /// Returns the string value.
    public var stringValue: String {
        switch self {
            case .Asc: return UsergridQuery.ASC
            case .Desc: return UsergridQuery.DESC
        }
    }
}

/**
`UsergridAsset` image specific content types.
*/
@objc public enum UsergridImageContentType : Int {

    // MARK: - Values -

    /// Content type: 'image/png'
    case Png
    /// Content type: 'image/jpeg'
    case Jpeg

    // MARK: - Methods -

    /// Returns the string value.
    public var stringValue: String {
        switch self {
            case .Png: return ASSET_IMAGE_PNG
            case .Jpeg: return ASSET_IMAGE_JPEG
        }
    }
}

/**
 An enumeration that is used when getting connections to entity objects. Used to determine which the direction of the connection is wanted.
 */
@objc public enum UsergridDirection : Int {

    // MARK: - Values -

    /// To get the entities that have created a connection to an entity. aka `connecting`
    case In

    /// To get the entities an entity has connected to. aka `connections`
    case Out

    // MARK: - Methods -

    /// Returns the connection value.
    public var connectionValue: String {
        switch self {
            case .In: return CONNECTION_TYPE_IN
            case .Out: return CONNECTION_TYPE_OUT
        }
    }
}

/**
 An enumeration for defining the HTTP methods used by Usergrid.
 */
@objc public enum UsergridHttpMethod : Int {

    /// GET
    case Get

    /// PUT
    case Put

    /// POST
    case Post

    /// DELETE
    case Delete

    /// Returns the string value.
    public var stringValue: String {
        switch self {
            case .Get: return "GET"
            case .Put: return "PUT"
            case .Post: return "POST"
            case .Delete: return "DELETE"
        }
    }
}

let ENTITY_TYPE = "type"
let ENTITY_UUID = "uuid"
let ENTITY_NAME = "name"
let ENTITY_CREATED = "created"
let ENTITY_MODIFIED = "modified"
let ENTITY_LOCATION = "location"
let ENTITY_LATITUDE = "latitude"
let ENTITY_LONGITUDE = "longitude"

let USER_USERNAME = "username"
let USER_PASSWORD = "password"
let USER_EMAIL = "email"
let USER_AGE = "age"
let USER_ACTIVATED = "activated"
let USER_DISABLED = "disabled"
let USER_PICTURE = "picture"

let DEVICE_MODEL = "deviceModel"
let DEVICE_PLATFORM = "devicePlatform"
let DEVICE_OSVERSION = "deviceOSVersion"

let ASSET_IMAGE_PNG = "image/png"
let ASSET_IMAGE_JPEG = "image/jpeg"

let CONNECTION_TYPE_IN = "connecting"
let CONNECTION_TYPE_OUT = "connections"
