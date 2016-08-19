//
//  ClientCreationTests.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 7/31/15.
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

class ClientCreationTests: XCTestCase {

    static let orgId = "rwalsh"
    static let appId = "sandbox"

    override func setUp() {
        super.setUp()
        Usergrid.initSharedInstance(orgId:ClientCreationTests.orgId, appId: ClientCreationTests.appId)
        Usergrid.persistCurrentUserInKeychain = false
    }

    override func tearDown() {
        Usergrid._sharedClient = nil
        super.tearDown()
    }

    func test_INSTANCE_POINTERS() {
        XCTAssertNotNil(Usergrid.sharedInstance)
    }

    func test_CLIENT_PROPERTIES() {
        XCTAssertEqual(Usergrid.appId, ClientCreationTests.appId)
        XCTAssertEqual(Usergrid.orgId, ClientCreationTests.orgId)
        XCTAssertEqual(Usergrid.authMode, UsergridAuthMode.User)
        XCTAssertEqual(Usergrid.persistCurrentUserInKeychain, false)
        XCTAssertEqual(Usergrid.baseUrl, UsergridClient.DEFAULT_BASE_URL)
        XCTAssertEqual(Usergrid.clientAppURL, "\(UsergridClient.DEFAULT_BASE_URL)/\(ClientCreationTests.orgId)/\(ClientCreationTests.appId)" )
        XCTAssertNil(Usergrid.currentUser)
        XCTAssertNil(Usergrid.userAuth)
    }

    func test_CLIENT_NSCODING() {
        let sharedInstanceAsData = NSKeyedArchiver.archivedDataWithRootObject(Usergrid.sharedInstance)
        let newInstanceFromData = NSKeyedUnarchiver.unarchiveObjectWithData(sharedInstanceAsData) as? UsergridClient

        XCTAssertNotNil(newInstanceFromData)

        if let newInstance = newInstanceFromData {
            XCTAssertEqual(Usergrid.appId, newInstance.appId)
            XCTAssertEqual(Usergrid.orgId, newInstance.orgId)
            XCTAssertEqual(Usergrid.authMode, newInstance.authMode)
            XCTAssertEqual(Usergrid.baseUrl, newInstance.baseUrl)
        }
    }
}
