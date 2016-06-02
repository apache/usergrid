//
//  User_Tests.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 9/14/15.
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

import XCTest
import CoreLocation
@testable import UsergridSDK

class User_Tests: XCTestCase {

    var user: UsergridUser!

    static let name = "Robert Walsh"
    static let age = 29
    static let email = "handsomeRob741@yahoo.com"
    static let username = "rwalsh"
    static let password = "password"
    static let resetPassword = "password111"
    static let picture = "http://www.gravatar.com/avatar/e466d447df831ddce35fbc50763fb03a"
    static let activated = true
    static let disabled = false

    override func setUp() {
        super.setUp()

        Usergrid.initSharedInstance(orgId:ClientCreationTests.orgId, appId: ClientCreationTests.appId)
        Usergrid.persistCurrentUserInKeychain = false

        user = UsergridUser(name:"a_bogus_name", email:User_Tests.email, username:User_Tests.username, password:User_Tests.password)
        user.name = User_Tests.name
        user.age = User_Tests.age
        user.location = CLLocation(latitude: -90, longitude: 100)
        user.picture = User_Tests.picture
        user.activated = User_Tests.activated
        user.disabled = User_Tests.disabled
    }

    override func tearDown() {
        Usergrid._sharedClient = nil        
        super.tearDown()
    }

    func test_USER_INIT() {
        user = UsergridUser(email: User_Tests.email, password: User_Tests.password)
        XCTAssertNotNil(user)
        XCTAssertNotNil(user.email)
        XCTAssertNotNil(user.password)
        XCTAssertEqual(user.email!, User_Tests.email)
        XCTAssertEqual(user.usernameOrEmail!, User_Tests.email)
        XCTAssertEqual(user.password!, User_Tests.password)
        XCTAssertFalse(user.activated)
        XCTAssertFalse(user.disabled)

        user = UsergridUser(name: User_Tests.name, propertyDict: ["password":User_Tests.password])
        XCTAssertNotNil(user)
        XCTAssertNotNil(user.name)
        XCTAssertNotNil(user.password)
        XCTAssertEqual(user.name!, User_Tests.name)
        XCTAssertEqual(user.password!, User_Tests.password)
        XCTAssertFalse(user.activated)
        XCTAssertFalse(user.disabled)

        user = UsergridUser(name:User_Tests.name, email: User_Tests.email, password: User_Tests.password)
        XCTAssertNotNil(user)
        XCTAssertNotNil(user.name)
        XCTAssertNotNil(user.email)
        XCTAssertNotNil(user.password)
        XCTAssertEqual(user.name!, User_Tests.name)
        XCTAssertEqual(user.email!, User_Tests.email)
        XCTAssertEqual(user.usernameOrEmail!, User_Tests.email)
        XCTAssertEqual(user.password!, User_Tests.password)
        XCTAssertFalse(user.activated)
        XCTAssertFalse(user.disabled)

        user = UsergridUser(username: User_Tests.username, password: User_Tests.password)
        XCTAssertNotNil(user)
        XCTAssertNotNil(user.username)
        XCTAssertNotNil(user.password)
        XCTAssertEqual(user.username!, User_Tests.username)
        XCTAssertEqual(user.usernameOrEmail!, User_Tests.username)
        XCTAssertEqual(user.password!, User_Tests.password)
        XCTAssertFalse(user.activated)
        XCTAssertFalse(user.disabled)

        user = UsergridUser(name: User_Tests.name, username: User_Tests.username, password: User_Tests.password)
        XCTAssertNotNil(user)
        XCTAssertNotNil(user.name)
        XCTAssertNotNil(user.username)
        XCTAssertNotNil(user.password)
        XCTAssertEqual(user.name!, User_Tests.name)
        XCTAssertEqual(user.username!, User_Tests.username)
        XCTAssertEqual(user.usernameOrEmail!, User_Tests.username)
        XCTAssertEqual(user.password!, User_Tests.password)
        XCTAssertFalse(user.activated)
        XCTAssertFalse(user.disabled)
    }

