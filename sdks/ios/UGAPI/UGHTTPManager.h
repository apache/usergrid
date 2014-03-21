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

enum
{
    kUGHTTPGet = 0,
    kUGHTTPPost = 1,
    kUGHTTPPostAuth = 2,
    kUGHTTPPut = 3,
    kUGHTTPDelete = 4
};

@interface UGHTTPManager : NSObject

// blocks until a response is received, or until there's an error.
// in the event of a response, it's returned. If there's an error, 
// the funciton returns nil and you can call getLastError to see what
// went wrong.
-(NSString *)syncTransaction:(NSString *)url operation:(int)op operationData:(NSString *)opData;

// sets up the transaction asynchronously. The delegate that's sent in
// must have the following functions: 
//
// -(void)httpManagerError:(UGHTTPManager *)manager error:(NSString *)error
// -(void)httpManagerResponse:(UGHTTPManager *)manager response:(NSString *)response
//
// In all cases, it returns a transaction ID. A return value
// of -1 means there was an error.
// You can call getLastError to find out what went wrong. 
-(int)asyncTransaction:(NSString *)url operation:(int)op operationData:(NSString *)opData delegate:(id)delegate;

// get the current transactionID
-(int)getTransactionID;

// sets the auth key
-(void)setAuth: (NSString *)auth;

// cancel a pending transaction. The delegate will not be called and the results
// will be ignored. Though the server side will still have happened.
-(void)cancel;

// returns YES if this instance is available. NO if this instance is currently
// in use as part of an asynchronous transaction.
-(BOOL)isAvailable;

// sets the availability flag of this instance. This is done by UGClient
-(void)setAvailable:(BOOL)available;

// a helpful utility function to make a string comform to URL
// rules. It will escape all the special characters.
+(NSString *)escapeSpecials:(NSString *)raw;

// At all times, this will return the plain-text explanation of the last
// thing that went wrong. It is cleared to "No Error" at the beginnign of 
// each new transaction.
-(NSString *)getLastError;

@end
