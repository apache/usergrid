//
//  RegisterViewController.swift
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

class RegisterViewController: UIViewController {

    @IBOutlet weak var nameTextField: UITextField!
    @IBOutlet weak var usernameTextField: UITextField!
    @IBOutlet weak var emailTextField: UITextField!
    @IBOutlet weak var passwordTextField: UITextField!

    @IBAction func registerButtonTouched(sender: AnyObject) {
        guard let name = nameTextField.text where !name.isEmpty,
              let username = usernameTextField.text where !username.isEmpty,
              let email = emailTextField.text where !email.isEmpty,
              let password = passwordTextField.text where !password.isEmpty
        else {
            self.showAlert(title: "Error Registering User", message: "Name, username, email, and password fields must not be empty.")
            return;
        }

        self.createUser(name, username: username, email: email, password: password)
    }

    private func createUser(name:String, username:String, email:String, password:String) {
        UsergridManager.createUser(name, username: username, email: email, password: password) { (response) -> Void in
            if let createdUser = response.user {
                self.showAlert(title: "Registering User Successful", message: "User description: \n \(createdUser.stringValue)") { (action) -> Void in
                    self.performSegueWithIdentifier("unwindSegue", sender: self)
                }
            } else {
                self.showAlert(title: "Error Registering User", message: response.error?.errorDescription)
            }
        }
    }
}