    func test_USERS_AND_PROPERTIES_NOT_NIL() {
        XCTAssertNotNil(user)
        XCTAssertNotNil(user.name)
        XCTAssertNotNil(user.age)
        XCTAssertNotNil(user.username)
        XCTAssertNotNil(user.email)
        XCTAssertNotNil(user.password)
        XCTAssertNotNil(user.picture)
        XCTAssertNotNil(user.activated)
        XCTAssertNotNil(user.disabled)
    }

    func test_USER_PROPERTIES_WITH_HELPERS() {
        user["uuid"] = User_Tests.age
        XCTAssertNil(user.uuid)

        XCTAssertEqual(user.name!, User_Tests.name)
        XCTAssertEqual(user.age!, User_Tests.age)
        XCTAssertEqual(user.username!, User_Tests.username)
        XCTAssertEqual(user.email!, User_Tests.email)
        XCTAssertEqual(user.password!, User_Tests.password)
        XCTAssertEqual(user.picture!, User_Tests.picture)
        XCTAssertTrue(user.activated)
        XCTAssertFalse(user.disabled)
    }

    func test_USER_PROPERTIES_WITHOUT_HELPERS() {
        XCTAssertEqual(user[UsergridUserProperties.Name.stringValue]! as? String, User_Tests.name)
        XCTAssertEqual(user[UsergridUserProperties.Age.stringValue]! as? Int, User_Tests.age)
        XCTAssertEqual(user[UsergridUserProperties.Username.stringValue]! as? String, User_Tests.username)
        XCTAssertEqual(user[UsergridUserProperties.Email.stringValue]! as? String, User_Tests.email)
        XCTAssertEqual(user[UsergridUserProperties.Password.stringValue]! as? String, User_Tests.password)
        XCTAssertEqual(user[UsergridUserProperties.Picture.stringValue]! as? String, User_Tests.picture)
        XCTAssertTrue(user[UsergridUserProperties.Activated.stringValue]! as! Bool)
        XCTAssertFalse(user[UsergridUserProperties.Disabled.stringValue]! as! Bool)
    }

    func deleteUser(expectation:XCTestExpectation) {
        self.user.remove() { removeResponse in
            XCTAssertTrue(NSThread.isMainThread())
            XCTAssertNotNil(removeResponse)
            XCTAssertTrue(removeResponse.ok)
            XCTAssertNotNil(removeResponse.user)
            XCTAssertNotNil(removeResponse.users)
            print(removeResponse.error)
            expectation.fulfill()
        }
    }

    func test_CREATE_AND_DELETE_USER() {
        let userExpect = self.expectationWithDescription("\(#function)")

        user.save() { (createResponse) in
            XCTAssertTrue(NSThread.isMainThread())
            XCTAssertNotNil(createResponse)
            XCTAssertTrue(createResponse.ok)
            XCTAssertNotNil(createResponse.user)
            XCTAssertNotNil(createResponse.users)

            if let createdUser = createResponse.user {
                XCTAssertTrue(createdUser.isUser)
                XCTAssertNotNil(createdUser.uuid)
                XCTAssertNotNil(createdUser.created)
                XCTAssertNotNil(createdUser.modified)
                XCTAssertNotNil(createdUser.location)
                XCTAssertEqual(createdUser.name!, User_Tests.name)
                XCTAssertEqual(createdUser.age!, User_Tests.age)
                XCTAssertEqual(createdUser.username!, User_Tests.username)
                XCTAssertEqual(createdUser.email!, User_Tests.email)
                XCTAssertEqual(createdUser.picture!, User_Tests.picture)
                XCTAssertTrue(createdUser.activated)
                XCTAssertFalse(createdUser.disabled)
                XCTAssertFalse(createdUser.hasAsset)

                self.deleteUser(userExpect)
            }
        }
        self.waitForExpectationsWithTimeout(100, handler: nil)
    }

