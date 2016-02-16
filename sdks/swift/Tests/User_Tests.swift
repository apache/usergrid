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
@testable import UsergridSDK

class User_Tests: XCTestCase {

    var client = UsergridClient(orgId:ClientCreationTests.orgId, appId: ClientCreationTests.appId)

    let userWithNoName = UsergridUser()
    let user = UsergridUser(name:User_Tests.name, email:User_Tests.email, username:User_Tests.username, password:User_Tests.password)

    static let name = "Robert Walsh"
    static let age = 29
    static let email = "handsomeRob741@yahoo.com"
    static let username = "rwalsh"
    static let password = "password"
    static let picture = "http://www.gravatar.com/avatar/e466d447df831ddce35fbc50763fb03a"
    static let activated = true
    static let disabled = false

    override func setUp() {
        super.setUp()
        user.age = User_Tests.age
        user.picture = User_Tests.picture
        user.activated = User_Tests.activated
        user.disabled = User_Tests.disabled
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

        XCTAssertNotNil(userWithNoName)
        XCTAssertNil(userWithNoName.name)
    }

    func test_USER_PROPERTIES_WITH_HELPERS() {
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

    func test_CREATE_AND_DELETE_USER() {
        let createUserExpect = self.expectationWithDescription("\(__FUNCTION__)")

        user.create(client) { (createResponse) in
            XCTAssertNotNil(createResponse)
            XCTAssertTrue(createResponse.ok)
            XCTAssertNotNil(createResponse.user)
            XCTAssertNotNil(createResponse.users)

            if let createdUser = createResponse.user {
                XCTAssertNotNil(createdUser.uuid)
                XCTAssertEqual(createdUser.name!, User_Tests.name)
                XCTAssertEqual(createdUser.age!, User_Tests.age)
                XCTAssertEqual(createdUser.username!, User_Tests.username)
                XCTAssertEqual(createdUser.email!, User_Tests.email)
                XCTAssertEqual(createdUser.picture!, User_Tests.picture)
                XCTAssertTrue(createdUser.activated)
                XCTAssertFalse(createdUser.disabled)

                createdUser.remove(self.client) { (removeResponse) in
                    XCTAssertNotNil(removeResponse)
                    XCTAssertTrue(removeResponse.ok)
                    XCTAssertNotNil(removeResponse.user)
                    XCTAssertNotNil(removeResponse.users)
                    createUserExpect.fulfill()
                }
            }
        }
        self.waitForExpectationsWithTimeout(10, handler: nil)
    }
}