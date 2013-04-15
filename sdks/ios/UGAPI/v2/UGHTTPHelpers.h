//
//  UGHTTPHelpers.h
//  UGHTTP
//
//  Created by Tim Burks on 2/24/12.
//  Copyright (c) 2012 Radtastical Inc. All rights reserved.
//
#import <Foundation/Foundation.h>

@interface NSString (UGHTTPHelpers)
- (NSString *) URLEncodedString;
- (NSString *) URLDecodedString;
- (NSDictionary *) URLQueryDictionary;
@end

@interface NSData (UGHTTPHelpers)
- (NSDictionary *) URLQueryDictionary;
@end

@interface NSDictionary (UGHTTPHelpers)
- (NSString *) URLQueryString;
- (NSData *) URLQueryData;
@end


