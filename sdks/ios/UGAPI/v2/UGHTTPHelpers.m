//
//  UGHTTPHelpers.m
//  UGHTTP
//
//  Created by Tim Burks on 2/24/12.
//  Copyright (c) 2012 Radtastical Inc. All rights reserved.
//

#import "UGHTTPHelpers.h"
#import <wctype.h>

static unichar char_to_int(unichar c)
{
    switch (c) {
        case '0': return 0;
        case '1': return 1;
        case '2': return 2;
        case '3': return 3;
        case '4': return 4;
        case '5': return 5;
        case '6': return 6;
        case '7': return 7;
        case '8': return 8;
        case '9': return 9;
        case 'A': case 'a': return 10;
        case 'B': case 'b': return 11;
        case 'C': case 'c': return 12;
        case 'D': case 'd': return 13;
        case 'E': case 'e': return 14;
        case 'F': case 'f': return 15;
    }
    return 0;                                     // not good
}

static char int_to_char[] = "0123456789ABCDEF";

@implementation NSString (UGHTTPHelpers)

- (NSString *) URLEncodedString
{
    NSMutableString *result = [NSMutableString string];
    int i = 0;
    const char *source = [self cStringUsingEncoding:NSUTF8StringEncoding];
    unsigned long max = strlen(source);
    while (i < max) {
        unsigned char c = source[i++];
        if (c == ' ') {
            [result appendString:@"%20"];
        }
        else if (iswalpha(c) || iswdigit(c) || (c == '-') || (c == '.') || (c == '_') || (c == '~')) {
            [result appendFormat:@"%c", c];
        }
        else {
            [result appendString:[NSString stringWithFormat:@"%%%c%c", int_to_char[(c/16)%16], int_to_char[c%16]]];
        }
    }
    return result;
}

- (NSString *) URLDecodedString
{
    int i = 0;
    NSUInteger max = [self length];
    char *buffer = (char *) malloc ((max + 1) * sizeof(char));
    int j = 0;
    while (i < max) {
        char c = [self characterAtIndex:i++];
        switch (c) {
            case '+':
                buffer[j++] = ' ';
                break;
            case '%':
                buffer[j++] =
                char_to_int([self characterAtIndex:i])*16
                + char_to_int([self characterAtIndex:i+1]);
                i = i + 2;
                break;
            default:
                buffer[j++] = c;
                break;
        }
    }
    buffer[j] = 0;
    NSString *result = [NSMutableString stringWithCString:buffer encoding:NSUTF8StringEncoding];
    if (!result) result = [NSMutableString stringWithCString:buffer encoding:NSASCIIStringEncoding];
    free(buffer);
    return result;
}

- (NSDictionary *) URLQueryDictionary
{
    NSMutableDictionary *result = [NSMutableDictionary dictionary];
    NSArray *pairs = [self componentsSeparatedByString:@"&"];
    int i;
    NSUInteger max = [pairs count];
    for (i = 0; i < max; i++) {
        NSArray *pair = [[pairs objectAtIndex:i] componentsSeparatedByString:@"="];
        if ([pair count] == 2) {
            NSString *key = [[pair objectAtIndex:0] URLDecodedString];
            NSString *value = [[pair objectAtIndex:1] URLDecodedString];
            [result setObject:value forKey:key];
        }
    }
    return result;
}

@end

@implementation NSDictionary (UGHTTPHelpers)

- (NSString *) URLQueryString
{
    NSMutableString *result = [NSMutableString string];
    NSEnumerator *keyEnumerator = [[[self allKeys] sortedArrayUsingSelector:@selector(compare:)] objectEnumerator];
    id key;
    while ((key = [keyEnumerator nextObject])) {
        id value = [self objectForKey:key];
        if (![value isKindOfClass:[NSString class]]) {
            if ([value respondsToSelector:@selector(stringValue)]) {
                value = [value stringValue];
            }
        }
        if ([value isKindOfClass:[NSString class]]) {
            if ([result length] > 0) [result appendString:@"&"];
            [result appendString:[NSString stringWithFormat:@"%@=%@",
                                  [key URLEncodedString],
                                  [value URLEncodedString]]];
        }
    }
    return [NSString stringWithString:result];
}

- (NSData *) URLQueryData
{
    return [[self URLQueryString] dataUsingEncoding:NSUTF8StringEncoding];
}

@end

@implementation NSData (UGHTTPHelpers)

- (NSDictionary *) URLQueryDictionary {
    return [[[NSString alloc] initWithData:self encoding:NSUTF8StringEncoding] URLQueryDictionary];
}

@end

