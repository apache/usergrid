//
//  ASSET_Tests.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 9/24/15.
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

class ASSET_Tests: XCTestCase {

    let sharedClient = Usergrid.initSharedInstance(orgId:ClientCreationTests.orgId, appId: ClientCreationTests.appId)

    static let collectionName = "books"
    static let entityUUID = "f4078aca-2fb1-11e5-8eb2-e13f8369aad1"
    static let imageLocation = "TestAssets/test.png"
    static let imageName = "test"

    func getFullPathOfFile(fileLocation:String) -> String {
        return (NSBundle(forClass: object_getClass(self)).resourcePath! as NSString).stringByAppendingPathComponent(fileLocation)
    }

    func test_IMAGE_UPLOAD() {
        let getExpect = self.expectationWithDescription("\(__FUNCTION__)")
        let uploadProgress : UsergridAssetRequestProgress = { (bytes,expected) in
            print("UPLOAD PROGRESS BLOCK: BYTES:\(bytes) --- EXPECTED:\(expected)")
        }
        let downloadProgress : UsergridAssetRequestProgress = { (bytes,expected) in
            print("DOWNLOAD PROGRESS BLOCK: BYTES:\(bytes) --- EXPECTED:\(expected)")
        }

        Usergrid.GET(ASSET_Tests.collectionName, uuidOrName:ASSET_Tests.entityUUID) { (response) in
            let entity = response.first!
            XCTAssertNotNil(entity)

            let imagePath = self.getFullPathOfFile(ASSET_Tests.imageLocation)
            XCTAssertNotNil(imagePath)

            let localImage = UIImage(contentsOfFile: imagePath)
            XCTAssertNotNil(localImage)

            let asset = UsergridAsset(fileName:ASSET_Tests.imageName,image: localImage!)
            XCTAssertNotNil(asset)

            entity.uploadAsset(self.sharedClient, asset:asset!, progress:uploadProgress) { (response, uploadedAsset, error) -> Void in
                XCTAssertNotNil(asset)
                XCTAssertNil(error)
                XCTAssertTrue(response.ok)
                entity.downloadAsset(UsergridImageContentType.Png.stringValue, progress:downloadProgress)
                { (downloadedAsset, error) -> Void in
                    XCTAssertNotNil(downloadedAsset)
                    XCTAssertNil(error)
                    let downloadedImage = UIImage(data: downloadedAsset!.data)
                    XCTAssertEqual(UIImagePNGRepresentation(localImage!), UIImagePNGRepresentation(downloadedImage!))
                    XCTAssertNotNil(downloadedImage)
                    getExpect.fulfill()
                }
            }
        }
        self.waitForExpectationsWithTimeout(10, handler: nil)
    }
}
