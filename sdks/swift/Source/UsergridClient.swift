//
//  UsergridClient.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 9/3/15.
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
The `UsergridClient` class is the base handler for making client connections to and managing relationships with Usergrid's API.
*/
public class UsergridClient: NSObject, NSCoding {

    static let DEFAULT_BASE_URL = "https://api.usergrid.com"

    // MARK: - Instance Properties -

    lazy private var _requestManager: UsergridRequestManager = UsergridRequestManager(client: self)

    /// The configuration object used by the client.
    public let config: UsergridClientConfig

    /// The application identifier.
    public var appId : String { return config.appId }

    /// The organization identifier.
    public var orgId : String { return config.orgId }

    /// The base URL that all calls will be made with.
    public var baseUrl : String { return config.baseUrl }

    /// The constructed URL string based on the `UsergridClient`'s `baseUrl`, `orgId`, and `appId`.
    public var clientAppURL : String { return "\(baseUrl)/\(orgId)/\(appId)" }

    /// Whether or not the current user will be saved and restored from the keychain.
    public var persistCurrentUserInKeychain: Bool {
        get { return config.persistCurrentUserInKeychain }
        set(persist) { config.persistCurrentUserInKeychain = persist }
    }

    /// The currently logged in `UsergridUser`.
    internal(set) public var currentUser: UsergridUser? = nil {
        didSet {
            if persistCurrentUserInKeychain {
                if let newUser = self.currentUser {
                    UsergridUser.saveCurrentUserKeychainItem(self,currentUser:newUser)
                } else if oldValue != nil {
                    UsergridUser.deleteCurrentUserKeychainItem(self)
                }
            }
        }
    }

    /// The `UsergridUserAuth` which consists of the token information from the `currentUser` property.
    public var userAuth: UsergridUserAuth? { return currentUser?.auth }

    /// The temporary `UsergridAuth` object that is set when calling the `UsergridClient.usingAuth()` method.
    private var tempAuth: UsergridAuth? = nil

    /// The application level `UsergridAppAuth` object.  Can be set manually but must call `authenticateApp` to retrieve token.
    public var appAuth: UsergridAppAuth? {
        get { return config.appAuth }
        set(auth) { config.appAuth = auth }
    }

    /// The `UsergridAuthMode` value used to determine what type of token will be sent, if any.
    public var authMode: UsergridAuthMode {
        get { return config.authMode }
        set(mode) { config.authMode = mode }
    }

    // MARK: - Initialization -

    /**
    Initializes instances of `UsergridClient`.

    - parameter orgId: The organization identifier.
    - parameter appId: The application identifier.

    - returns: The new instance of `UsergridClient`.
    */
    public convenience init(orgId: String, appId:String) {
        self.init(configuration:UsergridClientConfig(orgId: orgId, appId: appId))
    }

    /**
    Initializes instances of `UsergridClient`.

    - parameter orgId:      The organization identifier.
    - parameter appId:      The application identifier.
    - parameter baseUrl:    The base URL that all calls will be made with.

    - returns: The new instance of `UsergridClient`.
    */
    public convenience init(orgId: String, appId:String, baseUrl:String) {
        self.init(configuration:UsergridClientConfig(orgId: orgId, appId: appId, baseUrl:baseUrl))
    }

    /**
    Initializes instances of `UsergridClient`.

    - parameter configuration: The configuration for the client to be set up with.

    - returns: The new instance of `UsergridClient`.
    */
    public init(configuration:UsergridClientConfig) {
        self.config = configuration
        super.init()
        if persistCurrentUserInKeychain {
            self.currentUser = UsergridUser.getCurrentUserFromKeychain(self) // Attempt to get the current user from the saved keychain data.
        }
        UsergridDevice.sharedDevice.save(self)
    }

    // MARK: - NSCoding -

    /**
    NSCoding protocol initializer.

    - parameter aDecoder: The decoder.

    - returns: A decoded `UsergridClient` object.
    */
    public required init?(coder aDecoder: NSCoder) {
        guard let config = aDecoder.decodeObjectForKey("config") as? UsergridClientConfig
        else {
            self.config = UsergridClientConfig(orgId: "", appId: "")
            super.init()
            return nil
        }

        self.config = config
        super.init()

        if let currentUser = aDecoder.decodeObjectForKey("currentUser") as? UsergridUser {
            self.currentUser = currentUser
        } else {
            if persistCurrentUserInKeychain {
                self.currentUser = UsergridUser.getCurrentUserFromKeychain(self)
            }
        }
        UsergridDevice.sharedDevice.save(self)
    }