    func test_AUTHENTICATE_USER() {
        let userExpect = self.expectationWithDescription("\(#function)")

        UsergridUser.checkAvailable(user.email, username: user.username) { error,available in

            XCTAssertTrue(NSThread.isMainThread())
            XCTAssertNil(error)
            XCTAssertTrue(available)

            self.user.create() { (createResponse) in
                XCTAssertTrue(NSThread.isMainThread())
                XCTAssertNotNil(createResponse)
                XCTAssertTrue(createResponse.ok)
                XCTAssertNotNil(createResponse.user)
                XCTAssertNotNil(createResponse.users)
                XCTAssertNotNil(self.user.uuid)

                self.user.login(self.user.username!, password:User_Tests.password) { (auth, loggedInUser, error) -> Void in
                    XCTAssertTrue(NSThread.isMainThread())
                    XCTAssertNil(error)
                    XCTAssertNotNil(auth)
                    XCTAssertNotNil(loggedInUser)
                    XCTAssertEqual(auth, self.user.auth!)

                    Usergrid.authenticateUser(self.user.auth!) { auth,currentUser,error in
                        XCTAssertTrue(NSThread.isMainThread())
                        XCTAssertNil(error)
                        XCTAssertNotNil(auth)
                        XCTAssertEqual(auth, self.user.auth!)

                        XCTAssertNotNil(currentUser)
                        XCTAssertNotNil(Usergrid.currentUser)
                        XCTAssertEqual(currentUser, Usergrid.currentUser!)

                        self.user.reauthenticate() { auth, reauthedUser, error in
                            XCTAssertTrue(NSThread.isMainThread())
                            XCTAssertNil(error)
                            XCTAssertNotNil(auth)
                            XCTAssertEqual(auth, self.user.auth!)

                            XCTAssertNotNil(reauthedUser)
                            XCTAssertNotNil(Usergrid.currentUser)

                            self.user.logout() { response in
                                XCTAssertTrue(NSThread.isMainThread())
                                XCTAssertNotNil(response)
                                XCTAssertTrue(response.ok)
                                XCTAssertNil(response.error)

                                self.deleteUser(userExpect)
                            }
                        }
                    }
                }
            }
        }
        self.waitForExpectationsWithTimeout(100, handler: nil)
    }

    func test_RESET_USER_PASSWORD() {
        let userExpect = self.expectationWithDescription("\(#function)")

        user.create() { (createResponse) in
            XCTAssertTrue(NSThread.isMainThread())
            XCTAssertNotNil(createResponse)
            XCTAssertTrue(createResponse.ok)
            XCTAssertNotNil(createResponse.user)
            XCTAssertNotNil(createResponse.users)
            XCTAssertNotNil(self.user.uuid)

            self.user.login(self.user.username!, password:User_Tests.password) { (auth, loggedInUser, error) -> Void in
                XCTAssertTrue(NSThread.isMainThread())
                XCTAssertNil(error)
                XCTAssertNotNil(auth)
                XCTAssertNotNil(loggedInUser)
                XCTAssertEqual(auth, self.user.auth!)

                self.user.resetPassword(User_Tests.password, new: User_Tests.resetPassword) { error,didSucceed in
                    XCTAssertTrue(NSThread.isMainThread())
                    XCTAssertTrue(didSucceed)
                    XCTAssertNil(error)

                    self.user.login(self.user.username!, password:User_Tests.resetPassword) { (auth, loggedInUser, error) -> Void in
                        XCTAssertTrue(NSThread.isMainThread())
                        XCTAssertNil(error)
                        XCTAssertNotNil(auth)
                        XCTAssertNotNil(loggedInUser)
                        XCTAssertEqual(auth, self.user.auth!)

                        self.deleteUser(userExpect)
                    }
                }
            }
        }
        self.waitForExpectationsWithTimeout(100, handler: nil)
    }

