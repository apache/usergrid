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

    static let collectionName = "books"
    static let entityUUID = "f4078aca-2fb1-11e5-8eb2-e13f8369aad1"
    static let pngLocation = "TestAssets/test.png"
    static let jpgLocation = "TestAssets/UsergridGuy.jpg"
    static let imageName = "test"

    override func setUp() {
        super.setUp()
        Usergrid.initSharedInstance(orgId:ClientCreationTests.orgId, appId: ClientCreationTests.appId)
        Usergrid.persistCurrentUserInKeychain = false
    }

    override func tearDown() {
        Usergrid._sharedClient = nil
        super.tearDown()
    }

    func getFullPathOfFile(fileLocation:String) -> String {
        return (NSBundle(forClass: object_getClass(self)).resourcePath! as NSString).stringByAppendingPathComponent(fileLocation)
    }

    func test_ASSET_INIT() {
        var filePath = self.getFullPathOfFile(ASSET_Tests.pngLocation)
        var image = UIImage(contentsOfFile: filePath)
        var asset = UsergridAsset(filename:ASSET_Tests.imageName,image: image!)
        XCTAssertNotNil(asset)
        XCTAssertNotNil(asset!.data)
        XCTAssertNotNil(asset!.filename)
        XCTAssertEqual(asset!.contentType, UsergridImageContentType.Png.stringValue)
        XCTAssertTrue(asset!.contentLength > 0)

        asset = UsergridAsset(filename:ASSET_Tests.imageName, fileURL: NSURL(fileURLWithPath: filePath))
        XCTAssertNotNil(asset)
        XCTAssertNotNil(asset!.data)
        XCTAssertNotNil(asset!.filename)
        XCTAssertEqual(asset!.contentType, UsergridImageContentType.Png.stringValue)
        XCTAssertTrue(asset!.contentLength > 0)

        filePath = self.getFullPathOfFile(ASSET_Tests.jpgLocation)
        image = UIImage(contentsOfFile: filePath)
        asset = UsergridAsset(filename:nil,image: image!, imageContentType:.Jpeg)
        XCTAssertNotNil(asset)
        XCTAssertNotNil(asset!.data)
        XCTAssertEqual(asset!.filename,UsergridAsset.DEFAULT_FILE_NAME)
        XCTAssertEqual(asset!.contentType, UsergridImageContentType.Jpeg.stringValue)
        XCTAssertTrue(asset!.contentLength > 0)
    }

    func test_IMAGE_UPLOAD() {
        let getExpect = self.expectationWithDescription("\(#function)")
        let uploadProgress : UsergridAssetRequestProgress = { (bytes,expected) in
            print("UPLOAD PROGRESS BLOCK: BYTES:\(bytes) --- EXPECTED:\(expected)")
        }
        let downloadProgress : UsergridAssetRequestProgress = { (bytes,expected) in
            print("DOWNLOAD PROGRESS BLOCK: BYTES:\(bytes) --- EXPECTED:\(expected)")
        }

        Usergrid.GET(ASSET_Tests.collectionName, uuidOrName:ASSET_Tests.entityUUID) { (response) in
            XCTAssertTrue(NSThread.isMainThread())

            let entity = response.first!
            XCTAssertNotNil(entity)
            XCTAssertFalse(entity.isUser)

            let imagePath = self.getFullPathOfFile(ASSET_Tests.pngLocation)
            XCTAssertNotNil(imagePath)

            let localImage = UIImage(contentsOfFile: imagePath)
            XCTAssertNotNil(localImage)

            let asset = UsergridAsset(filename:ASSET_Tests.imageName,image: localImage!)
            XCTAssertNotNil(asset)

            entity.uploadAsset(asset!, progress:uploadProgress) { uploadedAsset,response in
                XCTAssertTrue(NSThread.isMainThread())
                XCTAssertTrue(response.ok)
                XCTAssertNil(response.error)

                XCTAssertNotNil(asset)
                XCTAssertNotNil(uploadedAsset)
                XCTAssertEqual(uploadedAsset!, asset!)

                XCTAssertTrue(entity.hasAsset)
                XCTAssertNotNil(entity.fileMetaData)
                XCTAssertNotNil(entity.fileMetaData!.eTag)
                XCTAssertNotNil(entity.fileMetaData!.checkSum)
                XCTAssertNotNil(entity.fileMetaData!.contentType)
                XCTAssertNotNil(entity.fileMetaData!.lastModifiedDate)
                XCTAssertEqual(entity.asset!.contentLength, entity.fileMetaData!.contentLength)
                XCTAssertEqual(entity.asset!.contentType, entity.fileMetaData!.contentType)

                entity.downloadAsset(UsergridImageContentType.Png.stringValue, progress:downloadProgress)
                { (downloadedAsset, error) -> Void in
                    XCTAssertTrue(NSThread.isMainThread())
                    XCTAssertNotNil(downloadedAsset)
                    XCTAssertNil(error)
                    let downloadedImage = UIImage(data: downloadedAsset!.data)
                    XCTAssertEqual(UIImagePNGRepresentation(localImage!), UIImagePNGRepresentation(downloadedImage!))
                    XCTAssertNotNil(downloadedImage)
                    getExpect.fulfill()
                }
            }
        }
        self.waitForExpectationsWithTimeout(100, handler: nil)
    }

    func deleteUser(user:UsergridUser,expectation:XCTestExpectation) {
        user.remove() { removeResponse in
            XCTAssertTrue(NSThread.isMainThread())
            XCTAssertNotNil(removeResponse)
            XCTAssertTrue(removeResponse.ok)
            XCTAssertNotNil(removeResponse.user)
            XCTAssertNotNil(removeResponse.users)
            print(removeResponse.error)
            expectation.fulfill()
        }
    }

    func test_ATTACH_ASSET_TO_CURRENT_USER() {
        let userAssetExpect = self.expectationWithDescription("\(#function)")

        let user = UsergridUser(name:User_Tests.name, email:User_Tests.email, username:User_Tests.username, password:User_Tests.password)
        let uploadProgress : UsergridAssetRequestProgress = { (bytes,expected) in
            print("UPLOAD PROGRESS BLOCK: BYTES:\(bytes) --- EXPECTED:\(expected)")
        }
        let downloadProgress : UsergridAssetRequestProgress = { (bytes,expected) in
            print("DOWNLOAD PROGRESS BLOCK: BYTES:\(bytes) --- EXPECTED:\(expected)")
        }

        UsergridUser.checkAvailable(user.email, username: user.username) { error,available in

            XCTAssertTrue(NSThread.isMainThread())
            XCTAssertNil(error)
            XCTAssertTrue(available)

            user.create() { (createResponse) in
                XCTAssertTrue(NSThread.isMainThread())
                XCTAssertNotNil(createResponse)
                XCTAssertTrue(createResponse.ok)
                XCTAssertNotNil(createResponse.user)
                XCTAssertNotNil(createResponse.users)
                XCTAssertNotNil(user.uuid)

                user.login(user.username!, password:User_Tests.password) { (auth, loggedInUser, error) -> Void in
                    XCTAssertTrue(NSThread.isMainThread())
                    XCTAssertNil(error)
                    XCTAssertNotNil(auth)
                    XCTAssertNotNil(loggedInUser)
                    XCTAssertEqual(auth, user.auth!)

                    Usergrid.authenticateUser(user.auth!) { auth,currentUser,error in
                        XCTAssertTrue(NSThread.isMainThread())
                        XCTAssertNil(error)
                        XCTAssertNotNil(auth)
                        XCTAssertEqual(auth, user.auth!)

                        XCTAssertNotNil(currentUser)
                        XCTAssertNotNil(Usergrid.currentUser)
                        XCTAssertEqual(currentUser, Usergrid.currentUser!)

                        let imagePath = self.getFullPathOfFile(ASSET_Tests.pngLocation)
                        XCTAssertNotNil(imagePath)

                        let localImage = UIImage(contentsOfFile: imagePath)
                        XCTAssertNotNil(localImage)

                        let asset = UsergridAsset(filename:ASSET_Tests.imageName,image: localImage!)
                        XCTAssertNotNil(asset)

                        Usergrid.currentUser!.uploadAsset(asset!, progress:uploadProgress) { uploadedAsset,response in
                            XCTAssertTrue(NSThread.isMainThread())
                            XCTAssertTrue(response.ok)
                            XCTAssertNil(response.error)

                            XCTAssertNotNil(asset)
                            XCTAssertNotNil(uploadedAsset)
                            XCTAssertEqual(uploadedAsset!, asset!)

                            XCTAssertTrue(Usergrid.currentUser!.hasAsset)
                            XCTAssertNotNil(Usergrid.currentUser!.fileMetaData)
                            XCTAssertNotNil(Usergrid.currentUser!.fileMetaData!.eTag)
                            XCTAssertNotNil(Usergrid.currentUser!.fileMetaData!.checkSum)
                            XCTAssertNotNil(Usergrid.currentUser!.fileMetaData!.contentType)
                            XCTAssertNotNil(Usergrid.currentUser!.fileMetaData!.lastModifiedDate)
                            XCTAssertEqual(Usergrid.currentUser!.asset!.contentLength, Usergrid.currentUser!.fileMetaData!.contentLength)
                            XCTAssertEqual(Usergrid.currentUser!.asset!.contentType, Usergrid.currentUser!.fileMetaData!.contentType)

                            Usergrid.currentUser!.downloadAsset(UsergridImageContentType.Png.stringValue, progress:downloadProgress)
                                { (downloadedAsset, error) -> Void in
                                    XCTAssertTrue(NSThread.isMainThread())
                                    XCTAssertNotNil(downloadedAsset)
                                    XCTAssertNil(error)
                                    let downloadedImage = UIImage(data: downloadedAsset!.data)
                                    XCTAssertEqual(UIImagePNGRepresentation(localImage!), UIImagePNGRepresentation(downloadedImage!))
                                    XCTAssertNotNil(downloadedImage)
                                    self.deleteUser(Usergrid.currentUser!,expectation:userAssetExpect)
                            }
                        }
                    }
                }
            }
        }
        self.waitForExpectationsWithTimeout(100, handler: nil)
    }


    func test_FILE_META_DATA_NSCODING() {
        let fileMetaDataDict = ["content-type":"image/png",
                                "etag":"dfa7421ea4f35d33e12ba93979a46b7e",
                                "checkSum":"dfa7421ea4f35d33e12ba93979a46b7e",
                                "content-length":1417896,
                                "last-modified":1455728898545]
        
        let fileMetaData = UsergridFileMetaData(fileMetaDataJSON:fileMetaDataDict)

        let fileMetaDataCodingData = NSKeyedArchiver.archivedDataWithRootObject(fileMetaData)
        let newInstanceFromData = NSKeyedUnarchiver.unarchiveObjectWithData(fileMetaDataCodingData) as? UsergridFileMetaData
        XCTAssertNotNil(newInstanceFromData)

        if let newInstance = newInstanceFromData {
            XCTAssertEqual(fileMetaData.eTag,newInstance.eTag)
            XCTAssertEqual(fileMetaData.checkSum,newInstance.checkSum)
            XCTAssertEqual(fileMetaData.contentType,newInstance.contentType)
            XCTAssertEqual(fileMetaData.contentLength,newInstance.contentLength)
            XCTAssertEqual(fileMetaData.lastModifiedDate,newInstance.lastModifiedDate)
        }
    }

    func test_ASSET_NSCODING() {
        let imagePath = self.getFullPathOfFile(ASSET_Tests.pngLocation)
        let asset = UsergridAsset(filename:ASSET_Tests.imageName,fileURL: NSURL(fileURLWithPath: imagePath))
        XCTAssertNotNil(asset)

        if let originalAsset = asset {
            let assetCodingData = NSKeyedArchiver.archivedDataWithRootObject(originalAsset)
            let newInstanceFromData = NSKeyedUnarchiver.unarchiveObjectWithData(assetCodingData) as? UsergridAsset

            XCTAssertNotNil(newInstanceFromData)

            if let newInstance = newInstanceFromData {
                XCTAssertEqual(originalAsset.filename,newInstance.filename)
                XCTAssertEqual(originalAsset.data,newInstance.data)
                XCTAssertEqual(originalAsset.originalLocation,newInstance.originalLocation)
                XCTAssertEqual(originalAsset.contentType,newInstance.contentType)
                XCTAssertEqual(originalAsset.contentLength,newInstance.contentLength)
            }
        }
    }
}
