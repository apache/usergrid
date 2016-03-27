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

    let testAuthClient = UsergridClient(orgId: ClientCreationTests.orgId, appId: "sdk.demo")
    let clientAuth = UsergridAppAuth(clientId: "b3U6THNcevskEeOQZLcUROUUVA", clientSecret: "b3U6RZHYznP28xieBzQPackFPmmnevU")
    private static let collectionName = "publicevent"
    private static let entityUUID = "fa015eaa-fe1c-11e3-b94b-63b29addea01"

    func test_CLIENT_AUTH() {

        let authExpect = self.expectationWithDescription("\(__FUNCTION__)")
        testAuthClient.authFallback = .App
        testAuthClient.authenticateApp(clientAuth) { [weak self] (auth,error) in

            XCTAssertNil(error)
            XCTAssertNotNil(self?.testAuthClient.appAuth)

            if let appAuth = self?.testAuthClient.appAuth {

                XCTAssertNotNil(appAuth.accessToken)
                XCTAssertNotNil(appAuth.expiry)

                self?.testAuthClient.GET(AUTH_Tests.collectionName) { (response) in

                    XCTAssertNotNil(response)
                    XCTAssertTrue(response.hasNextPage)
                    XCTAssertEqual(response.entities!.count, 10)
                    XCTAssertEqual(response.first!.type, AUTH_Tests.collectionName)
                    
                    authExpect.fulfill()
                }
            } else {
                authExpect.fulfill()
            }
        }
        self.waitForExpectationsWithTimeout(20, handler: nil)
    }
}