    /**
     NSCoding protocol encoder.

     - parameter aCoder: The encoder.
     */
    public func encodeWithCoder(aCoder: NSCoder) {
        aCoder.encodeObject(self.config, forKey: "config")
        aCoder.encodeObject(self.currentUser, forKey: "currentUser")
    }

    // MARK: - Device Registration/Push Notifications -

    /**
    Sets the push token for the given notifier ID and performs a PUT request to update the shared `UsergridDevice` instance.

    - parameter pushToken:  The push token from Apple.
    - parameter notifierID: The Usergrid notifier ID.
    - parameter completion: The completion block.
    */
    public func applyPushToken(pushToken: NSData, notifierID: String, completion: UsergridResponseCompletion? = nil) {
        self.applyPushToken(UsergridDevice.sharedDevice, pushToken: pushToken, notifierID: notifierID, completion: completion)
    }

    /**
    Sets the push token for the given notifier ID and performs a PUT request to update the given `UsergridDevice` instance.

    - parameter device:     The `UsergridDevice` object.
    - parameter pushToken:  The push token from Apple.
    - parameter notifierID: The Usergrid notifier ID.
    - parameter completion: The completion block.
    */
    public func applyPushToken(device: UsergridDevice, pushToken: NSData, notifierID: String, completion: UsergridResponseCompletion? = nil) {
        device.applyPushToken(pushToken, notifierID: notifierID)
        self.PUT("devices", jsonBody: device.jsonObjectValue) { (response) in
            if let responseEntity = response.entity {
                device.copyInternalsFromEntity(responseEntity)
            }
            completion?(response: response)
        }
    }

    // MARK: - Authorization and User Management -

    /**
    Determines the `UsergridAuth` object that will be used for all outgoing requests made.

    If there is a valid temporary `UsergridAuth` set by the functions `usingAuth` or `usingToken` it will return that.

    If there is a `UsergridUser` logged in and the token of that user is valid then it will return that.

    Otherwise, if the `authMode` is `.App`, and the `UsergridAppAuth` of the client is set and the token is valid it will return that.

    - returns: The `UsergridAuth` if one is found or nil if not.
    */
    public func authForRequests() -> UsergridAuth? {
        var usergridAuth: UsergridAuth?
        if let tempAuth = self.tempAuth {
            if tempAuth.isValid {
                usergridAuth = tempAuth
            }
            self.tempAuth = nil
        } else {
            switch(self.authMode) {
                case .User:
                    if let userAuth = self.userAuth where userAuth.isValid {
                        usergridAuth = userAuth
                    }
                    break
                case .App:
                    if let appAuth = self.appAuth where appAuth.isValid {
                        usergridAuth = appAuth
                    }
                    break
                case .None:
                    usergridAuth = nil
                    break
            }
        }
        return usergridAuth
    }

    /**
     Sets the client's `tempAuth` property using the passed in `UsergridAuth`.

     This will cause the next CRUD method performed by the client to use the `tempAuth` property once and will then reset.

     - parameter auth: The `UsergridAuth` object to temporarily use for authentication.

     - returns: `Self`
     */
    public func usingAuth(auth:UsergridAuth) -> Self {
        self.tempAuth = auth
        return self
    }

    /**
     Sets the client's `tempAuth` property using the passed in token.
     
     This will cause the next CRUD method performed by the client to use the `tempAuth` property once and will then reset.

     - parameter auth: The access token to temporarily use for authentication.

     - returns: `Self`
     */
    public func usingToken(token:String) -> Self {
        self.tempAuth = UsergridAuth(accessToken: token)
        return self
    }

    /**
    Authenticates with the `UsergridAppAuth` that is contained this instance of `UsergridCient`.

    - parameter completion: The completion block that will be called after authentication has completed.
    */
    public func authenticateApp(completion: UsergridAppAuthCompletionBlock? = nil) {
        guard let appAuth = self.appAuth
        else {
            let error = UsergridResponseError(errorName: "Invalid UsergridAppAuth.", errorDescription: "UsergridClient's appAuth is nil.")
            completion?(auth: nil, error: error)
            return
        }
        self.authenticateApp(appAuth, completion: completion)
    }

