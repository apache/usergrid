#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>

enum
{
    kUGQueryOperationEquals = 0,
    kUGQueryOperationLessThan = 1,
    kUGQueryOperationLessThanOrEqualTo = 2,
    kUGQueryOperationGreaterThan = 3,
    kUGQueryOperationGreaterThanOrEqualTo = 4
};

/***************************************************************
 QUERY MANAGEMENT:
 
 All query functions take one of these as an optional parameter
 (the client may send nil if desired). This will specify the
 limitations of the query. 
***************************************************************/

@interface UGQuery : NSObject

// url terms used in UG searches. Set as you like.
// These are convenience methods. the same effect can be done
// by calling addURLTerm
-(void)setConsumer: (NSString *)consumer;
-(void)setLastUUID: (NSString *)lastUUID;
-(void)setTime: (long)time;
-(void)setPrev: (int)prev;
-(void)setNext: (int)next;
-(void)setLimit: (int)limit;
-(void)setPos: (NSString *)pos;
-(void)setUpdate: (BOOL)update;
-(void)setSynchronized: (BOOL)synchronized;

// a general function for adding additional URL terms.
// Note that all of the set functions above turn around and
// call this.
-(void)addURLTerm: (NSString *)urlTerm equals:(NSString *)equals;

// ql operation requirements. For each of these, you provide the term, followed 
// by the operation (a kUGQueryOperationXXXX constant) followed by the value
// in whatever form you have it (NSString, int, or float are supported)
// Example: [foo addRequiredOperation: @"age" kUGQueryLessThan valueInt:27] would
// add the term "age < 27" to the ql.
-(void)addRequiredOperation: (NSString *)term op:(int)op valueStr:(NSString *)valueStr;
-(void)addRequiredOperation: (NSString *)term op:(int)op valueInt:(int) valueInt;

// adds a "contains" requirement to the query. This adds the requirement that a value
// contain a given string. Example: [foo addRequiredContains:@"hobbies value:@"fishing"]
// would add the term "hobbies contains 'fishing'" to the ql.
-(void)addRequiredContains: (NSString *)term value:(NSString *)value;

// adds an "in" requirement to the query. This adds a requirement that a field
// be within a certain range. Example [foo appendRequiredIn:@"age" low:16.0 high:22.0]
// would add the term "age in 16.0,22.0" to the ql. 
// Note that the qualifier is inclusive, meaning it is true if low <= term <= high. 
-(void)addRequiredIn:(NSString *)term low:(int)low high:(int)high;

// adds a "within" requirement. This adds a constraint that the term be within a 
// certain distance of the sent-in x,y coordinates.
-(void)addRequiredWithin:(NSString *)term latitude:(float)latitude longitude:(float)longitude distance:(float)distance;

// assembles a "within" requirement with a term name, CLLocation, and distance
-(void)addRequiredWithinLocation:(NSString *)term location:(CLLocation *)location distance:(float)distance;

//-------------------- Oblique usage ----------------------------
// adds a requirement to the query. The requirements will
// *all* be sent when the query is adopted. This is an escalating
// list as you add them. Requirements are in UG Querty language.
// So something like "firstname='bob'". This is one of the few places
// where the data you give will be sent to the server almost untouched. 
// So if you make a mistake in your query, you are likely to cause the whole
// transaction to return an error. 
// NOTE: This is different thant URL terms. These are query terms sent along
// to the *single* URL term "ql". 
// Note: This is an oblique-usage function. You will find all the ql operations
// supported in the various addRequiredXXXX functions above. You would only use
// this function if you already have ql strings prepared for some reason, or if
// there are new ql format operations that are not supported by this API.
-(void)addRequirement: (NSString *)requirement;

// returns the URL-ready string that detailes all specified requirements.
// This is used internally by UGClient, you don't need to call it.
-(NSString *)getURLAppend;

@end
