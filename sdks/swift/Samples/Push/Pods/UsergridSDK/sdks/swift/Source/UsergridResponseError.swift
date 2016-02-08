//
//  UsergridResponseError.swift
//  UsergridSDK
//
//  Created by Robert Walsh on 1/8/16.
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

/// A standard error object that contains details about a request failure.
public class UsergridResponseError: NSObject {

    // MARK: - Instance Properties -

    /// The error's name.
    public let errorName : String

    /// The error's description.
    public let errorDescription: String

    /// The exception.
    public var exception: String?

    /// The description.
    public override var description : String {
        return "Error Name: \(errorName).  Error Description: \(errorDescription).  Exception: \(exception)."
    }

    /// The debug description.
    public override var debugDescription : String {
        return "Error Name: \(errorName).  Error Description: \(errorDescription).  Exception: \(exception)."
    }

    // MARK: - Initialization -

    /**
    Designated initializer for `UsergridResponseError`.

    - parameter errorName:        The error's name.
    - parameter errorDescription: The error's description.
    - parameter exception:        The exception.

    - returns: A new instance of `UsergridResponseError`
    */
    public init(errorName:String, errorDescription:String, exception:String? = nil) {
        self.errorName = errorName
        self.errorDescription = errorDescription
        self.exception = exception
    }

    /**
     Convenience initializer for `UsergridResponseError` that determines if the given `jsonDictionary` contains an error.

     - parameter jsonDictionary: The JSON dictionary that may contain error information.

     - returns: A new instance of `UsergridResponseError` if the JSON dictionary did indeed contain error information.
     */
    public convenience init?(jsonDictionary:[String:AnyObject]) {
        if let errorName = jsonDictionary[USERGRID_ERROR] as? String,
               errorDescription = jsonDictionary[USERGRID_ERROR_DESCRIPTION] as? String {
            self.init(errorName:errorName,errorDescription:errorDescription,exception:jsonDictionary[USERGRID_EXCEPTION] as? String)
        } else {
            self.init(errorName:"",errorDescription:"")
            return nil
        }
    }
}

let USERGRID_ERROR = "error"
let USERGRID_ERROR_DESCRIPTION = "error_description"
let USERGRID_EXCEPTION = "exception"