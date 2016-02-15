//
//  FollowViewController.swift
//  ActivityFeed
//
//  Created by Robert Walsh on 1/21/16.
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
import UIKit
import UsergridSDK

class FollowViewController : UIViewController {

    @IBOutlet weak var usernameTextField: UITextField!

    @IBAction func addFollowerButtonTouched(sender:AnyObject?) {
        guard let username = usernameTextField.text where !username.isEmpty
        else {
            self.showAlert(title: "Follow failed.", message: "Please enter a valid username.")
            return
        }

        UsergridManager.followUser(username) { (response) -> Void in
            if response.ok {
                self.performSegueWithIdentifier("unwindToChatSegue", sender: self)
            } else {
                self.showAlert(title: "Follow failed.", message: "No user with the username \"\(username)\" found.")
            }
        }
    }
}