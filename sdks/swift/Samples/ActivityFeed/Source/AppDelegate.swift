//
//  AppDelegate.swift
//  ActivityFeed
//
//  Created by Robert Walsh on 11/19/15.
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


import UIKit
import UsergridSDK

// TODO: Change the values to correspond to your organization, application, and notifier identifiers.

@UIApplicationMain class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {

        UINavigationBar.appearance().tintColor = UIColor.whiteColor()
        application.registerUserNotificationSettings(UIUserNotificationSettings( forTypes: [.Alert, .Badge, .Sound], categories: nil))
        application.registerForRemoteNotifications()

        // Initialize the Usergrid shared instance.
        UsergridManager.initializeSharedInstance()

        // If there is a current user already logged in from the keychain we will skip the login page and go right to the chat screen

        if Usergrid.currentUser != nil {
            let rootViewController = self.window!.rootViewController as! UINavigationController
            let loginViewController = rootViewController.viewControllers.first!
            loginViewController.performSegueWithIdentifier("loginSuccessNonAnimatedSegue", sender: loginViewController)
        }

        return true
    }

    func application(application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: NSData) {
        Usergrid.applyPushToken(deviceToken, notifierID: UsergridManager.NOTIFIER_ID, completion: nil)
    }

    func application(application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: NSError) {
        print("Application failed to register for remote notifications")
    }
}

