//
//  Usergrid.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 7/21/15.
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

/// The version number for the Usergrid Swift SDK.
public let UsergridSDKVersion = "2.1.1"

/**
 The `Usergrid` class acts as a static shared instance manager for the `UsergridClient` class.

 The methods and variables in this class are all static and therefore you will never need or want to initialize an instance of the `Usergrid` class.

 Use of this class depends on initialization of the shared instance of the `UsergridClient` object.  Because of this, before using any of the static methods
 provided you will need to call one of the shared instance initialization methods.  Failure to do so will result in failure from all methods.
 */
public class Usergrid: NSObject {

    // MARK: - Static Variables -

    internal static var _sharedClient : UsergridClient!

    /// Used to determine if the shared instance of the `UsergridClient` has been initialized.
    public static var isInitialized : Bool  { return Usergrid._sharedClient != nil }

    /**
    A shared instance of `UsergridClient`, used by the `Usergrid` static methods and acts as the default `UsergridClient`
    within the UsergridSDK library.

    - Warning: You must call one of the `Usergrid.initSharedInstance` methods before this or any other `Usergrid` static methods are valid.
    */
    public static var sharedInstance : UsergridClient {
        assert(Usergrid.isInitialized, "Usergrid shared instance is not initalized!")
        return Usergrid._sharedClient
    }

    /// The application identifier the shared instance of `UsergridClient`.
    public static var appId : String { return Usergrid.sharedInstance.appId }

    /// The organization identifier of the shared instance of `UsergridClient`.
    public static var orgId : String { return Usergrid.sharedInstance.orgId }

    /// The base URL that all calls will be made with of the shared instance of `UsergridClient`.
    public static var baseUrl : String { return Usergrid.sharedInstance.baseUrl }

    /// The constructed URL string based on the `UsergridClient`'s baseUrl, orgId, and appId of the shared instance of `UsergridClient`.
    public static var clientAppURL : String { return Usergrid.sharedInstance.clientAppURL }

    /// The currently logged in `UsergridUser` of the shared instance of `UsergridClient`.
    public static var currentUser: UsergridUser?  { return Usergrid.sharedInstance.currentUser }

    /// Whether or not the current user will be saved and restored from the keychain using the shared instance of `UsergridClient`.
    public static var persistCurrentUserInKeychain: Bool {
        get { return Usergrid.sharedInstance.persistCurrentUserInKeychain }
        set(persist) { Usergrid.sharedInstance.persistCurrentUserInKeychain = persist }
    }

    /// The `UsergridUserAuth` which consists of the token information from the `currentUser` property of the shared instance of `UsergridClient`.
    public static var userAuth: UsergridUserAuth?  { return Usergrid.sharedInstance.userAuth }

    /// The application level `UsergridAppAuth` object of the shared instance of `UsergridClient`.
    public static var appAuth: UsergridAppAuth?  {
        get { return Usergrid.sharedInstance.appAuth }
        set(auth) { Usergrid.sharedInstance.appAuth = auth }
    }

    /// The `UsergridAuthMode` value used to determine what type of token will be sent of the shared instance of `UsergridClient`, if any.
    public static var authMode: UsergridAuthMode {
        get { return Usergrid.sharedInstance.authMode }
        set(mode) { Usergrid.sharedInstance.authMode = mode }
    }

    // MARK: - Initialization -

    /**
    Initializes the `Usergrid.sharedInstance` of `UsergridClient`.

    - parameter orgId: The organization identifier.
    - parameter appId: The application identifier.

    - returns: The shared instance of `UsergridClient`.
    */
    public static func initSharedInstance(orgId orgId : String, appId: String) -> UsergridClient {
        if !Usergrid.isInitialized {
            Usergrid._sharedClient = UsergridClient(orgId: orgId, appId: appId)
        } else {
            print("The Usergrid shared instance was already initialized. All subsequent initialization attempts (including this) will be ignored.")
        }
        return Usergrid._sharedClient
    }

