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
//
//  UGHTTPResult.m
//  UGAPIApp
//
//  Created by Tim Burks on 4/3/13.
//
//

#import "UGHTTPResult.h"

@implementation UGHTTPResult

- (id) object {
    if (!_object && !_error) {
        NSError *error;
        // NSLog(@"JSON %@", [[NSString alloc] initWithData:_data encoding:NSUTF8StringEncoding]);
        _object = [NSJSONSerialization JSONObjectWithData:_data options:0 error:&error];
        _error = error;
        if (_error) {
            NSLog(@"JSON ERROR: %@", [error description]);
        }
    }
    return _object;
}

- (NSString *) UTF8String {
    return [[NSString alloc] initWithData:self.data encoding:NSUTF8StringEncoding];
}

@end