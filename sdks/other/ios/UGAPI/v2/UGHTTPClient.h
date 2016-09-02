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

@class UGHTTPResult;

typedef void (^UGHTTPCompletionHandler)(UGHTTPResult *result);
typedef void (^UGHTTPProgressHandler)(CGFloat progress);

@interface UGHTTPClient : NSObject
#if TARGET_OS_IPHONE
<NSURLConnectionDataDelegate>
#endif

@property (nonatomic, copy) UGHTTPCompletionHandler completionHandler;
@property (nonatomic, copy) UGHTTPProgressHandler progressHandler;
@property (readonly) CGFloat progress;
@property (readonly) BOOL isRunning;

- (id) initWithRequest:(NSMutableURLRequest *) request;

- (UGHTTPResult *) connect;

- (void) connectWithCompletionHandler:(UGHTTPCompletionHandler) completionHandler;

- (void) connectWithCompletionHandler:(UGHTTPCompletionHandler) completionHandler
                      progressHandler:(UGHTTPProgressHandler) progressHandler;

- (void) cancel;

@end
