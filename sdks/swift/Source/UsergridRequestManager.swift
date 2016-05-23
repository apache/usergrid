//
//  UsergridRequestManager.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 9/22/15.
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

final class UsergridRequestManager {

    unowned let client: UsergridClient

    let session: NSURLSession

    var sessionDelegate : UsergridSessionDelegate {
        return session.delegate as! UsergridSessionDelegate
    }

    init(client:UsergridClient) {
        self.client = client

        let config = NSURLSessionConfiguration.defaultSessionConfiguration()

        #if os(tvOS)
        config.HTTPAdditionalHeaders = ["User-Agent": "usergrid-tvOS/v\(UsergridSDKVersion)"]
        #elseif os(iOS)
        config.HTTPAdditionalHeaders = ["User-Agent": "usergrid-ios/v\(UsergridSDKVersion)"]
        #elseif os(watchOS)
        config.HTTPAdditionalHeaders = ["User-Agent": "usergrid-watchOS/v\(UsergridSDKVersion)"]
        #elseif os(OSX)
        config.HTTPAdditionalHeaders = ["User-Agent": "usergrid-osx/v\(UsergridSDKVersion)"]
        #endif

        self.session = NSURLSession(configuration:  config,
                                    delegate:       UsergridSessionDelegate(),
                                    delegateQueue:  nil)
    }

    deinit {
        session.invalidateAndCancel()
    }

    func performRequest(request:UsergridRequest, completion:UsergridResponseCompletion?) {
        session.dataTaskWithRequest(request.buildNSURLRequest()) { [weak self] (data, response, error) -> Void in
            let response = UsergridResponse(client:self?.client, data: data, response: response as? NSHTTPURLResponse, error: error)
            dispatch_async(dispatch_get_main_queue()) {
                completion?(response: response)
            }
        }.resume()
    }
}


// MARK: - Authentication -
extension UsergridRequestManager {

    static func getTokenAndExpiryFromResponseJSON(jsonDict:[String:AnyObject]) -> (token:String?,expiry:NSDate?) {
        var token: String? = nil
        var expiry: NSDate? = nil
        if let accessToken = jsonDict["access_token"] as? String {
            token = accessToken
        }
        if let expiresIn = jsonDict["expires_in"] as? Int {
            let expiresInAdjusted = expiresIn - 5000
            expiry = NSDate(timeIntervalSinceNow: NSTimeInterval(expiresInAdjusted))
        }
        return (token,expiry)
    }

    func performUserAuthRequest(userAuth:UsergridUserAuth, request:UsergridRequest, completion:UsergridUserAuthCompletionBlock?) {
        session.dataTaskWithRequest(request.buildNSURLRequest()) { (data, response, error) -> Void in
            let dataAsJSON = try! NSJSONSerialization.JSONObjectWithData(data!, options: NSJSONReadingOptions.MutableContainers)

            var createdUser: UsergridUser? = nil
            var responseError: UsergridResponseError? = nil

            if let jsonDict = dataAsJSON as? [String:AnyObject] {
                let tokenAndExpiry = UsergridRequestManager.getTokenAndExpiryFromResponseJSON(jsonDict)
                userAuth.accessToken = tokenAndExpiry.token
                userAuth.expiry = tokenAndExpiry.expiry

                if let userDict = jsonDict[UsergridUser.USER_ENTITY_TYPE] as? [String:AnyObject] {
                    if let newUser = UsergridEntity.entity(jsonDict: userDict) as? UsergridUser {
                        newUser.auth = userAuth
                        createdUser = newUser
                    }
                }
                if createdUser == nil {
                    responseError = UsergridResponseError(jsonDictionary: jsonDict) ?? UsergridResponseError(errorName: "Auth Failed.", errorDescription: "Error Description: \(error?.localizedDescription).")
                }
            } else {
                responseError = UsergridResponseError(errorName: "Auth Failed.", errorDescription: "Error Description: \(error?.localizedDescription).")
            }

            dispatch_async(dispatch_get_main_queue()) {
                completion?(auth: userAuth, user: createdUser, error: responseError)
            }
        }.resume()
    }

    func performAppAuthRequest(appAuth: UsergridAppAuth, request: UsergridRequest, completion: UsergridAppAuthCompletionBlock?) {
        session.dataTaskWithRequest(request.buildNSURLRequest()) { (data, response, error) -> Void in
            let dataAsJSON = try! NSJSONSerialization.JSONObjectWithData(data!, options: NSJSONReadingOptions.MutableContainers)

            var responseError: UsergridResponseError? = nil

            if let jsonDict = dataAsJSON as? [String:AnyObject] {
                let tokenAndExpiry = UsergridRequestManager.getTokenAndExpiryFromResponseJSON(jsonDict)
                appAuth.accessToken = tokenAndExpiry.token
                appAuth.expiry = tokenAndExpiry.expiry
            } else {
                responseError = UsergridResponseError(errorName: "Auth Failed.", errorDescription: "Error Description: \(error?.localizedDescription).")
            }

            dispatch_async(dispatch_get_main_queue()) {
                completion?(auth: appAuth, error: responseError)
            }
        }.resume()
    }
}

// MARK: - Asset Management -
extension UsergridRequestManager {

    func performAssetDownload(contentType:String, usergridRequest:UsergridRequest, progress: UsergridAssetRequestProgress? = nil, completion:UsergridAssetDownloadCompletion? = nil) {
        let downloadTask = session.downloadTaskWithRequest(usergridRequest.buildNSURLRequest())
        let requestWrapper = UsergridAssetRequestWrapper(session: self.session, sessionTask: downloadTask, progress: progress)  { (request) -> Void in
            var asset: UsergridAsset? = nil
            var responseError: UsergridResponseError? = nil

            if let assetData = request.responseData where assetData.length > 0 {
                asset = UsergridAsset(data: assetData, contentType: contentType)
            } else {
                responseError = UsergridResponseError(errorName: "Download Failed.", errorDescription: "Downloading asset failed.  No data was recieved.")
            }

            dispatch_async(dispatch_get_main_queue()) {
                completion?(asset: asset, error: responseError)
            }
        }
        self.sessionDelegate.addRequestDelegate(requestWrapper.sessionTask, requestWrapper:requestWrapper)
        requestWrapper.sessionTask.resume()
    }

    func performAssetUpload(usergridRequest:UsergridAssetUploadRequest, progress:UsergridAssetRequestProgress? = nil, completion: UsergridAssetUploadCompletion? = nil) {
        let uploadTask = session.uploadTaskWithRequest(usergridRequest.buildNSURLRequest(), fromData: usergridRequest.multiPartHTTPBody)
        let requestWrapper = UsergridAssetRequestWrapper(session: self.session, sessionTask: uploadTask, progress: progress)  { [weak self] (request) -> Void in
            let response = UsergridResponse(client: self?.client, data: request.responseData, response: request.response as? NSHTTPURLResponse, error: request.error)
            dispatch_async(dispatch_get_main_queue()) {
                completion?(asset: usergridRequest.asset, response: response)
            }
        }
        self.sessionDelegate.addRequestDelegate(requestWrapper.sessionTask, requestWrapper:requestWrapper)
        requestWrapper.sessionTask.resume()
    }
}