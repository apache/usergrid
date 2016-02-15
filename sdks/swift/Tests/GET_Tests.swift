//
//  GET_Tests.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 9/2/15.
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

class GET_Tests: XCTestCase {

    let usergridClientInstance = UsergridClient(orgId:ClientCreationTests.orgId, appId: ClientCreationTests.appId)

    static let collectionName = "books"
    static let entityUUID = "f4078aca-2fb1-11e5-8eb2-e13f8369aad1"

    let query = UsergridQuery(GET_Tests.collectionName).fromString("select * where title = 'The Sun Also Rises' or title = 'The Old Man and the Sea'")


    func test_GET_WITHOUT_QUERY() {

        let getExpect = self.expectationWithDescription("\(__FUNCTION__)")
        usergridClientInstance.GET(GET_Tests.collectionName) { (response) in
            XCTAssertNotNil(response)
            XCTAssertTrue(response.ok)
            XCTAssertTrue(response.hasNextPage)
            XCTAssertEqual(response.count, 10)
            getExpect.fulfill()
        }
        self.waitForExpectationsWithTimeout(10, handler: nil)
    }

    func test_GET_WITH_QUERY() {

        let getExpect = self.expectationWithDescription("\(__FUNCTION__)")
        usergridClientInstance.GET(self.query) { (response) in
            XCTAssertNotNil(response)
            XCTAssertTrue(response.ok)
            XCTAssertEqual(response.count, 3)
            getExpect.fulfill()
        }
        self.waitForExpectationsWithTimeout(10, handler: nil)
    }

    func test_GET_WITH_UUID() {

        let getExpect = self.expectationWithDescription("\(__FUNCTION__)")
        usergridClientInstance.GET(GET_Tests.collectionName, uuidOrName:GET_Tests.entityUUID) { (response) in
            XCTAssertNotNil(response)
            XCTAssertTrue(response.ok)
            let entity = response.first!
            XCTAssertFalse(response.hasNextPage)
            XCTAssertEqual(response.count, 1)
            XCTAssertNotNil(entity)
            XCTAssertEqual(entity.uuid!, GET_Tests.entityUUID)
            getExpect.fulfill()
        }
        self.waitForExpectationsWithTimeout(10, handler: nil)
    }

    func test_GET_NEXT_PAGE_WITH_NO_QUERY() {

        let getExpect = self.expectationWithDescription("\(__FUNCTION__)")
        usergridClientInstance.GET(GET_Tests.collectionName) { (response) in
            XCTAssertNotNil(response)
            XCTAssertTrue(response.ok)
            XCTAssertTrue(response.hasNextPage)
            XCTAssertEqual(response.count, 10)

            response.loadNextPage() { (nextPageResponse) in
                XCTAssertTrue(nextPageResponse.ok)
                XCTAssertNotNil(nextPageResponse)
                XCTAssertFalse(nextPageResponse.hasNextPage)
                XCTAssertEqual(nextPageResponse.entities!.count, 6)
                getExpect.fulfill()
            }
        }
        self.waitForExpectationsWithTimeout(20, handler: nil)
    }
    
}
