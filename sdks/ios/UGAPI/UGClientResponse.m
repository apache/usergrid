#import "UGClientResponse.h"

@implementation UGClientResponse

@synthesize transactionID;
@synthesize transactionState;
@synthesize response;
@synthesize rawResponse;

-(id)init
{
    self = [super init];
    if ( self )
    {
        transactionID = -1;
        transactionState = -1;
        response = nil;
        rawResponse = nil;
    }
    return self;
}

@end
