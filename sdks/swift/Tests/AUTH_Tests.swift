//
//  AUTH_Tests.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 9/17/15.
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

class AUTH_Tests: XCTestCase {

    var appAuth: UsergridAppAuth!
    var userAuth: UsergridUserAuth!

    private static let collectionName = "publicevent"
    private static let entityUUID = "fa015eaa-fe1c-11e3-b94b-63b29addea01"

    override func setUp() {
        super.setUp()
        appAuth = UsergridAppAuth(clientId: "b3U6THNcevskEeOQZLcUROUUVA", clientSecret: "b3U6RZHYznP28xieBzQPackFPmmnevU")
        userAuth = UsergridUserAuth(username: "username", password: "password")
        Usergrid.initSharedInstance(orgId:ClientCreationTests.orgId, appId: "sdk.demo")
    }

    override func tearDown() {
        Usergrid._sharedClient = nil
        super.tearDown()
    }

    func test_CLIENT_AUTH() {

        let authExpect = self.expectationWithDescription("\(#function)")
        Usergrid.authMode = .App
        Usergrid.authenticateApp(appAuth) { auth,error in
            XCTAssertTrue(NSThread.isMainThread())
            XCTAssertNil(error)
            XCTAssertNotNil(Usergrid.appAuth)

            if let appAuth = Usergrid.appAuth {

                XCTAssertNotNil(appAuth.accessToken)
                XCTAssertNotNil(appAuth.expiry)
                XCTAssertNotNil(appAuth.isValid)

                Usergrid.GET(AUTH_Tests.collectionName) { (response) in
                    XCTAssertTrue(NSThread.isMainThread())
                    XCTAssertNotNil(response)
                    XCTAssertTrue(response.hasNextPage)
                    XCTAssertEqual(response.entities!.count, 10)
                    XCTAssertEqual(response.first!.type, AUTH_Tests.collectionName)
                    
                    authExpect.fulfill()
                }
            }
        }
        self.waitForExpectationsWithTimeout(100, handler: nil)
    }

    func test_DESTROY_AUTH() {
        let auth = UsergridAuth(accessToken: "YWMt91Q2YtWaEeW_Ki2uDueMEwAAAVMUTVSPeOdX-oradxdqirEFz5cPU3GWybs")

        XCTAssertTrue(auth.isValid)
        XCTAssertNotNil(auth.accessToken)
        XCTAssertNil(auth.expiry)

        auth.destroy()

        XCTAssertFalse(auth.isValid)
        XCTAssertNil(auth.accessToken)
        XCTAssertNil(auth.expiry)
    }

    func test_APP_AUTH_NSCODING() {

        appAuth.accessToken = "YWMt91Q2YtWaEeW_Ki2uDueMEwAAAVMUTVSPeOdX-oradxdqirEFz5cPU3GWybs"
        appAuth.expiry = NSDate.distantFuture()

        let authCodingData = NSKeyedArchiver.archivedDataWithRootObject(appAuth)
        let newInstanceFromData = NSKeyedUnarchiver.unarchiveObjectWithData(authCodingData) as? UsergridAppAuth

        XCTAssertNotNil(newInstanceFromData)

        if let newInstance = newInstanceFromData {
            XCTAssertTrue(appAuth.isValid)
            XCTAssertTrue(newInstance.isValid)
            XCTAssertEqual(appAuth.clientId,newInstance.clientId)
            XCTAssertEqual(appAuth.accessToken,newInstance.accessToken)
            XCTAssertEqual(appAuth.expiry,newInstance.expiry)
        }
    }

    func test_USER_AUTH_NSCODING() {

        userAuth.accessToken = "YWMt91Q2YtWaEeW_Ki2uDueMEwAAAVMUTVSPeOdX-oradxdqirEFz5cPU3GWybs"
        userAuth.expiry = NSDate.distantFuture()

        let authCodingData = NSKeyedArchiver.archivedDataWithRootObject(userAuth)
        let newInstanceFromData = NSKeyedUnarchiver.unarchiveObjectWithData(authCodingData) as? UsergridUserAuth

        XCTAssertNotNil(newInstanceFromData)

        if let newInstance = newInstanceFromData {
            XCTAssertTrue(userAuth.isValid)
            XCTAssertTrue(newInstance.isValid)
            XCTAssertEqual(userAuth.username,newInstance.username)
            XCTAssertEqual(userAuth.accessToken,newInstance.accessToken)
            XCTAssertEqual(userAuth.expiry,newInstance.expiry)
        }
    }


}
