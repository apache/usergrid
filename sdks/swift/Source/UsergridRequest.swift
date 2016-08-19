//
//  UsergridRequest.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 1/12/16.
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
 The UsergridRequest class incapsulates the properties that all requests made by the SDK have in common.  

 This class is also functions to create `NSURLRequest` objects based on the properties of the class.
*/
public class UsergridRequest : NSObject {

    // MARK: - Instance Properties -

    /// The HTTP method.
    public let method: UsergridHttpMethod

    /// The base URL.
    public let baseUrl: String

    /// The paths to append to the base URL.
    public let paths: [String]?

    /// The query to append to the URL.
    public let query: UsergridQuery?

    /// The auth that will be used.
    public let auth: UsergridAuth?

    /// The headers to add to the request.
    public let headers: [String:String]?

    /// The JSON body that will be set on the request.  Can be either a valid JSON object or NSData.
    public let jsonBody: AnyObject?
    
    /// The query params that will be set on the request.
    public let queryParams: [String:String]?

    // MARK: - Initialization -

    /**
    The designated initializer for `UsergridRequest` objects.
    
    - parameter method:      The HTTP method.
    - parameter baseUrl:     The base URL.
    - parameter paths:       The optional paths to append to the base URL.
    - parameter query:       The optional query to append to the URL.
    - parameter auth:        The optional `UsergridAuth` that will be used in the Authorization header.
    - parameter headers:     The optional headers.
    - parameter jsonBody:    The optional JSON body. Can be either a valid JSON object or NSData.
    - parameter queryParams: The optional query params to be appended to the request url.
    
    - returns: A new instance of `UsergridRequest`.
    */
    public init(method:UsergridHttpMethod,
        baseUrl:String,
        paths:[String]? = nil,
        query:UsergridQuery? = nil,
        auth:UsergridAuth? = nil,
        headers:[String:String]? = nil,
        jsonBody:AnyObject? = nil,
        queryParams:[String:String]? = nil) {
            self.method = method
            self.baseUrl = baseUrl
            self.paths = paths
            self.auth = auth
            self.headers = headers
            self.query = query
            self.queryParams = queryParams
            if let body = jsonBody where (body is NSData || NSJSONSerialization.isValidJSONObject(body)) {
                self.jsonBody = body
            } else {
                self.jsonBody = nil
            }
    }

    // MARK: - Instance Methods -

    /**
    Constructs a `NSURLRequest` object with this objects instance properties.

    - returns: An initialized and configured `NSURLRequest` object.
    */
    public func buildNSURLRequest() -> NSURLRequest {
        let request = NSMutableURLRequest(URL: self.buildURL())
        request.HTTPMethod = self.method.stringValue
        self.applyHeaders(request)
        self.applyBody(request)
        self.applyAuth(request)
        return request
    }

    private func buildURL() -> NSURL {
        var constructedURLString = self.baseUrl
        if let appendingPaths = self.paths {
            for pathToAppend in appendingPaths {
                if let encodedPath = pathToAppend.stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLPathAllowedCharacterSet()) {
                    constructedURLString = "\(constructedURLString)\(UsergridRequest.FORWARD_SLASH)\(encodedPath)"
                }
            }
        }
        if let queryToAppend = self.query {
            let appendFromQuery = queryToAppend.build()
            if !appendFromQuery.isEmpty {
                constructedURLString = "\(constructedURLString)\(UsergridRequest.FORWARD_SLASH)\(appendFromQuery)"
            }
        }
        if let queryParams = self.queryParams {
            if let components = NSURLComponents(string: constructedURLString) {
                components.queryItems = components.queryItems ?? []
                for (key, value) in queryParams {
                    let q: NSURLQueryItem = NSURLQueryItem(name: key, value: value)
                    components.queryItems!.append(q)
                }
                constructedURLString = components.string!
            }
        }
        return NSURL(string:constructedURLString)!
    }

    private func applyHeaders(request:NSMutableURLRequest) {
        if let httpHeaders = self.headers {
            for (key,value) in httpHeaders {
                request.setValue(value, forHTTPHeaderField: key)
            }
        }
    }

    private func applyBody(request:NSMutableURLRequest) {
        if let jsonBody = self.jsonBody, httpBody = UsergridRequest.jsonBodyToData(jsonBody) {
            request.HTTPBody = httpBody
            request.setValue(String(format: "%lu", httpBody.length), forHTTPHeaderField: UsergridRequest.CONTENT_LENGTH)
        }
    }

    private func applyAuth(request:NSMutableURLRequest) {
        if let usergridAuth = self.auth {
            if usergridAuth.isValid, let accessToken = usergridAuth.accessToken {
                request.setValue("\(UsergridRequest.BEARER) \(accessToken)", forHTTPHeaderField: UsergridRequest.AUTHORIZATION)
            }
        }
    }

    private static func jsonBodyToData(jsonBody:AnyObject) -> NSData? {
        if let jsonBodyAsNSData = jsonBody as? NSData {
            return jsonBodyAsNSData
        } else {
            var jsonBodyAsNSData: NSData? = nil
            do { jsonBodyAsNSData = try NSJSONSerialization.dataWithJSONObject(jsonBody, options: NSJSONWritingOptions(rawValue: 0)) }
            catch { print(error) }
            return jsonBodyAsNSData
        }
    }

    private static let AUTHORIZATION = "Authorization"
    private static let ACCESS_TOKEN = "access_token"
    private static let APPLICATION_JSON = "application/json; charset=utf-8"
    private static let BEARER = "Bearer"
    private static let CONTENT_LENGTH = "Content-Length"
    private static let CONTENT_TYPE = "Content-Type"
    private static let FORWARD_SLASH = "/"

    static let JSON_CONTENT_TYPE_HEADER = [UsergridRequest.CONTENT_TYPE:UsergridRequest.APPLICATION_JSON]
}

