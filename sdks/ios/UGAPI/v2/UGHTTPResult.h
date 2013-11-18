//
//  UGHTTPResult.h
//  UGAPIApp
//
//  Created by Tim Burks on 4/3/13.
//
//

#import <Foundation/Foundation.h>

@interface UGHTTPResult : NSObject
@property (nonatomic, strong) NSHTTPURLResponse *response;
@property (nonatomic, strong) NSData *data;
@property (nonatomic, strong) NSError *error;
@property (nonatomic, strong) id object;
@property (nonatomic, readonly) NSString *UTF8String;

@end