    /**
    Authenticates with the `UsergridAppAuth` that is passed in.

    - parameter auth:       The `UsergridAppAuth` that will be authenticated.
    - parameter completion: The completion block that will be called after authentication has completed.
    */
    public func authenticateApp(appAuth: UsergridAppAuth, completion: UsergridAppAuthCompletionBlock? = nil) {
        let request = UsergridRequest(method: .Post,
                                      baseUrl: self.clientAppURL,
                                      paths: ["token"],
                                      auth: self.authForRequests(),
                                      jsonBody: appAuth.credentialsJSONDict)

        _requestManager.performAppAuthRequest(appAuth, request: request) { [weak self] (auth,error) in
            self?.appAuth = auth
            completion?(auth: auth, error: error)
        }
    }

    /**
    Authenticates with the `UsergridUserAuth` that is passed in.

    - parameter auth:       The `UsergridUserAuth` that will be authenticated.
    - parameter completion: The completion block that will be called after authentication has completed.
    */
    public func authenticateUser(userAuth: UsergridUserAuth, completion: UsergridUserAuthCompletionBlock? = nil) {
        self.authenticateUser(userAuth, setAsCurrentUser:true, completion:completion)
    }

    /**
    Authenticates with the `UsergridUserAuth` that is passed in.

    - parameter auth:               The `UsergridUserAuth` that will be authenticated.
    - parameter setAsCurrentUser:   If the authenticated user should be set as the `UsergridClient.currentUser`.
    - parameter completion:         The completion block that will be called after authentication has completed.
    */
    public func authenticateUser(userAuth: UsergridUserAuth, setAsCurrentUser: Bool, completion: UsergridUserAuthCompletionBlock? = nil) {
        let request = UsergridRequest(method: .Post,
                                      baseUrl: self.clientAppURL,
                                      paths: ["token"],
                                      auth: self.authForRequests(),
                                      jsonBody: userAuth.credentialsJSONDict)
        _requestManager.performUserAuthRequest(userAuth, request: request) { [weak self] (auth,user,error) in
            if setAsCurrentUser {
                self?.currentUser = user
            }
            completion?(auth: auth, user: user, error: error)
        }
    }

    /**
     Changes the given `UsergridUser`'s current password.

     - parameter user:       The user.
     - parameter old:        The old password.
     - parameter new:        The new password.
     - parameter completion: The optional completion block.
     */
    public func resetPassword(user: UsergridUser, old:String, new:String, completion:UsergridUserResetPasswordCompletion? = nil) {
        guard let usernameOrEmail = user.usernameOrEmail
        else {
            completion?(error: UsergridResponseError(errorName: "Error resetting password.", errorDescription: "The UsergridUser object must contain a valid username or email to reset the password."), didSucceed: false)
            return
        }

        let request = UsergridRequest(method: .Put,
                                      baseUrl: self.clientAppURL,
                                      paths: ["users",usernameOrEmail,"password"],
                                      auth: self.authForRequests(),
                                      jsonBody:["oldpassword":old,"newpassword":new])

        _requestManager.performRequest(request, completion: { (response) -> Void in
            completion?(error: response.error, didSucceed: response.statusCode == 200)
        })
    }

    /**
    Logs out the current user locally and remotely.

    - parameter completion: The completion block that will be called after logout has completed.
    */
    public func logoutCurrentUser(completion:UsergridResponseCompletion? = nil) {
        guard let uuidOrUsername = self.currentUser?.uuidOrUsername,
              let token = self.currentUser?.auth?.accessToken
        else {
            completion?(response:UsergridResponse(client: self, errorName: "Logout Failed.", errorDescription: "UsergridClient's currentUser is not valid."))
            return
        }

        self.logoutUser(uuidOrUsername, token: token)
    }

    /**
    Logs out the user remotely with the given tokens.

    - parameter completion: The completion block that will be called after logout has completed.
    */
    public func logoutUserAllTokens(uuidOrUsername:String, completion:UsergridResponseCompletion? = nil) {
        self.logoutUser(uuidOrUsername, token: nil, completion: completion)
    }

    /**
    Logs out a user with the give UUID or username using the shared instance of `UsergridCient`.

    Passing in a token will log out the user for just that token.  Passing in nil for the token will logout the user for all tokens.

    - parameter completion: The completion block that will be called after logout has completed.
    */
    public func logoutUser(uuidOrUsername:String, token:String?, completion:UsergridResponseCompletion? = nil) {
        var paths = ["users",uuidOrUsername]
        var queryParams: [String: String]?
        if let accessToken = token {
            paths.append("revoketoken")
            queryParams = ["token": accessToken]
        } else {
            paths.append("revoketokens")
        }
        let request = UsergridRequest(method: .Put,
                                      baseUrl: self.clientAppURL,
                                      paths: paths,
                                      auth: self.authForRequests(),
                                      queryParams: queryParams)

        self.sendRequest(request) { response in
            if uuidOrUsername == self.currentUser?.uuidOrUsername { // Check to see if this user is the currentUser
                if response.ok || response.error?.errorName == "auth_bad_access_token" { // If the logout was successful or if we have a bad token reset things.
                    self.currentUser?.auth = nil
                    self.currentUser = nil
                }
            }
            completion?(response: response)
        }
    }

