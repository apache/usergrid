//
//  UsergridAssetRequestWrapper.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 10/1/15.
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

typealias UsergridAssetRequestWrapperCompletionBlock = (requestWrapper:UsergridAssetRequestWrapper) -> Void

final class UsergridAssetRequestWrapper {
    weak var session: NSURLSession?
    let sessionTask: NSURLSessionTask

    var response: NSURLResponse?
    var responseData: NSData?
    var error: NSError?

    var progress: UsergridAssetRequestProgress?
    let completion: UsergridAssetRequestWrapperCompletionBlock

    init(session:NSURLSession?, sessionTask:NSURLSessionTask, progress:UsergridAssetRequestProgress?, completion:UsergridAssetRequestWrapperCompletionBlock) {
        self.session = session
        self.sessionTask = sessionTask
        self.progress = progress
        self.completion = completion
    }
}