    /**
    Initializes the `Usergrid.sharedInstance` of `UsergridClient`.

    - parameter orgId:      The organization identifier.
    - parameter appId:      The application identifier.
    - parameter baseUrl:    The base URL that all calls will be made with.

    - returns: The shared instance of `UsergridClient`.
    */
    public static func initSharedInstance(orgId orgId : String, appId: String, baseUrl: String) -> UsergridClient {
        if !Usergrid.isInitialized {
            Usergrid._sharedClient = UsergridClient(orgId: orgId, appId: appId, baseUrl: baseUrl)
        } else {
            print("The Usergrid shared instance was already initialized. All subsequent initialization attempts (including this) will be ignored.")
        }
        return Usergrid._sharedClient
    }

    /**
    Initializes the `Usergrid.sharedInstance` of `UsergridClient`.

    - parameter configuration: The configuration for the client to be set up with.
    
    - returns: The shared instance of `UsergridClient`.
    */
    public static func initSharedInstance(configuration configuration: UsergridClientConfig) -> UsergridClient {
        if !Usergrid.isInitialized {
            Usergrid._sharedClient = UsergridClient(configuration: configuration)
        }  else {
            print("The Usergrid shared instance was already initialized. All subsequent initialization attempts (including this) will be ignored.")
        }
        return Usergrid._sharedClient
    }

    // MARK: - Push Notifications -

    /**
    Sets the push token for the given notifier ID and performs a PUT request to update the shared `UsergridDevice` instance using the shared instance of `UsergridCient`.

    - parameter pushToken:  The push token from Apple.
    - parameter notifierID: The Usergrid notifier ID.
    - parameter completion: The completion block.
    */
    public static func applyPushToken(pushToken: NSData, notifierID: String, completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.applyPushToken(pushToken, notifierID: notifierID, completion: completion)
    }

    /**
    Sets the push token for the given notifier ID and performs a PUT request to update the given `UsergridDevice` instance using the shared instance of `UsergridCient`.

    - parameter device:     The `UsergridDevice` object.
    - parameter pushToken:  The push token from Apple.
    - parameter notifierID: The Usergrid notifier ID.
    - parameter completion: The completion block.
    */
    public static func applyPushToken(device: UsergridDevice, pushToken: NSData, notifierID: String, completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.applyPushToken(device, pushToken: pushToken, notifierID: notifierID, completion: completion)
    }


    // MARK: - Authorization -

    /**
     Sets the shared `UsergridClient`'s `tempAuth` property using the passed in `UsergridAuth`.

     This will cause the next CRUD method performed by the client to use the `tempAuth` property once and will then reset.

     - parameter auth: The `UsergridAuth` object to temporarily use for authentication.

     - returns: The shared instance of `UsergridClient`
     */
    public static func usingAuth(auth:UsergridAuth) -> UsergridClient {
        return Usergrid.sharedInstance.usingAuth(auth)
    }

    /**
     Sets the shared `UsergridClient`'s `tempAuth` property using the passed in token.

     This will cause the next CRUD method performed by the client to use the `tempAuth` property once and will then reset.

     - parameter auth: The access token to temporarily use for authentication.

     - returns: The shared instance of `UsergridClient`
     */
    public static func usingToken(token:String) -> UsergridClient {
        return Usergrid.sharedInstance.usingToken(token)
    }


    /**
    Determines the `UsergridAuth` object that will be used for all outgoing requests made by the shared instance of `UsergridClient`.

    If there is a `UsergridUser` logged in and the token of that user is valid then it will return that.

    Otherwise, if the `authMode` is `.App`, and the `UsergridAppAuth` of the client is set and the token is valid it will return that.

    - returns: The `UsergridAuth` if one is found or nil if not.
    */
    public static func authForRequests() -> UsergridAuth? {
        return Usergrid.sharedInstance.authForRequests()
    }

    /**
    Authenticates with the `UsergridAppAuth` that is contained within the shared instance of `UsergridCient`.

    - parameter completion: The completion block that will be called after authentication has completed.
    */
    public static func authenticateApp(completion: UsergridAppAuthCompletionBlock? = nil) {
        Usergrid.sharedInstance.authenticateApp(completion)
    }

    /**
    Authenticates with the `UsergridAppAuth` that is passed in using the shared instance of `UsergridCient`.

    - parameter auth:       The `UsergridAppAuth` that will be authenticated.
    - parameter completion: The completion block that will be called after authentication has completed.
    */
    public static func authenticateApp(auth: UsergridAppAuth, completion: UsergridAppAuthCompletionBlock? = nil) {
        Usergrid.sharedInstance.authenticateApp(auth, completion: completion)
    }