    // MARK: - Generic Request Methods -

    /**
    Starts the `UsergridRequest` sending process.
    
    - Note: This method should only be used when you construct your own `UsergridRequest` objects.

    - parameter request:    The `UsergridRequest` object to send.
    - parameter completion: The optional completion block that will be called once the request has completed.
    */
    public func sendRequest(request:UsergridRequest, completion:UsergridResponseCompletion? = nil) {
        _requestManager.performRequest(request, completion: completion)
    }

    // MARK: - GET -

    /**
    Gets a single `UsergridEntity` of a given type with a specific UUID/name.

    - parameter type:       The `UsergridEntity` type.
    - parameter uuidOrName: The UUID or name of the `UsergridEntity`.
    - parameter completion: The optional completion block that will be called once the request has completed.
    */
    public func GET(type: String, uuidOrName: String, completion: UsergridResponseCompletion? = nil) {
        let request = UsergridRequest(method: .Get, baseUrl: self.clientAppURL, paths: [type,uuidOrName], auth:self.authForRequests())
        self.sendRequest(request, completion: completion)
    }

    /**
     Gets a group of `UsergridEntity` objects of a given type.

     - parameter type:       The `UsergridEntity` type.
     - parameter completion: The optional completion block that will be called once the request has completed.
     */
    public func GET(type: String, completion: UsergridResponseCompletion? = nil) {
        let request = UsergridRequest(method: .Get, baseUrl: self.clientAppURL, paths: [type], query: nil, auth: self.authForRequests())
        self.sendRequest(request, completion: completion)
    }

    /**
    Gets a group of `UsergridEntity` objects using a given query.

    - parameter query:           The query to use when gathering `UsergridEntity` objects.
    - parameter queryCompletion: The optional completion block that will be called once the request has completed.
    */
    public func GET(query: UsergridQuery, queryCompletion: UsergridResponseCompletion? = nil) {
        guard let type = query.collectionName
            else {
                queryCompletion?(response: UsergridResponse(client:self, errorName: "Query collection name missing.", errorDescription: "Query collection name is missing."))
                return
        }

        let request = UsergridRequest(method: .Get, baseUrl: self.clientAppURL, paths: [type], query: query, auth: self.authForRequests())
        self.sendRequest(request, completion: queryCompletion)
    }

    // MARK: - PUT -

    /**
    Updates an `UsergridEntity` with the given type and UUID/name specified using the passed in jsonBody.

    - parameter type:       The `UsergridEntity` type.
    - parameter uuidOrName: The UUID or name of the `UsergridEntity`.
    - parameter jsonBody:   The valid JSON body dictionary to update the `UsergridEntity` with.
    - parameter completion: The optional completion block that will be called once the request has completed.
    */
    public func PUT(type: String, uuidOrName: String, jsonBody:[String:AnyObject], completion: UsergridResponseCompletion? = nil) {
        let request = UsergridRequest(method: .Put,
                                      baseUrl: self.clientAppURL,
                                      paths: [type,uuidOrName],
                                      auth: self.authForRequests(),
                                      headers: UsergridRequest.JSON_CONTENT_TYPE_HEADER,
                                      jsonBody: jsonBody)
        self.sendRequest(request, completion: completion)
    }

    /**
    Updates the passed in `UsergridEntity`.

    - parameter entity:     The `UsergridEntity` to update.
    - parameter completion: The optional completion block that will be called once the request has completed.
    */
    public func PUT(entity: UsergridEntity, completion: UsergridResponseCompletion? = nil) {
        PUT(entity.type, jsonBody: entity.jsonObjectValue, completion: completion)
    }

