#import "UGMultiStepAction.h"

@implementation UGMultiStepAction

@synthesize transactionID;
@synthesize nextAction;
@synthesize userID;
@synthesize groupID;
@synthesize activity;
@synthesize outwardTransactionID;
@synthesize reportToClient;


-(id)init
{
    self = [super init];
    if ( self )
    {
        reportToClient = NO;
    }
    return self;
}
@end
