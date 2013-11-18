#import "UGQuery.h"
#import "UGHTTPManager.h"

@implementation UGQuery
{
    NSMutableArray *m_requirements;
    NSMutableString *m_urlTerms;
}

-(id)init
{
    self = [super init];
    if ( self )
    {
        m_requirements = [NSMutableArray new];
        m_urlTerms = [NSMutableString new];
    }
    return self;
}

-(void)setConsumer: (NSString *)consumer
{
    [self addURLTerm:@"consumer" equals:consumer];    
}

-(void)setLastUUID: (NSString *)lastUUID
{
    [self addURLTerm:@"last" equals:lastUUID];    
}

-(void)setTime: (long)time
{
    NSMutableString *str = [NSMutableString new];
    [str appendFormat:@"%ld", time];
    [self addURLTerm:@"time" equals:str];
}

-(void)setPrev: (int)prev
{
    NSMutableString *str = [NSMutableString new];
    [str appendFormat:@"%d", prev];
    [self addURLTerm:@"prev" equals:str];
}

-(void)setNext: (int)next
{
    NSMutableString *str = [NSMutableString new];
    [str appendFormat:@"%d", next];
    [self addURLTerm:@"next" equals:str];
}

-(void)setLimit: (int)limit
{
    NSMutableString *str = [NSMutableString new];
    [str appendFormat:@"%d", limit];
    [self addURLTerm:@"limit" equals:str];
}

-(void)setPos: (NSString *)pos
{
    [self addURLTerm:@"pos" equals:pos];    
}

-(void)setUpdate: (BOOL)update
{
    if ( update )
    {
        [self addURLTerm:@"update" equals:@"true"];
    }
    else
    {
        [self addURLTerm:@"update" equals:@"false"];
    }
}

-(void)setSynchronized: (BOOL)synchronized
{
    if ( synchronized )
    {
        [self addURLTerm:@"synchronized" equals:@"true"];
    }
    else
    {
        [self addURLTerm:@"synchronized" equals:@"false"];
    }
}

-(void)addURLTerm: (NSString *)urlTerm equals:(NSString *)equals
{
    // ignore anything with a nil
    if ( !urlTerm ) return;
    if ( !equals ) return;

    // escape the strings
    NSString *escapedUrlTerm = [UGHTTPManager escapeSpecials:urlTerm];
    NSString *escapedEquals = [UGHTTPManager escapeSpecials:equals];

    // add it in
    if ( [m_urlTerms length] > 0 )
    {
        // we already have some terms. Append an & before continuing
        [m_urlTerms appendFormat:@"&"];
    }
    [m_urlTerms appendFormat:@"%@=%@", escapedUrlTerm, escapedEquals];
}

-(void)addRequiredOperation: (NSString *)term op:(int)op valueStr:(NSString *)valueStr
{
    // disregard invalid values
    if ( !term ) return;
    if ( !valueStr ) return;
    
    NSString *opStr = [self getOpStr: op];
    if ( !opStr ) return; // nil opStr means they sent in an invalid op code
    
    // assemble the requirement string
    NSMutableString *assembled = [NSMutableString new];
    [assembled appendFormat:@"%@ %@ '%@'", term, opStr, valueStr];
    
    // add it as a req
    [self addRequirement:assembled];
}

-(void)addRequiredOperation: (NSString *)term op:(int)op valueInt:(int) valueInt
{
    // disregard invalid values
    if ( !term ) return;
    
    NSString *opStr = [self getOpStr: op];
    if ( !opStr ) return; // nil opStr means they sent in an invalid op code
    
    // assemble the requirement string
    NSMutableString *assembled = [NSMutableString new];
    [assembled appendFormat:@"%@ %@ %d", term, opStr, valueInt];
    
    // add it as a req
    [self addRequirement:assembled];
}

-(void)addRequiredContains: (NSString *)term value:(NSString *)value
{
    // disregard invalid values
    if ( !term ) return;
    if ( !value ) return;
    
    // assemble the requirement string
    NSMutableString *assembled = [NSMutableString new];
    [assembled appendFormat:@"%@ contains '%@'", term, value];
    
    // add it as a req
    [self addRequirement:assembled];
}

-(void)addRequiredIn:(NSString *)term low:(int)low high:(int)high
{
    // disregard invalid values
    if ( !term ) return;
    
    // assemble the requirement string
    NSMutableString *assembled = [NSMutableString new];
    [assembled appendFormat:@"%@ in %d,%d", term, low, high];
    
    // add it as a req
    [self addRequirement:assembled];    
}

-(void)addRequiredWithin:(NSString *)term latitude:(float)latitude longitude:(float)longitude distance:(float)distance;
{
    // disregard invalid values
    if ( !term ) return;
    
    // assemble the requirement string
    NSMutableString *assembled = [NSMutableString new];
    [assembled appendFormat:@"%@ within %f of %f,%f", term, distance, latitude, longitude];
    
    // add it as a req
    [self addRequirement:assembled];   
}

-(void)addRequiredWithinLocation:(NSString *)term location:(CLLocation *)location distance:(float)distance
{
    [self addRequiredWithin:term latitude:location.coordinate.latitude longitude:location.coordinate.longitude distance:distance];
}

-(void)addRequirement: (NSString *)requirement
{
    // add the URL-ready requirement to our list
    [m_requirements addObject:requirement];
}

-(NSString *)getURLAppend
{    
    // assemble a url append for all the requirements
    // prep a mutable string
    NSMutableString *ret = [NSMutableString new];
    [ret setString:@"?"];
  
    // true if we've put anything in the string yet.
    BOOL bHasContent = NO;
    
    // start with the ql term
    if ( [m_requirements count] > 0 )
    {    
        // if we're here, there are queries
        // assemble a single string for the ql
        NSMutableString *ql = [NSMutableString new];
        for ( int i=0 ; i<[m_requirements count] ; i++ )
        {
            if ( i>0 )
            {
                // connect terms
                [ql appendFormat:@" and "];
            }
            [ql appendFormat:@"%@", [m_requirements objectAtIndex:i]];
        }
        
        // escape it
        NSString *escapedQL = [UGHTTPManager escapeSpecials:ql];
        [ret appendFormat:@"ql=%@", escapedQL];
        bHasContent = YES;
    }

    if ( [m_urlTerms length] > 0 )
    {
        if ( bHasContent ) 
        {
            [ret appendFormat:@"&%@", m_urlTerms];
        }
        else 
        {
            [ret appendFormat:@"%@", m_urlTerms];
        }
        bHasContent = YES;
    }
    
    if ( !bHasContent )
    {
        // no content
        return @"";
    }
    
    // all prepared
    return ret;
}

// Internal function
 -(NSString *)getOpStr:(int)op
{
    switch (op)
    {
        case kUGQueryOperationEquals: return @"=";
        case kUGQueryOperationGreaterThan: return @">";
        case kUGQueryOperationGreaterThanOrEqualTo: return @">=";
        case kUGQueryOperationLessThan: return @"<";
        case kUGQueryOperationLessThanOrEqualTo: return @"<=";
    }
    return nil;
}

@end