    /**
    Updates an `UsergridEntity` with the given type using the jsonBody where the UUID/name is specified inside of the jsonBody.

    - Note: The `jsonBody` must contain a valid value for either `uuid` or `name` keys.

    - parameter type:       The `UsergridEntity` type.
    - parameter jsonBody:   The valid JSON body dictionary to update the `UsergridEntity` with.
    - parameter completion: The optional completion block that will be called once the request has completed.
    */
    public func PUT(type: String, jsonBody:[String:AnyObject], completion: UsergridResponseCompletion? = nil) {
        guard let uuidOrName = (jsonBody[UsergridEntityProperties.UUID.stringValue] ?? jsonBody[UsergridEntityProperties.Name.stringValue]) as? String
        else {
            completion?(response: UsergridResponse(client:self, errorName: "jsonBody not valid.", errorDescription: "The `jsonBody` must contain a valid value for either `uuid` or `name`."))
            return
        }
        let request = UsergridRequest(method: .Put,
                                      baseUrl: self.clientAppURL,
                                      paths: [type,uuidOrName],
                                      auth: self.authForRequests(),
                                      headers: UsergridRequest.JSON_CONTENT_TYPE_HEADER,
                                      jsonBody: jsonBody)
        self.sendRequest(request, completion: completion)
    }

    /**
    Updates the entities that fit the given query using the passed in jsonBody.

    - Note: The query parameter must have a valid `collectionName` before calling this method.

    - parameter query:           The query to use when filtering what entities to update.
    - parameter jsonBody:        The valid JSON body dictionary to update with.
    - parameter queryCompletion: The optional completion block that will be called once the request has completed.
    */
    public func PUT(query: UsergridQuery, jsonBody:[String:AnyObject], queryCompletion: UsergridResponseCompletion? = nil) {
        guard let type = query.collectionName
        else {
            queryCompletion?(response: UsergridResponse(client:self, errorName: "Query collection name invalid.", errorDescription: "Query is missing a collection name."))
            return
        }
        let request = UsergridRequest(method: .Put,
                                      baseUrl: self.clientAppURL,
                                      paths: [type],
                                      query: query,
                                      auth: self.authForRequests(),
                                      headers: UsergridRequest.JSON_CONTENT_TYPE_HEADER,
                                      jsonBody: jsonBody)
        self.sendRequest(request, completion: queryCompletion)
    }

    // MARK: - POST -

    /**
    Creates and posts creates an `UsergridEntity`.
    - parameter entity:     The `UsergridEntity` to create.
    - parameter completion: The optional completion block that will be called once the request has completed.
    */
    public func POST(entity:UsergridEntity, completion: UsergridResponseCompletion? = nil) {
        let request = UsergridRequest(method: .Post,
                                      baseUrl: self.clientAppURL,
                                      paths: [entity.type],
                                      auth: self.authForRequests(),
                                      headers: UsergridRequest.JSON_CONTENT_TYPE_HEADER,
                                      jsonBody: entity.jsonObjectValue)
        self.sendRequest(request, completion: completion)
    }

    /**
    Creates and posts an array of `UsergridEntity` objects.

    - Note: Each `UsergridEntity` in the array much already have a type assigned and must be the same.

    - parameter entities:           The `UsergridEntity` objects to create.
    - parameter entitiesCompletion: The optional completion block that will be called once the request has completed.
    */
    public func POST(entities:[UsergridEntity], entitiesCompletion: UsergridResponseCompletion? = nil) {
        guard let type = entities.first?.type
        else {
            entitiesCompletion?(response: UsergridResponse(client:self, errorName: "No type found.", errorDescription: "The first entity in the array had no type found."))
            return
        }
        POST(type, jsonBodies: entities.map { return ($0).jsonObjectValue }, completion: entitiesCompletion)
    }

    /**
    Creates and posts an `UsergridEntity` of the given type with the given jsonBody.

    - parameter type:       The `UsergridEntity` type.
    - parameter jsonBody:   The valid JSON body dictionary to use when creating the `UsergridEntity`.
    - parameter completion: The optional completion block that will be called once the request has completed.
    */
    public func POST(type: String, jsonBody:[String:AnyObject], completion: UsergridResponseCompletion? = nil) {
        let request = UsergridRequest(method: .Post,
                                      baseUrl: self.clientAppURL,
                                      paths: [type],
                                      auth: self.authForRequests(),
                                      headers: UsergridRequest.JSON_CONTENT_TYPE_HEADER,
                                      jsonBody: jsonBody)
        self.sendRequest(request, completion: completion)
    }

