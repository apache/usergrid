//
//  UsergridManager.swift
//  ActivityFeed
//
//  Created by Robert Walsh on 1/19/16.
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
import UsergridSDK

/// This class handles the primary communications to the UsergirdSDK.
public class UsergridManager {

    static let ORG_ID = "rwalsh"
    static let APP_ID = "sandbox"
    static let NOTIFIER_ID = "usergridsample"
    static let BASE_URL = "https://api.usergrid.com"

    static func initializeSharedInstance() {
        Usergrid.initSharedInstance(configuration: UsergridClientConfig(orgId: UsergridManager.ORG_ID, appId: UsergridManager.APP_ID, baseUrl: UsergridManager.BASE_URL))
        ActivityEntity.registerSubclass()
    }

    static func loginUser(username:String, password:String, completion:UsergridUserAuthCompletionBlock) {
        let userAuth = UsergridUserAuth(username: username, password: password)
        Usergrid.authenticateUser(userAuth, completion: completion)
    }

    static func createUser(name:String, username:String, email:String, password:String, completion:UsergridResponseCompletion) {
        let user = UsergridUser(name: name, propertyDict: [UsergridUserProperties.Username.stringValue:username,
                                                            UsergridUserProperties.Email.stringValue:email,
                                                            UsergridUserProperties.Password.stringValue:password])
        user.create(completion)
    }

    static func getFeedMessages(completion:UsergridResponseCompletion) {
        Usergrid.GET(UsergridQuery("users/me/feed").desc(UsergridEntityProperties.Created.stringValue), queryCompletion: completion)
    }

    static func postFeedMessage(text:String,completion:UsergridResponseCompletion) {
        let currentUser = Usergrid.currentUser!

        let verb = "post"
        let content = text

        var actorDictionary = [String:AnyObject]()
        actorDictionary["displayName"] = currentUser.name ?? currentUser.usernameOrEmail ?? ""
        actorDictionary["email"] = currentUser.email ?? ""
        if let imageURL = currentUser.picture {
            actorDictionary["image"] = ["url":imageURL,"height":80,"width":80]
        }

        Usergrid.POST("users/me/activities", jsonBody: ["actor":actorDictionary,"verb":verb,"content":content], completion: completion)
    }

    static func followUser(username:String, completion:UsergridResponseCompletion) {
        Usergrid.connect("users", entityID: "me", relationship: "following", toType: "users", toName: username, completion: completion)
    }
}