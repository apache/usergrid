//
//  UsergridFileMetaData.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 10/6/15.
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

import Foundation

/**
`UsergridFileMetaData` is a helper class for dealing with reading `UsergridEntity` file meta data.
*/
public class UsergridFileMetaData : NSObject,NSCoding {

    internal static let FILE_METADATA = "file-metadata"

    // MARK: - Instance Properties -

    /// The eTag.
    public let eTag: String?

    /// The check sum.
    public let checkSum: String?

    /// The content type associated with the file data.
    public let contentType: String?

    /// The content length of the file data.
    public let contentLength: Int

    /// The last modified time stamp.
    public let lastModifiedTimeStamp: Int

    /// The `NSDate` object corresponding to the last modified time stamp.
    public let lastModifiedDate: NSDate?

    // MARK: - Initialization -

    /**
    Designated initializer for `UsergridFileMetaData` objects.

    - parameter fileMetaDataJSON: The file meta data JSON dictionary.

    - returns: A new instance of `UsergridFileMetaData`.
    */
    public init(fileMetaDataJSON:[String:AnyObject]) {
        self.eTag = fileMetaDataJSON["etag"] as? String
        self.checkSum = fileMetaDataJSON["checksum"] as? String
        self.contentType = fileMetaDataJSON["content-type"] as? String
        self.contentLength = fileMetaDataJSON["content-length"] as? Int ?? 0
        self.lastModifiedTimeStamp = fileMetaDataJSON["last-modified"] as? Int ?? 0

        if self.lastModifiedTimeStamp > 0 {
            self.lastModifiedDate = NSDate(milliseconds: self.lastModifiedTimeStamp.description)
        } else {
            self.lastModifiedDate = nil
        }
    }

    // MARK: - NSCoding -

    /**
    NSCoding protocol initializer.

    - parameter aDecoder: The decoder.

    - returns: A decoded `UsergridUser` object.
    */
    required public init?(coder aDecoder: NSCoder) {
        self.eTag = aDecoder.decodeObjectForKey("etag") as? String
        self.checkSum = aDecoder.decodeObjectForKey("checksum") as? String
        self.contentType = aDecoder.decodeObjectForKey("content-type") as? String
        self.contentLength = aDecoder.decodeIntegerForKey("content-length") ?? 0
        self.lastModifiedTimeStamp = aDecoder.decodeIntegerForKey("last-modified") ?? 0

        if self.lastModifiedTimeStamp > 0 {
            self.lastModifiedDate = NSDate(milliseconds: self.lastModifiedTimeStamp.description)
        } else {
            self.lastModifiedDate = nil
        }
    }

    /**
     NSCoding protocol encoder.

     - parameter aCoder: The encoder.
     */
    public func encodeWithCoder(aCoder: NSCoder) {
        aCoder.encodeObject(self.eTag, forKey: "etag")
        aCoder.encodeObject(self.checkSum, forKey: "checksum")
        aCoder.encodeObject(self.contentType, forKey: "content-type")
        aCoder.encodeInteger(self.contentLength, forKey: "content-length")
        aCoder.encodeInteger(self.lastModifiedTimeStamp, forKey: "last-modified")
    }
}