    /**
    Creates and posts an array of `Entity` objects while assigning the given type to them.

    - parameter type:       The `UsergridEntity` type.
    - parameter jsonBody:   The valid JSON body dictionaries to use when creating the `UsergridEntity` objects.
    - parameter completion: The optional completion block that will be called once the request has completed.
    */
    public func POST(type: String, jsonBodies:[[String:AnyObject]], completion: UsergridResponseCompletion? = nil) {
        let request = UsergridRequest(method: .Post,
                                      baseUrl: self.clientAppURL,
                                      paths: [type],
                                      auth: self.authForRequests(),
                                      headers: UsergridRequest.JSON_CONTENT_TYPE_HEADER,
                                      jsonBody: jsonBodies)
        self.sendRequest(request, completion: completion)
    }

    /**
    Creates and posts an `UsergridEntity` of the given type with a given name and the given jsonBody.

    - parameter type:       The `UsergridEntity` type.
    - parameter name:       The name of the `UsergridEntity`.
    - parameter jsonBody:   The valid JSON body dictionary to use when creating the `UsergridEntity`.
    - parameter completion: The optional completion block that will be called once the request has completed.
    */
    public func POST(type: String, name: String, jsonBody:[String:AnyObject], completion: UsergridResponseCompletion? = nil) {
        var jsonBodyWithName = jsonBody
        jsonBodyWithName[UsergridEntityProperties.Name.stringValue] = name
        let request = UsergridRequest(method: .Post,
                                      baseUrl: self.clientAppURL,
                                      paths: [type],
                                      auth: self.authForRequests(),
                                      headers: UsergridRequest.JSON_CONTENT_TYPE_HEADER,
                                      jsonBody: jsonBodyWithName)
        self.sendRequest(request, completion: completion)

    }

    // MARK: - DELETE -

    /**
    Destroys the passed `UsergridEntity`.

    - Note: The entity object must have a `uuid` or `name` assigned.

    - parameter entity:     The `UsergridEntity` to delete.
    - parameter completion: The optional completion block that will be called once the request has completed.
    */
    public func DELETE(entity:UsergridEntity, completion: UsergridResponseCompletion? = nil) {
        guard let uuidOrName = entity.uuidOrName
        else {
            completion?(response: UsergridResponse(client:self, errorName: "No UUID or name found.", errorDescription: "The entity object must have a `uuid` or `name` assigned."))
            return
        }

        DELETE(entity.type, uuidOrName: uuidOrName, completion: completion)
    }

    /**
    Destroys the `UsergridEntity` objects that fit the given `UsergridQuery`.

    - Note: The query parameter must have a valid `collectionName` before calling this method.

    - parameter query:              The query to use when filtering what entities to delete.
    - parameter queryCompletion:    The optional completion block that will be called once the request has completed.
    */
    public func DELETE(query:UsergridQuery, queryCompletion: UsergridResponseCompletion? = nil) {
        guard let type = query.collectionName
        else {
            queryCompletion?(response: UsergridResponse(client:self, errorName: "Query collection name invalid.", errorDescription: "Query is missing a collection name."))
            return
        }

        let request = UsergridRequest(method: .Delete,
                                      baseUrl: self.clientAppURL,
                                      paths: [type],
                                      query: query,
                                      auth: self.authForRequests(),
                                      headers: UsergridRequest.JSON_CONTENT_TYPE_HEADER)
        self.sendRequest(request, completion: queryCompletion)
    }

    /**
    Destroys the `UsergridEntity` of a given type with a specific UUID/name.

    - parameter type:       The `UsergridEntity` type.
    - parameter uuidOrName: The UUID or name of the `UsergridEntity`.
    - parameter completion: The optional completion block that will be called once the request has completed.
    */
    public func DELETE(type:String, uuidOrName: String, completion: UsergridResponseCompletion? = nil) {
        let request = UsergridRequest(method: .Delete,
                                      baseUrl: self.clientAppURL,
                                      paths: [type,uuidOrName],
                                      auth: self.authForRequests(),
                                      headers: UsergridRequest.JSON_CONTENT_TYPE_HEADER)
        self.sendRequest(request, completion: completion)
    }

    // MARK: - Connection Management -

    /**
    Connects the `UsergridEntity` objects via the relationship.

    - parameter entity:             The `UsergridEntity` that will contain the connection.
    - parameter relationship:       The relationship of the connection.
    - parameter to:                 The `UsergridEntity` which is connected.
    - parameter completion:         The optional completion block that will be called once the request has completed.
    */
    public func connect(entity:UsergridEntity, relationship:String, to:UsergridEntity, completion: UsergridResponseCompletion? = nil) {
        guard let entityID = entity.uuidOrName,
              let toID = to.uuidOrName
        else {
            completion?(response: UsergridResponse(client: self, errorName: "Invalid Entity Connection Attempt.", errorDescription: "One or both entities that are attempting to be connected do not contain a valid UUID or Name property."))
            return
        }
        self.connect(entity.type, entityID: entityID, relationship: relationship, toType: to.type, toID: toID, completion: completion)
    }