    /**
    Authenticates with the `UsergridUserAuth` that is passed in using the shared instance of `UsergridCient`.

    - parameter auth:       The `UsergridUserAuth` that will be authenticated.
    - parameter completion: The completion block that will be called after authentication has completed.
    */
    public static func authenticateUser(auth: UsergridUserAuth, completion: UsergridUserAuthCompletionBlock? = nil) {
        Usergrid.sharedInstance.authenticateUser(auth, completion: completion)
    }

    /**
    Authenticates with the `UsergridUserAuth` that is passed in using the shared instance of `UsergridCient`.

    - parameter auth:               The `UsergridUserAuth` that will be authenticated.
    - parameter setAsCurrentUser:   If the authenticated user should be set as the `UsergridClient.currentUser`.
    - parameter completion:         The completion block that will be called after authentication has completed.
    */
    public static func authenticateUser(userAuth: UsergridUserAuth, setAsCurrentUser:Bool, completion: UsergridUserAuthCompletionBlock? = nil) {
        Usergrid.sharedInstance.authenticateUser(userAuth, setAsCurrentUser: setAsCurrentUser, completion: completion)
    }

    /**
     Changes the given `UsergridUser`'s current password with the shared instance of `UsergridClient`.

     - parameter user:       The user.
     - parameter old:        The old password.
     - parameter new:        The new password.
     - parameter completion: The optional completion block.
     */
    public static func resetPassword(user: UsergridUser, old:String, new:String, completion:UsergridUserResetPasswordCompletion? = nil) {
        Usergrid.sharedInstance.resetPassword(user, old: old, new: new, completion: completion)
    }

    /**
    Logs out the current user of the shared instance locally and remotely using the shared instance of `UsergridClient`.

    - parameter completion: The completion block that will be called after logout has completed.
    */
    public static func logoutCurrentUser(completion:UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.logoutCurrentUser(completion)
    }

    /**
    Logs out the user remotely with the given tokens using the shared instance of `UsergridCient`.

    - parameter completion: The completion block that will be called after logout has completed.
    */
    public static func logoutUserAllTokens(uuidOrUsername:String, completion:UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.logoutUserAllTokens(uuidOrUsername, completion: completion)
    }

    /**
    Logs out a user with the give UUID or username using the shared instance of `UsergridCient`.
    
    Passing in a token will log out the user for just that token.  Passing in nil for the token will logout the user for all tokens.

    - parameter completion: The completion block that will be called after logout has completed.
    */
    public static func logoutUser(uuidOrUsername:String, token:String?, completion:UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.logoutUser(uuidOrUsername, token: token, completion: completion)
    }

    // MARK: - Generic Request Methods -

    /**
    Starts the `UsergridRequest` sending process using the shared instance of `UsergridCient`.

    - Note: This method should only be used when you construct your own `UsergridRequest objects.

    - parameter request:    The `UsergridRequest` object to send.
    - parameter completion: The optional completion block that will be called once the request has completed.
    */
    public static func sendRequest(request:UsergridRequest, completion:UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.sendRequest(request, completion: completion)
    }

    // MARK: - GET -

    /**
    Gets a single `UsergridEntity` of a given type with a specific UUID/name using the shared instance of `UsergridCient`.

    - parameter type:       The `UsergridEntity` type.
    - parameter uuidOrName: The UUID or name of the `UsergridEntity`.
    - parameter completion: The completion block that will be called once the request has completed.
    */
    public static func GET(type: String, uuidOrName: String, completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.GET(type,uuidOrName:uuidOrName,completion:completion)
    }

    /**
     Gets a group of `UsergridEntity` objects of a given type  using the shared instance of `UsergridCient`.

     - parameter type:       The `UsergridEntity` type.
     - parameter completion: The optional completion block that will be called once the request has completed.
     */
    public static func GET(type: String, completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.GET(type,completion:completion)
    }

    /**
    Gets a group of `UsergridEntity` objects with a given query using the shared instance of `UsergridCient`.

    - parameter query:           The query to use when gathering `UsergridEntity` objects.
    - parameter queryCompletion: The completion block that will be called once the request has completed.
    */
    public static func GET(query: UsergridQuery, queryCompletion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.GET(query,queryCompletion:queryCompletion)
    }

