//
//  CONNECTION_Tests.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 10/5/15.
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

class CONNECTION_Tests: XCTestCase {

    let clientAuth = UsergridAppAuth(clientId: "b3U6THNcevskEeOQZLcUROUUVA", clientSecret: "b3U6RZHYznP28xieBzQPackFPmmnevU")
    private static let collectionName = "publicevent"

    override func setUp() {
        super.setUp()
        Usergrid.initSharedInstance(orgId:ClientCreationTests.orgId, appId: "sdk.demo")
    }

    override func tearDown() {
        Usergrid._sharedClient = nil
        super.tearDown()
    }

    func test_CLIENT_AUTH() {

        let authExpect = self.expectationWithDescription("\(#function)")
        Usergrid.authMode = .App
        Usergrid.authenticateApp(clientAuth) { auth,error in
            XCTAssertTrue(NSThread.isMainThread())
            XCTAssertNil(error)
            XCTAssertNotNil(Usergrid.appAuth)

            if let appAuth = Usergrid.appAuth {

                XCTAssertNotNil(appAuth.accessToken)
                XCTAssertNotNil(appAuth.expiry)

                Usergrid.GET(CONNECTION_Tests.collectionName) { (response) in
                    XCTAssertTrue(NSThread.isMainThread())
                    XCTAssertNotNil(response)
                    XCTAssertTrue(response.ok)
                    XCTAssertTrue(response.hasNextPage)
                    XCTAssertEqual(response.entities!.count, 10)

                    let entity = response.first!
                    let entityToConnect = response.entities![1]
                    XCTAssertEqual(entity.type, CONNECTION_Tests.collectionName)

                    entity.connect("likes", toEntity: entityToConnect) { (response) -> Void in
                        XCTAssertTrue(NSThread.isMainThread())
                        XCTAssertNotNil(response)
                        XCTAssertTrue(response.ok)

                        entity.getConnections(.Out, relationship: "likes", query:nil) { (response) -> Void in
                            XCTAssertTrue(NSThread.isMainThread())
                            XCTAssertNotNil(response)
                            XCTAssertTrue(response.ok)

                            let connectedEntity = response.first!
                            XCTAssertNotNil(connectedEntity)
                            XCTAssertEqual(connectedEntity.uuidOrName, entityToConnect.uuidOrName)

                            entity.disconnect("likes", fromEntity: connectedEntity) { (response) -> Void in
                                XCTAssertTrue(NSThread.isMainThread())
                                XCTAssertNotNil(response)
                                XCTAssertTrue(response.ok)

                                entity.getConnections(.Out, relationship: "likes", query:nil) { (response) -> Void in
                                    XCTAssertTrue(NSThread.isMainThread())
                                    XCTAssertNotNil(response)
                                    XCTAssertTrue(response.ok)
                                    authExpect.fulfill()
                                }
                            }
                        }
                    }
                }
            } else {
                authExpect.fulfill()
            }
        }
        self.waitForExpectationsWithTimeout(20, handler: nil)
    }
}