    /**
     Connects the entity objects via the relationship.

     - parameter entityType:       The entity type.
     - parameter entityID:         The entity UUID or name.
     - parameter relationship:     The relationship of the connection.
     - parameter toType:           The type of the entity you are connecting to.
     - parameter toName:           The name of the entity you are connecting to.
     - parameter completion:       The optional completion block that will be called once the request has completed.
     */
    public func connect(entityType:String, entityID:String, relationship:String, toType:String, toName: String, completion: UsergridResponseCompletion? = nil) {
        self.connect(entityType, entityID: entityID, relationship: relationship, toType: toType, toID: toName, completion: completion)
    }

    /**
     Connects the entity objects via the relationship.

     - parameter entityType:       The entity type.
     - parameter entityID:         The entity UUID or name.
     - parameter relationship:     The relationship of the connection.
     - parameter toType:           The optional type of the entity you are connecting to.
     - parameter toID:             The UUID of the entity you are connecting to.
     - parameter completion:       The optional completion block that will be called once the request has completed.
     */
    public func connect(entityType:String, entityID:String, relationship:String, toType:String?, toID: String, completion: UsergridResponseCompletion? = nil) {
        var paths = [entityType,entityID,relationship]
        if let toType = toType {
            paths.append(toType)
        }
        paths.append(toID)

        let request = UsergridRequest(method: .Post,
                                      baseUrl: self.clientAppURL,
                                      paths: paths,
                                      auth: self.authForRequests())
        self.sendRequest(request, completion: completion)
    }

    /**
    Disconnects the `UsergridEntity` objects via the relationship.

    - parameter entity:             The `UsergridEntity` that contains the connection.
    - parameter relationship:       The relationship of the connection.
    - parameter from:               The `UsergridEntity` which is connected.
    - parameter completion:         The optional completion block that will be called once the request has completed.
    */
    public func disconnect(entity:UsergridEntity, relationship:String, from:UsergridEntity, completion: UsergridResponseCompletion? = nil) {
        guard let entityID = entity.uuidOrName,
              let fromID = from.uuidOrName
        else {
            completion?(response: UsergridResponse(client: self, errorName: "Invalid Entity Disconnect Attempt.", errorDescription: "The connecting and connected entities must have a `uuid` or `name` assigned."))
            return
        }

        self.disconnect(entity.type, entityID: entityID, relationship: relationship, fromType: from.type, fromID: fromID, completion: completion)
    }

    /**
     Disconnects the entity objects via the relationship.

     - parameter entityType:       The entity type.
     - parameter entityID:         The entity UUID or name.
     - parameter relationship:     The relationship of the connection.
     - parameter fromType:         The type of the entity you are disconnecting from.
     - parameter fromName:         The name of the entity you are disconnecting from.
     - parameter completion:       The optional completion block that will be called once the request has completed.
     */
    public func disconnect(entityType:String, entityID:String, relationship:String, fromType:String, fromName: String, completion: UsergridResponseCompletion? = nil) {
        self.disconnect(entityType, entityID: entityID, relationship: relationship, fromType: fromType, fromID: fromName, completion: completion)
    }

    /**
     Disconnects the entity objects via the relationship.

     - parameter entityType:       The entity type.
     - parameter entityID:         The entity UUID or name.
     - parameter relationship:     The relationship of the connection.
     - parameter fromType:         The optional type of the entity you are disconnecting from.
     - parameter toID:             The UUID of the entity you are disconnecting from.
     - parameter completion:       The optional completion block that will be called once the request has completed.
     */
    public func disconnect(entityType:String, entityID:String, relationship:String, fromType:String?, fromID: String, completion: UsergridResponseCompletion? = nil) {

        var paths = [entityType,entityID,relationship]
        if let fromType = fromType {
            paths.append(fromType)
        }
        paths.append(fromID)

        let request = UsergridRequest(method: .Delete,
                                      baseUrl: self.clientAppURL,
                                      paths: paths,
                                      auth: self.authForRequests())
        self.sendRequest(request, completion: completion)
    }