    // MARK: - PUT -

    /**
    Updates an `UsergridEntity` with the given type and UUID/name specified using the passed in jsonBody using the shared instance of `UsergridCient`.

    - parameter type:       The `UsergridEntity` type.
    - parameter uuidOrName: The UUID or name of the `UsergridEntity`.
    - parameter jsonBody:   The valid JSON body dictionary to update the `UsergridEntity` with.
    - parameter completion: The completion block that will be called once the request has completed.
    */
    public static func PUT(type: String, uuidOrName: String, jsonBody:[String:AnyObject], completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.PUT(type, uuidOrName: uuidOrName, jsonBody: jsonBody, completion: completion)
    }

    /**
    Updates an `UsergridEntity` with the given type using the jsonBody where the UUID/name is specified inside of the jsonBody using the shared instance of `UsergridCient`.

    - Note: The `jsonBody` must contain a valid value for either `uuid` or `name`.

    - parameter type:       The `UsergridEntity` type.
    - parameter jsonBody:   The valid JSON body dictionary to update the `UsergridEntity` with.
    - parameter completion: The completion block that will be called once the request has completed.
    */
    public static func PUT(type: String, jsonBody:[String:AnyObject], completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.PUT(type, jsonBody: jsonBody, completion: completion)
    }

    /**
    Updates the passed in `UsergridEntity` using the shared instance of `UsergridCient`.

    - parameter entity:     The `UsergridEntity` to update.
    - parameter completion: The completion block that will be called once the request has completed.
    */
    public static func PUT(entity: UsergridEntity, completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.PUT(entity, completion: completion)
    }

    /**
    Updates the entities that fit the given query using the passed in jsonBody using the shared instance of `UsergridCient`.

    - Note: The query parameter must have a valid `collectionName` before calling this method.

    - parameter query:              The query to use when filtering what entities to update.
    - parameter jsonBody:           The valid JSON body dictionary to update with.
    - parameter queryCompletion:    The completion block that will be called once the request has completed.
    */
    public static func PUT(query: UsergridQuery, jsonBody:[String:AnyObject], queryCompletion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.PUT(query, jsonBody: jsonBody, queryCompletion: queryCompletion)
    }

    // MARK: - POST -

    /**
    Creates and posts an `UsergridEntity` of the given type with a given name and the given jsonBody using the shared instance of `UsergridCient`.

    - parameter type:       The `UsergridEntity` type.
    - parameter name:       The name of the `UsergridEntity`.
    - parameter jsonBody:   The valid JSON body dictionary to use when creating the `UsergridEntity`.
    - parameter completion: The completion block that will be called once the request has completed.
    */
    public static func POST(type: String, name: String, jsonBody:[String:AnyObject], completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.POST(type, name: name, jsonBody: jsonBody, completion: completion)
    }

    /**
    Creates and posts an `UsergridEntity` of the given type with the given jsonBody using the shared instance of `UsergridCient`.

    - parameter type:       The `UsergridEntity` type.
    - parameter jsonBody:   The valid JSON body dictionary to use when creating the `UsergridEntity`.
    - parameter completion: The completion block that will be called once the request has completed.
    */
    public static func POST(type: String, jsonBody:[String:AnyObject], completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.POST(type, jsonBody: jsonBody, completion: completion)
    }

    /**
    Creates and posts an array of `Entity` objects while assinging the given type to them using the shared instance of `UsergridCient`.

    - parameter type:       The `UsergridEntity` type.
    - parameter jsonBody:   The valid JSON body dictionaries to use when creating the `UsergridEntity` objects.
    - parameter completion: The completion block that will be called once the request has completed.
    */
    public static func POST(type: String, jsonBodies:[[String:AnyObject]], completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.POST(type, jsonBodies: jsonBodies, completion: completion)
    }

    /**
    Creates and posts creates an `UsergridEntity` using the shared instance of `UsergridCient`.

    - parameter entity:     The `UsergridEntity` to create.
    - parameter completion: The completion block that will be called once the request has completed.
    */
    public static func POST(entity:UsergridEntity, completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.POST(entity, completion: completion)
    }

