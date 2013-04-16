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
        _object = [NSJSONSerialization JSONObjectWithData:_data options:NSJSONReadingMutableLeaves error:&error];
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