/**
 The `UsergridRequest` sub class which is used for uploading assets.
 */
public class UsergridAssetUploadRequest: UsergridRequest {

    // MARK: - Instance Properties -

    /// The asset to use for uploading.
    public let asset: UsergridAsset

    /// A constructed multipart http body for requests to upload.
    public var multiPartHTTPBody: NSData {
        let httpBodyString = UsergridAssetUploadRequest.MULTIPART_START +
            "\(UsergridAssetUploadRequest.CONTENT_DISPOSITION):\(UsergridAssetUploadRequest.FORM_DATA); name=file; filename=\(self.asset.filename)\r\n" +
            "\(UsergridRequest.CONTENT_TYPE): \(self.asset.contentType)\r\n\r\n" as NSString

        let httpBody = NSMutableData()
        httpBody.appendData(httpBodyString.dataUsingEncoding(NSUTF8StringEncoding)!)
        httpBody.appendData(self.asset.data)
        httpBody.appendData(UsergridAssetUploadRequest.MULTIPART_END.dataUsingEncoding(NSUTF8StringEncoding)!)

        return httpBody
    }

    // MARK: - Initialization -

    /**
     The designated initializer for `UsergridAssetUploadRequest` objects.

     - parameter baseUrl: The base URL.
     - parameter paths:   The optional paths to append to the base URL.
     - parameter auth:    The optional `UsergridAuth` that will be used in the Authorization header.
     - parameter asset:   The asset to upload.

    - returns: A new instance of `UsergridRequest`.
     */
    public init(baseUrl:String,
                paths:[String]? = nil,
                auth:UsergridAuth? = nil,
                asset:UsergridAsset) {
                    self.asset = asset
                    super.init(method: .Put, baseUrl: baseUrl, paths: paths, auth: auth)
    }

    private override func applyHeaders(request: NSMutableURLRequest) {
        super.applyHeaders(request)
        request.setValue(UsergridAssetUploadRequest.ASSET_UPLOAD_CONTENT_HEADER, forHTTPHeaderField: UsergridRequest.CONTENT_TYPE)
        request.setValue(String(format: "%lu", self.multiPartHTTPBody.length), forHTTPHeaderField: UsergridRequest.CONTENT_LENGTH)
    }

    private static let ASSET_UPLOAD_BOUNDARY = "usergrid-asset-upload-boundary"
    private static let ASSET_UPLOAD_CONTENT_HEADER = "multipart/form-data; boundary=\(UsergridAssetUploadRequest.ASSET_UPLOAD_BOUNDARY)"
    private static let CONTENT_DISPOSITION = "Content-Disposition"
    private static let MULTIPART_START = "--\(UsergridAssetUploadRequest.ASSET_UPLOAD_BOUNDARY)\r\n"
    private static let MULTIPART_END = "\r\n--\(UsergridAssetUploadRequest.ASSET_UPLOAD_BOUNDARY)--\r\n" as NSString
    private static let FORM_DATA = "form-data"
}