    /**
    Creates and posts an array of `UsergridEntity` objects using the shared instance of `UsergridCient`.
    
    - Note: Each `UsergridEntity` in the array much already have a type assigned and must be the same.

    - parameter entities:           The `UsergridEntity` objects to create.
    - parameter entitiesCompletion: The completion block that will be called once the request has completed.
    */
    public static func POST(entities:[UsergridEntity], entitiesCompletion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.POST(entities, entitiesCompletion: entitiesCompletion)
    }

    // MARK: - DELETE -

    /**
    Destroys the `UsergridEntity` of a given type with a specific UUID/name using the shared instance of `UsergridCient`.

    - parameter type:       The `UsergridEntity` type.
    - parameter uuidOrName: The UUID or name of the `UsergridEntity`.
    - parameter completion: The completion block that will be called once the request has completed.
    */
    public static func DELETE(type:String, uuidOrName: String, completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.DELETE(type, uuidOrName: uuidOrName, completion: completion)
    }

    /**
    Destroys the passed `UsergridEntity` using the shared instance of `UsergridCient`.

    - Note: The entity object must have a `uuid` or `name` assigned.

    - parameter entity:     The `UsergridEntity` to delete.
    - parameter completion: The completion block that will be called once the request has completed.
    */
    public static func DELETE(entity:UsergridEntity, completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.DELETE(entity, completion:completion)
    }

    /**
    Destroys the `UsergridEntity` objects that fit the given `UsergridQuery` using the shared instance of `UsergridCient`.

    - Note: The query parameter must have a valid `collectionName` before calling this method.

    - parameter query:              The query to use when filtering what entities to delete.
    - parameter queryCompletion:    The completion block that will be called once the request has completed.
    */
    public static func DELETE(query:UsergridQuery, queryCompletion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.DELETE(query, queryCompletion:queryCompletion)
    }

    // MARK: - Connection Management -

    /**
    Connects the `UsergridEntity` objects via the relationship using the shared instance of `UsergridCient`.

    - parameter entity:             The entity that will contain the connection.
    - parameter relationship:       The relationship of the two entities.
    - parameter to:                 The entity which is connected.
    - parameter completion:         The completion block that will be called once the request has completed.
    */
    public static func connect(entity:UsergridEntity, relationship:String, to:UsergridEntity, completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.connect(entity, relationship: relationship, to: to, completion: completion)
    }

    /**
     Connects the entity objects via the relationship using the shared instance of `UsergridCient`.

     - parameter entityType:       The entity type.
     - parameter entityID:         The entity UUID or name.
     - parameter relationship:     The relationship of the connection.
     - parameter toType:           The optional type of the entity you are connecting to.
     - parameter toID:             The UUID of the entity you are connecting to.
     - parameter completion:       The completion block that will be called once the request has completed.
     */
    public static func connect(entityType:String, entityID:String, relationship:String, toType:String?, toID: String, completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.connect(entityType, entityID: entityID, relationship: relationship, toType: toType, toID: toID, completion: completion)
    }

    /**
     Connects the entity objects via the relationship using the shared instance of `UsergridCient`.

     - parameter entityType:       The entity type.
     - parameter entityID:         The entity UUID or name.
     - parameter relationship:     The relationship of the connection.
     - parameter toType:           The type of the entity you are connecting to.
     - parameter toName:           The name of the entity you are connecting to.
     - parameter completion:       The completion block that will be called once the request has completed.
     */
    public static func connect(entityType:String, entityID:String, relationship:String, toType:String, toName: String, completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.connect(entityType, entityID: entityID, relationship: relationship, toType: toType, toName: toName, completion: completion)
    }


    /**
    Disconnects the `UsergridEntity` objects via the relationship using the shared instance of `UsergridCient`.

    - parameter entity:             The entity that contains the connection.
    - parameter relationship:       The relationship of the two entities.
    - parameter connectingEntity:   The entity which is connected.
    - parameter completion:         The completion block that will be called once the request has completed.
    */
    public static func disconnect(entity:UsergridEntity, relationship:String, from:UsergridEntity, completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.disconnect(entity, relationship: relationship, from: from, completion: completion)
    }