    func test_DEVICE_CONNECTION() {
        let userExpect = self.expectationWithDescription("\(#function)")

        user.create() { createResponse in
            XCTAssertNotNil(createResponse)
            XCTAssertTrue(createResponse.ok)
            XCTAssertNotNil(createResponse.user)
            XCTAssertNotNil(createResponse.users)
            XCTAssertNotNil(self.user.uuid)

            self.user.connectToDevice() { connectResponse in
                XCTAssertNotNil(connectResponse)
                XCTAssertTrue(connectResponse.ok)
                XCTAssertNil(connectResponse.error)

                self.user.getConnectedDevice() { getConnectedDeviceResponse in
                    XCTAssertNotNil(getConnectedDeviceResponse)
                    XCTAssertTrue(getConnectedDeviceResponse.ok)
                    XCTAssertNil(getConnectedDeviceResponse.error)
                    XCTAssertNotNil(getConnectedDeviceResponse.entity)

                    if let responseEntity = getConnectedDeviceResponse.entity {
                        XCTAssertTrue(responseEntity is UsergridDevice)
                    }

                    self.user.disconnectFromDevice() { disconnectResponse in
                        XCTAssertNotNil(disconnectResponse)
                        XCTAssertTrue(disconnectResponse.ok)
                        XCTAssertNil(disconnectResponse.error)

                        self.deleteUser(userExpect)
                    }
                }
            }
        }
        self.waitForExpectationsWithTimeout(100, handler: nil)
    }

    func test_DEVICE_CONNECT_FAIL() {
        let userExpect = self.expectationWithDescription("\(#function)")

        user.create() { createResponse in
            XCTAssertNotNil(createResponse)
            XCTAssertTrue(createResponse.ok)
            XCTAssertNotNil(createResponse.user)
            XCTAssertNotNil(createResponse.users)
            XCTAssertNotNil(self.user.uuid)

            self.user.connectToDevice() { connectResponse in
                XCTAssertNotNil(connectResponse)
                XCTAssertTrue(connectResponse.ok)
                XCTAssertNil(connectResponse.error)

                self.user.getConnectedDevice() { getConnectedDeviceResponse in
                    XCTAssertNotNil(getConnectedDeviceResponse)
                    XCTAssertTrue(getConnectedDeviceResponse.ok)
                    XCTAssertNil(getConnectedDeviceResponse.error)
                    XCTAssertNotNil(getConnectedDeviceResponse.entity)

                    if let responseEntity = getConnectedDeviceResponse.entity {
                        XCTAssertTrue(responseEntity is UsergridDevice)
                    }

                    self.user.disconnectFromDevice() { disconnectResponse in
                        XCTAssertNotNil(disconnectResponse)
                        XCTAssertTrue(disconnectResponse.ok)
                        XCTAssertNil(disconnectResponse.error)

                        self.deleteUser(userExpect)
                    }
                }
            }
        }
        self.waitForExpectationsWithTimeout(100, handler: nil)
    }


    func test_USER_NSCODING() {
        let userData = NSKeyedArchiver.archivedDataWithRootObject(user)
        let newInstanceFromData = NSKeyedUnarchiver.unarchiveObjectWithData(userData) as? UsergridUser

        XCTAssertNotNil(newInstanceFromData)

        if let newInstance = newInstanceFromData {
            XCTAssertEqual(user.uuid,newInstance.uuid)
            XCTAssertEqual(user.uuidOrName,newInstance.uuidOrName)
            XCTAssertEqual(user.uuidOrUsername,newInstance.uuidOrUsername)
            XCTAssertEqual(user.auth,newInstance.auth)
            XCTAssertEqual(user.created,newInstance.created)
            XCTAssertEqual(user.modified,newInstance.modified)
            XCTAssertEqual(user.location!.coordinate.longitude,newInstance.location!.coordinate.longitude)
            XCTAssertEqual(user.location!.coordinate.latitude,newInstance.location!.coordinate.latitude)
            XCTAssertEqual(user.name,newInstance.name)
            XCTAssertEqual(user.age,newInstance.age)
            XCTAssertEqual(user.username,newInstance.username)
            XCTAssertEqual(user.email,newInstance.email)
            XCTAssertEqual(user.picture,newInstance.picture)
            XCTAssertEqual(user.activated,newInstance.activated)
            XCTAssertEqual(user.disabled,newInstance.disabled)
            XCTAssertEqual(user.hasAsset,newInstance.hasAsset)
        }
    }
}