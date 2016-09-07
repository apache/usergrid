/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import <Foundation/Foundation.h>


// response states
enum
{
    kUGClientResponseSuccess = 0,
    kUGClientResponseFailure = 1,
    kUGClientResponsePending = 2
};


@interface UGClientResponse : NSObject

// this will be a unique ID for this transaction. If you have
// multiple transactions in progress, you can keep track of them
// with this value. Note: The transaction ID of a synchronous
// call response is always -1.
@property int transactionID;

// this will be one of three possible valuse:
// kUGClientResponseSuccess: The operation is complete and was successful. response will 
//                          be valid, as will rawResponse
//
// kUGClientResponseFailure: There was an error with the operation. No further 
//                          processing will be done. response will be an NSString with
//                          a plain-text description of what went wrong. rawResponse
//                          will be valid if the error occurred after receiving data from
//                          the service. If it occurred before, rawResponse will be nil.
//
// kUGClientResponsePending: The call is being handled asynchronously and not yet complete. 
//                          response will be nil. rawResponse will also be nil
@property int transactionState;

// This is the response. The type of this variable is dependant on the call that caused
// this response. 
@property id response;

// This is the raw text that was returned by the server. 
@property NSString *rawResponse;

@end