    /**
     Disconnects the entity objects via the relationship using the shared instance of `UsergridCient`.

     - parameter entityType:       The entity type.
     - parameter entityID:         The entity UUID or name.
     - parameter relationship:     The relationship of the connection.
     - parameter fromType:         The optional type of the entity you are disconnecting from.
     - parameter toID:             The UUID of the entity you are disconnecting from.
     - parameter completion:       The completion block that will be called once the request has completed.
     */
    public static func disconnect(entityType:String, entityID:String, relationship:String, fromType:String?, fromID: String, completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.disconnect(entityType, entityID: entityID, relationship: relationship, fromType: fromType, fromID: fromID, completion: completion)
    }

    /**
     Disconnects the entity objects via the relationship using the shared instance of `UsergridCient`.

     - parameter entityType:       The entity type.
     - parameter entityID:         The entity UUID or name.
     - parameter relationship:     The relationship of the connection.
     - parameter fromType:         The type of the entity you are disconnecting from.
     - parameter fromName:         The name of the entity you are disconnecting from.
     - parameter completion:       The completion block that will be called once the request has completed.
     */
    public static func disconnect(entityType:String, entityID:String, relationship:String, fromType:String, fromName: String, completion: UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.disconnect(entityType, entityID: entityID, relationship: relationship, fromType: fromType, fromName: fromName, completion: completion)
    }

    /**
    Gets the connected entities for the given relationship using the shared instance of `UsergridCient`.

    - parameter direction:    The direction of the connection.
    - parameter entity:       The entity that contains the connection.
    - parameter relationship: The relationship.
    - parameter completion:   The completion block that will be called once the request has completed.
    */
    public static func getConnections(direction:UsergridDirection, entity:UsergridEntity, relationship:String, query:UsergridQuery? = nil, completion:UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.getConnections(direction, entity: entity, relationship: relationship, query:query, completion: completion)
    }

    /**
     Gets the connected entities for the given relationship using the shared instance of `UsergridCient`.

     - parameter direction:        The direction of the connection.
     - parameter type:             The entity type.
     - parameter uuidOrName:       The entity UUID or name.
     - parameter relationship:     The relationship of the connection.
     - parameter query:            The optional query.
     - parameter completion:       The completion block that will be called once the request has completed.
     */
    public static func getConnections(direction:UsergridDirection, type:String, uuidOrName:String, relationship:String, query:UsergridQuery? = nil, completion:UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.getConnections(direction, type: type, uuidOrName: uuidOrName, relationship: relationship, query:query, completion: completion)
    }

    /**
     Gets the connected entities for the given relationship using the shared instance of `UsergridCient`.

     - parameter direction:    The direction of the connection.
     - parameter uuid:         The entity UUID.
     - parameter relationship: The relationship of the connection.
     - parameter query:        The optional query.
     - parameter completion:   The optional completion block that will be called once the request has completed.
     */
    public static func getConnections(direction:UsergridDirection, uuid:String, relationship:String, query:UsergridQuery? = nil, completion:UsergridResponseCompletion? = nil) {
        Usergrid.sharedInstance.getConnections(direction, uuid: uuid, relationship: relationship, query: query, completion: completion)
    }

    // MARK: - Asset Management -

    /**
    Uploads the asset and connects the data to the given `UsergridEntity` using the shared instance of `UsergridCient`.

    - parameter entity:     The entity to connect the asset to.
    - parameter asset:      The asset to upload.
    - parameter progress:   The progress block that will be called to update the progress of the upload.
    - parameter completion: The completion block that will be called once the request has completed.
    */
    public static func uploadAsset(entity:UsergridEntity, asset:UsergridAsset, progress:UsergridAssetRequestProgress? = nil, completion:UsergridAssetUploadCompletion? = nil) {
        Usergrid.sharedInstance.uploadAsset(entity, asset: asset, progress: progress, completion: completion)
    }

    /**
    Downloads the asset from the given `UsergridEntity` using the shared instance of `UsergridCient`.

    - parameter entity:         The entity to which the asset to.
    - parameter contentType:    The content type of the asset's data.
    - parameter progress:       The progress block that will be called to update the progress of the download.
    - parameter completion:     The completion block that will be called once the request has completed.
    */
    public static func downloadAsset(entity:UsergridEntity, contentType:String, progress:UsergridAssetRequestProgress? = nil, completion:UsergridAssetDownloadCompletion? = nil) {
        Usergrid.sharedInstance.downloadAsset(entity, contentType: contentType, progress: progress, completion: completion)
    }
}
