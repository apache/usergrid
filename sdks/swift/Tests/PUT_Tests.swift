//
//  PUT_Tests.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 9/11/15.
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

class PUT_Tests: XCTestCase {

    let query = UsergridQuery(PUT_Tests.collectionName)
        .eq("title", value: "The Sun Also Rises")
        .or()
        .eq("title", value: "The Old Man and the Sea")

    static let collectionName = "books"
    static let entityUUID = "f4078aca-2fb1-11e5-8eb2-e13f8369aad1"

    override func setUp() {
        super.setUp()
        Usergrid.initSharedInstance(orgId:ClientCreationTests.orgId, appId: ClientCreationTests.appId)
    }

    override func tearDown() {
        Usergrid._sharedClient = nil
        super.tearDown()
    }

    func test_PUT_BY_SPECIFYING_UUID_AS_PARAMETER() {

        let propertyNameToUpdate = "\(#function)"
        let propertiesNewValue = "\(propertyNameToUpdate)_VALUE"
        let putExpect = self.expectationWithDescription(propertyNameToUpdate)

        Usergrid.PUT(PUT_Tests.collectionName, uuidOrName: PUT_Tests.entityUUID, jsonBody:[propertyNameToUpdate : propertiesNewValue]) { (response) in
            XCTAssertTrue(NSThread.isMainThread())
            XCTAssertNotNil(response)
            XCTAssertTrue(response.ok)
            XCTAssertEqual(response.entities!.count, 1)
            let entity = response.first!

            XCTAssertNotNil(entity)
            XCTAssertEqual(entity.uuid!, PUT_Tests.entityUUID)

            let updatedPropertyValue = entity[propertyNameToUpdate] as? String
            XCTAssertNotNil(updatedPropertyValue)
            XCTAssertEqual(updatedPropertyValue!,propertiesNewValue)
            putExpect.fulfill()
        }
        self.waitForExpectationsWithTimeout(10, handler: nil)
    }

    func test_PUT_BY_SPECIFYING_UUID_WITHIN_JSON_BODY() {

        let propertyNameToUpdate = "\(#function)"
        let propertiesNewValue = "\(propertyNameToUpdate)_VALUE"
        let putExpect = self.expectationWithDescription(propertyNameToUpdate)

        let jsonDictToPut = [UsergridEntityProperties.UUID.stringValue : PUT_Tests.entityUUID, propertyNameToUpdate : propertiesNewValue]

        Usergrid.PUT(PUT_Tests.collectionName, jsonBody: jsonDictToPut) { (response) in
            XCTAssertTrue(NSThread.isMainThread())
            XCTAssertNotNil(response)
            XCTAssertTrue(response.ok)
            XCTAssertEqual(response.entities!.count, 1)
            let entity = response.first!

            XCTAssertNotNil(entity)
            XCTAssertEqual(entity.uuid!, PUT_Tests.entityUUID)

            let updatedPropertyValue = entity[propertyNameToUpdate] as? String
            XCTAssertNotNil(updatedPropertyValue)
            XCTAssertEqual(updatedPropertyValue!,propertiesNewValue)
            putExpect.fulfill()
        }
        self.waitForExpectationsWithTimeout(10, handler: nil)
    }

    func test_PUT_WITH_ENTITY_OBJECT() {
        let propertyNameToUpdate = "\(#function)"
        let propertiesNewValue = "\(propertyNameToUpdate)_VALUE"
        let putExpect = self.expectationWithDescription(propertyNameToUpdate)

        Usergrid.GET(PUT_Tests.collectionName, uuidOrName: PUT_Tests.entityUUID) { (getResponse) in
            XCTAssertTrue(NSThread.isMainThread())
            XCTAssertNotNil(getResponse)
            XCTAssertTrue(getResponse.ok)
            XCTAssertEqual(getResponse.entities!.count, 1)

            var responseEntity = getResponse.first!

            XCTAssertNotNil(responseEntity)
            XCTAssertEqual(responseEntity.uuid!, PUT_Tests.entityUUID)

            responseEntity[propertyNameToUpdate] = propertiesNewValue

            Usergrid.PUT(responseEntity) { (putResponse) in
                XCTAssertTrue(NSThread.isMainThread())
                XCTAssertNotNil(putResponse)
                XCTAssertTrue(putResponse.ok)
                XCTAssertEqual(putResponse.entities!.count, 1)
                responseEntity = putResponse.first!

                XCTAssertNotNil(responseEntity)
                XCTAssertEqual(responseEntity.uuid!, PUT_Tests.entityUUID)

                let updatedPropertyValue = responseEntity[propertyNameToUpdate] as? String
                XCTAssertNotNil(updatedPropertyValue)
                XCTAssertEqual(updatedPropertyValue!,propertiesNewValue)
                putExpect.fulfill()
            }
        }
        self.waitForExpectationsWithTimeout(20, handler: nil)
    }

    func test_PUT_WITH_QUERY() {
        let propertyNameToUpdate = "\(#function)"
        let propertiesNewValue = "\(propertyNameToUpdate)_VALUE"
        let putExpect = self.expectationWithDescription(propertyNameToUpdate)

        Usergrid.PUT(self.query, jsonBody: [propertyNameToUpdate : propertiesNewValue]) { (putResponse) in
            XCTAssertTrue(NSThread.isMainThread())
            XCTAssertNotNil(putResponse)
            XCTAssertTrue(putResponse.ok)
            XCTAssertEqual(putResponse.entities!.count, 1)

            let responseEntity = putResponse.first!
            XCTAssertNotNil(responseEntity)

            let updatedPropertyValue = responseEntity[propertyNameToUpdate] as? String
            XCTAssertNotNil(updatedPropertyValue)
            XCTAssertEqual(updatedPropertyValue!,propertiesNewValue)
            putExpect.fulfill()
        }
        self.waitForExpectationsWithTimeout(10, handler: nil)
    }
}