    /**
    Gets the connected entities for the given relationship.

    - parameter entity:       The entity that contains the connection.
    - parameter relationship: The relationship of the connection.
    - parameter query:        The optional query.
    - parameter completion:   The optional completion block that will be called once the request has completed.
    */
    public func getConnections(direction:UsergridDirection, entity:UsergridEntity, relationship:String, query:UsergridQuery? = nil, completion:UsergridResponseCompletion? = nil) {
        guard let uuidOrName = entity.uuidOrName
        else {
            completion?(response: UsergridResponse(client: self, errorName: "Invalid Entity Get Connections Attempt.", errorDescription: "The entity must have a `uuid` or `name` assigned."))
            return
        }
        self.getConnections(direction, type: entity.type, uuidOrName: uuidOrName, relationship: relationship, query:query, completion: completion)
    }

    /**
     Gets the connected entities for the given relationship.

     - parameter direction:        The direction of the connection.
     - parameter type:             The entity type.
     - parameter uuidOrName:       The entity UUID or name.
     - parameter relationship:     The relationship of the connection.
     - parameter query:            The optional query.
     - parameter completion:       The optional completion block that will be called once the request has completed.
     */
    public func getConnections(direction:UsergridDirection, type:String, uuidOrName:String, relationship:String, query:UsergridQuery? = nil, completion:UsergridResponseCompletion? = nil) {
        let request = UsergridRequest(method: .Get,
                                      baseUrl: self.clientAppURL,
                                      paths: [type, uuidOrName, direction.connectionValue, relationship],
                                      query: query,
                                      auth: self.authForRequests())
        self.sendRequest(request, completion: completion)
    }

    /**
     Gets the connected entities for the given relationship.

     - parameter direction:    The direction of the connection.
     - parameter uuid:         The entity UUID.
     - parameter relationship: The relationship of the connection.
     - parameter query:        The optional query.
     - parameter completion:   The optional completion block that will be called once the request has completed.
     */
    public func getConnections(direction:UsergridDirection, uuid:String, relationship:String, query:UsergridQuery? = nil, completion:UsergridResponseCompletion? = nil) {
        let request = UsergridRequest(method: .Get,
            baseUrl: self.clientAppURL,
            paths: [uuid, direction.connectionValue, relationship],
            query: query,
            auth: self.authForRequests())
        self.sendRequest(request, completion: completion)
    }

    // MARK: - Asset Management -

    /**
    Uploads the asset and connects the data to the given `UsergridEntity`.

    - parameter entity:     The `UsergridEntity` to connect the asset to.
    - parameter asset:      The `UsergridAsset` to upload.
    - parameter progress:   The optional progress block that will be called to update the progress of the upload.
    - parameter completion: The optional completion block that will be called once the request has completed.
    */
    public func uploadAsset(entity:UsergridEntity, asset:UsergridAsset, progress:UsergridAssetRequestProgress? = nil, completion:UsergridAssetUploadCompletion? = nil) {
        let assetRequest = UsergridAssetUploadRequest(baseUrl: self.clientAppURL,
                                                      paths: [entity.type,entity.uuidOrName!],
                                                      auth: self.authForRequests(),
                                                      asset: asset)

        _requestManager.performAssetUpload(assetRequest, progress: progress) { asset, response in
            if response.ok {
                entity.asset = asset
                if let responseEntityFileMetaData = response.entity?.fileMetaData {
                    entity.fileMetaData = responseEntityFileMetaData
                }
            }
            completion?(asset: asset, response: response)
        }
    }

    /**
    Downloads the asset from the given `UsergridEntity`.

    - parameter entity:         The `UsergridEntity` to which the asset to.
    - parameter contentType:    The content type of the asset's data.
    - parameter progress:       The optional progress block that will be called to update the progress of the download.
    - parameter completion:     The optional completion block that will be called once the request has completed.
    */
    public func downloadAsset(entity:UsergridEntity, contentType:String, progress:UsergridAssetRequestProgress? = nil, completion:UsergridAssetDownloadCompletion? = nil) {
        guard entity.hasAsset
        else {
            completion?(asset: nil, error: UsergridResponseError(errorName: "Download asset failed.", errorDescription: "Entity does not have an asset attached."))
            return
        }

        let downloadAssetRequest = UsergridRequest(method: .Get,
                                                   baseUrl: self.clientAppURL,
                                                   paths: [entity.type,entity.uuidOrName!],
                                                   auth: self.authForRequests(),
                                                   headers:  ["Accept":contentType])

        _requestManager.performAssetDownload(contentType, usergridRequest: downloadAssetRequest, progress: progress, completion: { (asset, error) -> Void in
            entity.asset = asset
            completion?(asset: asset, error: error)
        })
    }
}
