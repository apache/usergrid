#import "UGClient.h"
#import "UGHTTPManager.h"
#import "SBJson.h"
#import "UGMultiStepAction.h"

NSString *g_deviceUUID = nil;

@implementation UGClient
{
    // the delegate for asynch callbacks
    id m_delegate;
    
    // the mutex to protect the delegate variable
    NSRecursiveLock *m_delegateLock;
    
    // a growing array of UGHTTPManager instances. See
    // "HTTPMANAGER POOLING" further down in this file.
    NSMutableArray *m_httpManagerPool;
    
    // the base URL for the service
    NSString *m_baseURL;
    
    // the appID for the specific app
    NSString *m_appID;
    
    // the appID for the specific app
    NSString *m_orgID;
    
    // the cached auth token
    UGUser *m_loggedInUser;
    
    // the auth code
    NSString *m_auth;
    
    // the list of currently pending multi-step actions
    NSMutableArray *m_pendingMultiStepActions;
    
    // logging state
    BOOL m_bLogging;
}

/************************** ACCESSORS *******************************/
/************************** ACCESSORS *******************************/
/************************** ACCESSORS *******************************/
+(NSString *) version
{
    return @"0.1.1";
}

-(NSString *)getAccessToken
{
    return m_auth;
}

-(UGUser *)getLoggedInUser
{
    return m_loggedInUser;
}

-(id) getDelegate
{
    return m_delegate;
}

/******************************* INIT *************************************/
/******************************* INIT *************************************/
/******************************* INIT *************************************/
-(id)init
{
    // you are not allowed to init without an organization id and application id
    // you can't init with [UGClient new]. You must call
    // [[UGClient alloc] initWithOrganizationId: <your UG org id> withApplicationId:<your UG app id>]
    assert(0); 
    return nil;
}

-(id) initWithOrganizationId: (NSString *)organizationID withApplicationID:(NSString *)applicationID
{
    self = [super init];
    if ( self )
    {
        m_delegate = nil;
        m_httpManagerPool = [NSMutableArray new];
        m_delegateLock = [NSRecursiveLock new];
        m_appID = applicationID;
        m_orgID = organizationID;
        m_baseURL = @"http://api.usergrid.com";
        m_pendingMultiStepActions = [NSMutableArray new];
        m_loggedInUser = nil;
        m_bLogging = NO;
    }
    return self;
}

-(id) initWithOrganizationId: (NSString *)organizationID withApplicationID:(NSString *)applicationID baseURL:(NSString *)baseURL
{
    self = [super init];
    if ( self )
    {
        m_delegate = nil;
        m_httpManagerPool = [NSMutableArray new];
        m_delegateLock = [NSRecursiveLock new];
        m_appID = applicationID;
        m_orgID = organizationID;
        m_baseURL = baseURL;
    }
    return self;
}

-(BOOL) setDelegate:(id)delegate
{
    // first off, clear any pending transactions
    for ( int i=0 ; i<[m_httpManagerPool count] ; i++ )
    {
        UGHTTPManager *mgr = [m_httpManagerPool objectAtIndex:i];
        
        // it's safe to call cancel at all times.
        [mgr cancel];
    }
    
    // nil is a valid answer. It means we're synchronous now.
    if ( delegate == nil )
    {
        [m_delegateLock lock];
        m_delegate = nil;
        [m_delegateLock unlock];
        return YES;
    }
    
    // if it's not nil, it has to have the delegation function
    if ( ![delegate respondsToSelector:@selector(ugClientResponse:)] )
    {
        return NO;
    }
    
    // if we're here, it means the delegate is valid
    [m_delegateLock lock];
    m_delegate = delegate;
    [m_delegateLock unlock];
    return YES;
}

/************************* HTTPMANAGER POOLING *******************************/
/************************* HTTPMANAGER POOLING *******************************/
/************************* HTTPMANAGER POOLING *******************************/

// any given instance of UGHTTPManager can only manage one transaction at a time,
// but we want the client to be able to have as many going at once as he likes. 
// so we have a pool of UGHTTPManagers as needed.
-(UGHTTPManager *)getHTTPManager;
{
    // find the first unused HTTPManager
    for ( int i=0 ; i<[m_httpManagerPool count] ; i++ )
    {
        UGHTTPManager *mgr = [m_httpManagerPool objectAtIndex:i];
        if ( [mgr isAvailable] )
        {
            // tag this guy as available
            [mgr setAvailable:NO];
            
            // return him
            return mgr;
        }
    }
    
    // if we're here, we didn't find any available managers
    // so we'll need to make a new one
    UGHTTPManager *newMgr = [UGHTTPManager new];
    
    // mark it as in-use (we're about to return it)
    [newMgr setAvailable:NO];
    
    // tell it the auth to use
    [newMgr setAuth:m_auth];
    
    // add it to the array
    [m_httpManagerPool addObject:newMgr];
    
    // return it
    return newMgr;
}

-(void)releaseHTTPManager:(UGHTTPManager *)toRelease
{
    [toRelease setAvailable:YES];
}

-(void)setAuth:(NSString *)auth
{
    // note the auth for ourselves
    m_auth = auth;
    
    // update all our managers
    for ( int i=0 ; i<[m_httpManagerPool count] ; i++ )
    {
        UGHTTPManager *mgr = [m_httpManagerPool objectAtIndex:i];
        [mgr setAuth:m_auth];
    }
}

/************************* GENERAL WORKHORSES *******************************/
/************************* GENERAL WORKHORSES *******************************/
/************************* GENERAL WORKHORSES *******************************/
// url: the URL to hit
// op: a kUGHTTP constant. Example: kUGHTTPPost
// opData: The data to send along with the operation. Can be nil
-(UGClientResponse *)httpTransaction:(NSString *)url op:(int)op opData:(NSString *)opData
{
    // get an http manager to do this transaction
    UGHTTPManager *mgr = [self getHTTPManager];
    
    if ( m_delegate )
    {
        if ( m_bLogging )
        {
            NSLog(@">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            NSLog(@"Asynch outgoing call: '%@'", url);
        }
        
        // asynch transaction
        int transactionID = [mgr asyncTransaction:url operation:op operationData:opData delegate:self];
        
        if ( m_bLogging )
        {
            NSLog(@"Transaction ID:%d", transactionID);
            NSLog(@">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n\n");
        }
        
        if ( transactionID == -1 )
        {
            if ( m_bLogging )
            {
                NSLog(@"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                NSLog(@"Response: ERROR: %@", [mgr getLastError]);
                NSLog(@"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n\n");
            }
            
            // there was an immediate failure in the transaction
            UGClientResponse *response = [UGClientResponse new];
            [response setTransactionID:-1];
            [response setTransactionState:kUGClientResponseFailure];
            [response setResponse:[mgr getLastError]];
            [response setRawResponse:nil];
            return response;
        }
        else 
        {
            // the transaction is in progress and pending
            UGClientResponse *response = [UGClientResponse new];
            [response setTransactionID:transactionID];
            [response setTransactionState:kUGClientResponsePending];
            [response setResponse:nil];
            [response setRawResponse:nil];
            return response;
        }
    }
    else 
    {
        if ( m_bLogging )
        {
            NSLog(@">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            NSLog(@"Synch outgoing call: '%@'", url);
            NSLog(@">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n\n");
        }
        
        // synch transaction
        NSString *result = [mgr syncTransaction:url operation:op operationData:opData];
        
        if ( m_bLogging )
        {
            NSLog(@"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            if ( result )
            {
                NSLog(@"Response:\n%@", result);
            }
            else
            {
                NSLog(@"Response: ERROR: %@", [mgr getLastError]);
            }
            NSLog(@"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n\n");
        }
        
        // since we're doing a synch transaction, we are now done with this manager.
        [self releaseHTTPManager:mgr];
        
        if ( result )
        {
            // got a valid result
            UGClientResponse *response = [self createResponse:-1 jsonStr:result];
            return response;
        }
        else 
        {
            // there was an error. Note the failure state, set the response to 
            // be the error string
            UGClientResponse *response = [UGClientResponse new];
            [response setTransactionID:-1];
            [response setTransactionState:kUGClientResponseFailure];
            [response setResponse:[mgr getLastError]];
            [response setRawResponse:nil];
            return response;
        }
    }
}

-(UGClientResponse *)createResponse:(int)transactionID jsonStr:(NSString *)jsonStr
{
    UGClientResponse *response = [UGClientResponse new];
    
    // set the raw response and transaction id
    [response setRawResponse:jsonStr];
    [response setTransactionID:transactionID];
    
    // parse the json
    SBJsonParser *parser = [SBJsonParser new];
    NSError *error;
    id result = [parser objectWithString:jsonStr error:&error];
    
    if ( result )
    {
        // first off, if the result is NOT an NSDictionary, something went wrong.
        // there should never be an array response
        if ( ![result isKindOfClass:[NSDictionary class]] )
        {
            [response setTransactionState:kUGClientResponseFailure];
            [response setResponse:@"Internal error: Response parsed to something other than NSDictionary"];
            return response;
        }
        
        // it successfully parsed. Though the result might still be an error.
        // it could be the server returning an error in perfectly formated json.
        NSString *err = [result valueForKey:@"error"];
        if ( err )
        {
            // there was an error. See if there's a more detailed description.
            // if there is, we'll use that. If not, we'll use the error value
            // itself.
            NSString *errDesc = [result valueForKey:@"error_description"];
            NSString *toReport = errDesc;
            if ( !toReport ) toReport = err;
            
            [response setTransactionState:kUGClientResponseFailure];
            [response setResponse:toReport];
            return response;
        }
        
        // if we're here we have a good auth. make note of it
        NSString *auth = [result valueForKey:@"access_token"];
        if ( auth )
        {
            [self setAuth: auth];
            
            // if there's an access token, there might be a user
            NSDictionary *dict = [result objectForKey:@"user"];
            if ( dict )
            {
                // get the fields for the user
                m_loggedInUser = [UGUser new];
                [m_loggedInUser setUsername:[dict valueForKey:@"username"]];
                [m_loggedInUser setUuid:[dict valueForKey:@"uuid"]];
                [m_loggedInUser setEmail:[dict valueForKey:@"email"]];
                [m_loggedInUser setPicture:[dict valueForKey:@"picture"]];
            }
        }
        
        [response setTransactionState:kUGClientResponseSuccess];
        [response setResponse:result];
        return response;
    }
    else
    {
        // there was an error during json parsing. 
        [response setTransactionState:kUGClientResponseFailure];
        [response setResponse:[error localizedDescription]];
        return response;
    }
}

// basic URL assembly functions. For convenience
-(NSMutableString *)createURL:(NSString *)append1
{
    NSMutableString *ret = [NSMutableString new];
    [ret appendFormat:@"%@/%@/%@/%@", m_baseURL, m_orgID, m_appID, append1];
    return ret;
}

-(NSMutableString *)createURL:(NSString *)append1 append2:(NSString *)append2
{
    NSMutableString *ret = [NSMutableString new];
    [ret appendFormat:@"%@/%@/%@/%@/%@", m_baseURL, m_orgID, m_appID, append1, append2];
    return ret;
}

-(NSMutableString *)createURL:(NSString *)append1 append2:(NSString *)append2 append3:(NSString *)append3
{
    NSMutableString *ret = [NSMutableString new];
    [ret appendFormat:@"%@/%@/%@/%@/%@/%@", m_baseURL, m_orgID, m_appID, append1, append2, append3];
    return ret;
}

-(NSMutableString *)createURL:(NSString *)append1 append2:(NSString *)append2 append3:(NSString *)append3 append4:(NSString *)append4
{
    NSMutableString *ret = [NSMutableString new];
    [ret appendFormat:@"%@/%@/%@/%@/%@/%@/%@", m_baseURL, m_orgID, m_appID, append1, append2, append3, append4];
    return ret;
}

-(NSMutableString *)createURL:(NSString *)append1 append2:(NSString *)append2 append3:(NSString *)append3 append4:(NSString *)append4 append5:(NSString *)append5
{
    NSMutableString *ret = [NSMutableString new];
    [ret appendFormat:@"%@/%@/%@/%@/%@/%@/%@/%@", m_baseURL, m_orgID, m_appID, append1, append2, append3, append4, append5];
    return ret;
}

-(void)appendQueryToURL:(NSMutableString *)url query:(UGQuery *)query
{
    if ( query )
    {
        [url appendFormat:@"%@", [query getURLAppend]];
    }
}

-(NSString *)createJSON:(NSDictionary *)data
{
    NSString *ret = [self createJSON:data error:nil];

    // the only way for ret to be nil here is for an internal
    // function to have a bug.
    assert(ret);
    return ret;
}

-(NSString *)createJSON:(NSDictionary *)data error:(NSString **)error
{
    SBJsonWriter *writer = [SBJsonWriter new];
    NSError *jsonError;
    NSString *jsonStr = [writer stringWithObject:data error:&jsonError];

    if ( jsonStr )
    {
        return jsonStr;
    }
    
    // if we're here, there was an assembly error
    if ( error )
    {
        *error = [jsonError localizedDescription];
    }
    return nil;
}

/************************** UGHTTPMANAGER DELEGATES *******************************/
/************************** UGHTTPMANAGER DELEGATES *******************************/
/************************** UGHTTPMANAGER DELEGATES *******************************/
-(void)httpManagerError:(UGHTTPManager *)manager error:(NSString *)error
{
    // prep an error response
    UGClientResponse *response = [UGClientResponse new];
    [response setTransactionID:[manager getTransactionID]];
    [response setTransactionState:kUGClientResponseFailure];
    [response setResponse:error];
    [response setRawResponse:nil];

    if ( m_bLogging )
    {
        NSLog(@"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        NSLog(@"Response: ERROR: %@", error);
        NSLog(@"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n\n");
    }
    
    // fire it off. Wrap in mutex locks to ensure we don't get 
    // race conditions that cause us to fire it off to Mr. Nil.
    [m_delegateLock lock];
    if ( m_delegate )
    {
        [m_delegate performSelector:@selector(ugClientResponse:) withObject:response];
    }
    [m_delegateLock unlock];
    
    // now that the callback is complete, it's safe to release this manager
    [self releaseHTTPManager:manager];
}

-(void)httpManagerResponse:(UGHTTPManager *)manager response:(NSString *)response
{
    if ( m_bLogging )
    {
        NSLog(@"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        NSLog(@"Response (Transaction ID %d):\n%@", [manager getTransactionID], response);
        NSLog(@"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n\n");
    }
    
    // form up the response
    UGClientResponse *ugResponse = [self createResponse:[manager getTransactionID] jsonStr:response];
    
    // if this is part of a multi-step call, we press on.
    for ( int i=0 ; i<[m_pendingMultiStepActions count] ; i++ )
    {
        UGMultiStepAction *action = [m_pendingMultiStepActions objectAtIndex:i];
        if ( [action transactionID] == [ugResponse transactionID] )
        {
            // multi-step call. Fire off the action.
            ugResponse = [self doMultiStepAction:action mostRecentResponse:ugResponse];
            if ( ![action reportToClient] )
            {
                // the action is still pending. We do not report this
                // to the user. We're done with the httpmanager we were using,
                // though. 
                [self releaseHTTPManager:manager];
                return;
            }
            
            // when the action is complete, we want to immediately break
            // from this loop, then fall through to the normal reporting
            // to the user. 
            break;
        }
    }
        
    // fire it off
    [m_delegateLock lock];
    if ( m_delegate )
    {
        [m_delegate performSelector:@selector(ugClientResponse:) withObject:ugResponse];
    }
    [m_delegateLock unlock];   
    
    // now that the callback is complete, it's safe to release this manager
    [self releaseHTTPManager:manager];
}

// multi-step follow-up function
-(UGClientResponse *)multiStepAction: (UGMultiStepAction *)action
{
    // different behavior if synch or asynch
    if ( m_delegate )
    {
        // asynch. Fire it off and we're done
        return [self doMultiStepAction:action mostRecentResponse:nil];
    }
    else 
    {
        // synchronous. keep calling until it finished or fails
        UGClientResponse *response = nil;
        do 
        {
            response = [self doMultiStepAction:action mostRecentResponse:response];
            if ( [action reportToClient] )
            {
                // done
                return response;
            }
        } while ([response transactionState] == kUGClientResponseSuccess);
        
        // if we're here, there was an error
        return response;
    }
}

-(UGClientResponse *)doMultiStepAction: (UGMultiStepAction *)action mostRecentResponse:(UGClientResponse *)mostRecentResponse
{
    // clear the pending array of this object
    [m_pendingMultiStepActions removeObject:action];

    // assume we aren't reporting to the client
    [action setReportToClient:NO];
    
    if ( mostRecentResponse )
    {
        // we don't care about pending responses
        if ( [mostRecentResponse transactionState] == kUGClientResponsePending )
        {
            // put ourselves back in the list
            [m_pendingMultiStepActions addObject:action];
            return mostRecentResponse;
        }
        
        // any failure is an immediate game ender
        if ( [mostRecentResponse transactionState] == kUGClientResponseFailure )
        {
            [mostRecentResponse setTransactionID:[action transactionID]];
            return mostRecentResponse;
        }
    }
    
    // if mostRecentRespons is nil, that means it's the first call to initiate 
    // the chain. So we continue on with processing.

    // so either we are reacting to a success or we are starting off the chain
    UGClientResponse *result = nil; 
    if ( [action nextAction] == kMultiStepCreateActivity )
    {
        // create the activity
        result = [self createActivity:[action activity]];
        
        // advance ourselves to the next step
        [action setNextAction:kMultiStepPostActivity];
    }
    else if ( [action nextAction] == kMultiStepCreateGroupActivity )
    {
        // create the activity
        result = [self createActivity:[action activity]];
        
        // advance ourselves to the next step
        [action setNextAction:kMultiStepPostGroupActivity];
    }
    else if ( [action nextAction] == kMultiStepPostActivity )
    {
        // we just created an activity, now we need to associate it with a user.
        // first, we'll need the activity's uuid
        NSDictionary *dict = [mostRecentResponse response]; // dictionary for the response
        NSArray *entities = [dict objectForKey:@"entities"]; // array for the entities
        NSDictionary *activity = [entities objectAtIndex:0]; // dict for the activity
        NSString *activityUUID = [activity valueForKey:@"uuid"]; // and finally the uuid string
        
        // fire off the next step
        result = [self postUserActivityByUUID:[action userID] activity:activityUUID];
        
        // advance the action
        [action setNextAction:kMultiStepCleanup];
    }
    else if ( [action nextAction] == kMultiStepPostGroupActivity )
    {
        // we just created an activity, now we need to associate it with a user.
        // first, we'll need the activity's uuid
        NSDictionary *dict = [mostRecentResponse response]; // dictionary for the response
        NSArray *entities = [dict objectForKey:@"entities"]; // array for the entities
        NSDictionary *activity = [entities objectAtIndex:0]; // dict for the activity
        NSString *activityUUID = [activity valueForKey:@"uuid"]; // and finally the uuid string
        
        // fire off the next step
        result = [self postGroupActivityByUUID:[action groupID] activity:activityUUID];
        
        // advance the action
        [action setNextAction:kMultiStepCleanup];
    }
    else if ( [action nextAction] == kMultiStepCleanup )
    {
        // all we do in cleanup is update the transaction ID of the 
        // response that was sent in. We do this to ensure that the transaction
        // id is constant across the entire transaction
        result = mostRecentResponse;
        [result setTransactionID:[action outwardTransactionID]];
        [action setReportToClient:YES];
    }
    
    if ( !mostRecentResponse )
    {
        // if mostRecentResponse is nil, it means we're on the first step. That means
        // we need to adopt a unique outward transaction ID. We'll simply use
        // the ID given back by the first transaction in the chain. This also means
        // we can simply return the first transaction pending response without modification. 
        [action setOutwardTransactionID:[result transactionID]];
    }

    // wherever we landed, if it's a pending transaction, the action needs to
    // know that transaction ID. Also, we need to go in to the pending array
    if ( [result transactionState] == kUGClientResponsePending )
    {
        [action setTransactionID:[result transactionID]];
        [m_pendingMultiStepActions addObject:action];
    }
    
    // result is now properly set up and ready to be handed to the user. 
    return result;
}

/*************************** LOGIN / LOGOUT ****************************/
/*************************** LOGIN / LOGOUT ****************************/
/*************************** LOGIN / LOGOUT ****************************/
-(UGClientResponse *)logInUser: (NSString *)userName password:(NSString *)password
{
    return [self logIn:@"password" userKey:@"username" userValue:userName pwdKey:@"password" pwdValue:password];
}

-(UGClientResponse *)logInUserWithPin: (NSString *)userName pin:(NSString *)pin
{
    return [self logIn:@"pin" userKey:@"username" userValue:userName pwdKey:@"pin" pwdValue:pin];
}

-(UGClientResponse *)logInUserWithFacebook: (NSString *)facebookToken
{
    NSMutableString *url = [self createURL:@"auth/facebook"];
    UGQuery *query = [[UGQuery alloc] init];
    [query addURLTerm:@"fb_access_token" equals:facebookToken];
    [self appendQueryToURL:url query:query];
    return [self httpTransaction:url op:kUGHTTPGet opData:nil];
}

-(UGClientResponse *)logInAdmin: (NSString *)adminUserName secret:(NSString *)adminSecret
{
    return [self logIn:@"client_credentials" userKey:@"client_id" userValue:adminUserName pwdKey:@"client_secret" pwdValue:adminSecret];
}

-(void)logOut
{
    // clear out auth
    [self setAuth: nil];
}

// general workhorse for auth logins
-(UGClientResponse *)logIn:(NSString *)grantType userKey:(NSString *)userKey userValue:(NSString *)userValue pwdKey:(NSString *)pwdKey pwdValue:(NSString *)pwdValue
{
    // create the URL
    NSString *url = [self createURL:@"token"];
    
    // because it's read as form data, we need to escape special characters.
    NSString *escapedUserValue = [UGHTTPManager escapeSpecials:userValue];
    NSString *escapedPwdValue = [UGHTTPManager escapeSpecials:pwdValue];
    
    // create the post data. For auth functions, we don't use json,
    // but instead use web form style data
    NSMutableString *postData = [NSMutableString new];
    [postData appendFormat:@"grant_type=%@&%@=%@&%@=%@", grantType, userKey, escapedUserValue, pwdKey, escapedPwdValue];
    
    // fire off the request
    return [self httpTransaction:url op:kUGHTTPPostAuth opData:postData];    
}
/*************************** USER MANAGEMENT ***************************/
/*************************** USER MANAGEMENT ***************************/
/*************************** USER MANAGEMENT ***************************/
-(UGClientResponse *)addUser:(NSString *)username email:(NSString *)email name:(NSString *)name password:(NSString *)password
{
    // make the URL we'll be posting to
    NSString *url = [self createURL:@"users"];
    
    // make the post data we'll be sending along with it.
    NSMutableDictionary *toPost = [NSMutableDictionary new];
    [toPost setObject:username forKey:@"username"];
    [toPost setObject:name forKey:@"name"];
    [toPost setObject:email forKey:@"email"];
    [toPost setObject:password forKey:@"password"];
    NSString *toPostStr = [self createJSON:toPost];
    
    // fire it off
    return [self httpTransaction:url op:kUGHTTPPost opData:toPostStr];
}

// updates a user's password
-(UGClientResponse *)updateUserPassword:(NSString *)usernameOrEmail oldPassword:(NSString *)oldPassword newPassword:(NSString *)newPassword
{
    // make the URL we'll be posting to
    NSString *url = [self createURL:@"users" append2:usernameOrEmail append3:@"password"];
    
    // make the post data we'll be sending along with it.
    NSMutableDictionary *toPost = [NSMutableDictionary new];
    [toPost setObject:oldPassword forKey:@"oldpassword"];
    [toPost setObject:newPassword forKey:@"newpassword"];
    NSString *toPostStr = [self createJSON:toPost];
    
    // fire it off
    return [self httpTransaction:url op:kUGHTTPPost opData:toPostStr];
}

-(UGClientResponse *)getGroupsForUser: (NSString *)userID;
{
    // make the URL, and fire off the get
    NSString *url = [self createURL:@"users" append2:userID append3:@"groups"];
    return [self httpTransaction:url op:kUGHTTPGet opData:nil];
}

-(UGClientResponse *)getUsers: (UGQuery *)query
{
    // create the URL
    NSMutableString *url = [self createURL:@"users"];
    [self appendQueryToURL:url query:query];
    return [self httpTransaction:url op:kUGHTTPGet opData:nil];
}

/************************** ACTIVITY MANAGEMENT **************************/
/************************** ACTIVITY MANAGEMENT **************************/
/************************** ACTIVITY MANAGEMENT **************************/
-(UGClientResponse *)createActivity: (NSDictionary *)activity
{
    // make the URL
    NSString *url = [self createURL:@"activity"];
    
    // get the json to send.
    // we have to json-ify a dictionary that was sent
    // in by the client. So naturally, we can't just trust it 
    // to work. Therefore we can't use our internal convenience 
    // function for making the json. We go straight to SBJson, so
    // we can identify and report any errors.
    SBJsonWriter *writer = [SBJsonWriter new];
    NSError *jsonError;
    NSString *toPostStr = [writer stringWithObject:activity error:&jsonError];

    if ( !toPostStr )
    {
        // error during json assembly
        UGClientResponse *ret = [UGClientResponse new];
        [ret setTransactionState:kUGClientResponseFailure];
        [ret setTransactionID:-1];
        [ret setResponse:[jsonError localizedDescription]];
        [ret setRawResponse:nil];
        return ret;
    }
    
    // fire it off
    return [self httpTransaction:url op:kUGHTTPPost opData:toPostStr];
}

// create an activity and post it to a user in a single step
-(UGClientResponse *)postUserActivity: (NSString *)userID activity:(NSDictionary *)activity
{
    // prep a multi-step action
    UGMultiStepAction *action = [UGMultiStepAction new];
    
    // set it up to start the create activity / post to user chain
    [action setNextAction:kMultiStepCreateActivity];
    [action setUserID:userID];
    [action setActivity:activity];
    
    // fire it off
    return [self multiStepAction:action];
}

-(UGClientResponse *)postUserActivityByUUID: (NSString *)userID activity:(NSString *)activityUUID
{
    // make the URL and fire off the post. there is no data
    NSString *url = [self createURL:@"users" append2:userID append3:@"activities" append4:activityUUID];
    return [self httpTransaction:url op:kUGHTTPPost opData:nil];
}

-(UGClientResponse *)postGroupActivity:(NSString *)groupID activity:(NSDictionary *)activity
{
    // prep a multi-step action
    UGMultiStepAction *action = [UGMultiStepAction new];
    
    // set it up to start the create activity / post to user chain
    [action setNextAction:kMultiStepCreateGroupActivity];
    [action setGroupID:groupID];
    [action setActivity:activity];
    
    // fire it off
    return [self multiStepAction:action];    
}

-(UGClientResponse *)postGroupActivityByUUID: (NSString *)groupID activity:(NSString *)activityUUID
{
    // make the URL and fire off the post. there is no data
    NSString *url = [self createURL:@"groups" append2:groupID append3:@"activities" append4:activityUUID];
    return [self httpTransaction:url op:kUGHTTPPost opData:nil];    
}

-(UGClientResponse *)getActivitiesForUser: (NSString *)userID query:(UGQuery *)query
{
    NSMutableString *url = [self createURL:@"users" append2:userID append3:@"activities"];
    [self appendQueryToURL:url query:query];
    return [self httpTransaction:url op:kUGHTTPGet opData:nil];    
}

-(UGClientResponse *)getActivityFeedForUser: (NSString *)userID query:(UGQuery *)query
{
    NSMutableString *url = [self createURL:@"users" append2:userID append3:@"feed"];
    [self appendQueryToURL:url query:query];
    return [self httpTransaction:url op:kUGHTTPGet opData:nil];    
}

-(UGClientResponse *)getActivitiesForGroup: (NSString *)groupID query:(UGQuery *)query
{
    NSMutableString *url = [self createURL:@"groups" append2:groupID append3:@"activities"];
    [self appendQueryToURL:url query:query];
    return [self httpTransaction:url op:kUGHTTPGet opData:nil]; 
}

-(UGClientResponse *)getActivityFeedForGroup: (NSString *)groupID query:(UGQuery *)query
{
    NSMutableString *url = [self createURL:@"groups" append2:groupID append3:@"feed"];
    [self appendQueryToURL:url query:query];
    return [self httpTransaction:url op:kUGHTTPGet opData:nil]; 
}

-(UGClientResponse *)removeActivity:(NSString *)activityUUID
{
    NSString *url = [self createURL:@"activities" append2:activityUUID];
    return [self httpTransaction:url op:kUGHTTPDelete opData:nil];
}

/************************** GROUP MANAGEMENT **************************/
/************************** GROUP MANAGEMENT **************************/
/************************** GROUP MANAGEMENT **************************/
-(UGClientResponse *)createGroup:(NSString *)groupPath groupTitle:(NSString *)groupTitle
{
    // make the URL
    NSString *url = [self createURL:@"groups"];
    
    // make the post data we'll be sending along with it.
    NSMutableDictionary *toPost = [NSMutableDictionary new];
    [toPost setObject:groupPath forKey:@"path"];
    if ( groupTitle )
    {
        [toPost setObject:groupTitle forKey:@"title"];
    }
    NSString *toPostStr = [self createJSON:toPost];
    
    // fire it off
    return [self httpTransaction:url op:kUGHTTPPost opData:toPostStr];
}

-(UGClientResponse *)addUserToGroup:(NSString *)userID group:(NSString *)groupID
{
    // make the URL
    NSString *url = [self createURL:@"groups" append2:groupID append3:@"users" append4:userID];
    
    // fire it off. This is a data-less POST
    return [self httpTransaction:url op:kUGHTTPPost opData:nil];
}

-(UGClientResponse *)removeUserFromGroup:(NSString *)userID group:(NSString *)groupID
{
    // this is identical to addUserToGroup, except we use the DELETE method instead of POST
    // make the URL
    NSString *url = [self createURL:@"groups" append2:groupID append3:@"users" append4:userID];
    
    // fire it off. This is a data-less POST
    return [self httpTransaction:url op:kUGHTTPDelete opData:nil];}

-(UGClientResponse *)getUsersForGroup:(NSString *)groupID query:(UGQuery *)query
{
    // create the URL
    NSMutableString *url = [self createURL:@"groups" append2:groupID append3:@"users"];
    [self appendQueryToURL:url query:query];
    return [self httpTransaction:url op:kUGHTTPGet opData:nil];
}

/************************** ENTITY MANAGEMENT **************************/
/************************** ENTITY MANAGEMENT **************************/
/************************** ENTITY MANAGEMENT **************************/
// jsonify the entity. If there's an error, it creates a UGClientResponse and 
// returns it. If there's no error, it returns nil, and the outJson field and
// the type field will be set correctly.
// yes, it's odd to have a function return nil on success, but it's internal.
-(UGClientResponse *)validateEntity:(NSDictionary *)newEntity outJson:(NSString **)jsonStr outType:(NSString **)type
{
    // validation
    NSString *error = nil;
    
    // the entity must exist
    if ( !newEntity )
    {
        error =@"entity is nil";
    }
    
    // the entity must have a "type" field
    *type = [newEntity valueForKey:@"type"];
    if ( !*type )
    {
        error = @"entity is missing a type field";
    }
    
    // make sure it can parse to a json
    SBJsonWriter *writer = [SBJsonWriter new];
    NSError *jsonError;
    *jsonStr = [writer stringWithObject:newEntity error:&jsonError];
    if ( !*jsonStr )
    {
        error = [jsonError localizedDescription];
    }
    
    // if error got set to anything, it means we failed
    if ( error )
    {
        UGClientResponse *ret = [UGClientResponse new];
        [ret setTransactionState:kUGClientResponseFailure];
        [ret setTransactionID:-1];
        [ret setResponse:error];
        [ret setRawResponse:nil]; 
        return ret;
    }
    
    // if we're here, it's a good json and we're done
    return nil;
}

-(UGClientResponse *)createEntity:(NSDictionary *)newEntity
{
    NSString *jsonStr;
    NSString *type;
    UGClientResponse *errorRet = [self validateEntity:newEntity outJson:&jsonStr outType:&type];
    if ( errorRet ) return errorRet;
    
    // we have a valid entity, ready to post. Make the URL
    NSString *url = [self createURL:type];
    
    // post it
    return [self httpTransaction:url op:kUGHTTPPost opData:jsonStr];
}

-(UGClientResponse *)getEntities: (NSString *)type query:(UGQuery *)query
{
    NSMutableString *url = [self createURL:type];
    [self appendQueryToURL:url query:query];
    return [self httpTransaction:url op:kUGHTTPGet opData:nil];    
}

-(UGClientResponse *)updateEntity: (NSString *)entityID entity:(NSDictionary *)updatedEntity
{
    NSString *jsonStr;
    NSString *type;
    UGClientResponse *errorRet = [self validateEntity:updatedEntity outJson:&jsonStr outType:&type];
    if ( errorRet ) return errorRet;
    
    // we have a valid entity, ready to post. Make the URL
    NSString *url = [self createURL:type append2:entityID];
    
    // post it
    return [self httpTransaction:url op:kUGHTTPPut opData:jsonStr];
}

-(UGClientResponse *)removeEntity: (NSString *)type entityID:(NSString *)entityID
{
    // Make the URL, then fire off the delete
    NSString *url = [self createURL:type append2:entityID];
    return [self httpTransaction:url op:kUGHTTPDelete opData:nil];
}


-(UGClientResponse *)connectEntities: (NSString *)connectorType connectorID:(NSString *)connectorID connectionType:(NSString *)connectionType connecteeType:(NSString *)connecteeType connecteeID:(NSString *)connecteeID
{
    NSString *url = [self createURL:connectorType append2:connectorID append3:connectionType append4:connecteeType append5:connecteeID];
    return [self httpTransaction:url op:kUGHTTPPost opData:nil];
}

-(UGClientResponse *)connectEntities: (NSString *)connectorType connectorID:(NSString *)connectorID type:(NSString *)connectionType connecteeID:(NSString *)connecteeID
{
    NSString *url = [self createURL:connectorType append2:connectorID append3:connectionType append4:connecteeID];
    return [self httpTransaction:url op:kUGHTTPPost opData:nil];
}

-(UGClientResponse *)disconnectEntities: (NSString *)connectorType connectorID:(NSString *)connectorID type:(NSString *)connectionType connecteeID:(NSString *)connecteeID
{
    NSString *url = [self createURL:connectorType append2:connectorID append3:connectionType append4:connecteeID];
    return [self httpTransaction:url op:kUGHTTPDelete opData:nil];
}

-(UGClientResponse *)getEntityConnections: (NSString *)connectorType connectorID:(NSString *)connectorID connectionType:(NSString *)connectionType query:(UGQuery *)query
{
    NSMutableString *url = [self createURL:connectorType append2:connectorID append3:connectionType];
    [self appendQueryToURL:url query:query];
    return [self httpTransaction:url op:kUGHTTPPost opData:nil];
}

/************************** MESSAGE MANAGEMENT **************************/
/************************** MESSAGE MANAGEMENT **************************/
/************************** MESSAGE MANAGEMENT **************************/
-(UGClientResponse *)postMessage: (NSString *)queuePath message:(NSDictionary *)message
{
    // because the NSDictionary is from the client, we can't trust it. We need 
    // to go through full error checking
    NSString *error;
    NSString *jsonStr = [self createJSON:message error:&error];
    
    if ( !jsonStr )
    {
        // report the error
        UGClientResponse *ret = [UGClientResponse new];
        [ret setTransactionID:-1];
        [ret setTransactionState:kUGClientResponseFailure];
        [ret setResponse:error];
        [ret setRawResponse:nil];
        return ret;
    }
    
    // make the path and fire it off
    NSString *url = [self createURL:@"queues" append2:queuePath];
    return [self httpTransaction:url op:kUGHTTPPost opData:jsonStr];
}

-(UGClientResponse *)getMessages: (NSString *)queuePath query:(UGQuery *)query;
{
    NSMutableString *url = [self createURL:@"queues" append2:queuePath];
    [self appendQueryToURL:url query:query];
    return [self httpTransaction:url op:kUGHTTPGet opData:nil];
}

-(UGClientResponse *)addSubscriber: (NSString *)queuePath subscriberPath:(NSString *)subscriberPath
{
    NSString *url = [self createURL:@"queues" append2:queuePath append3:@"subscribers" append4:subscriberPath];
    return [self httpTransaction:url op:kUGHTTPPost opData:nil];
}

-(UGClientResponse *)removeSubscriber: (NSString *)queuePath subscriberPath:(NSString *)subscriberPath
{
    NSString *url = [self createURL:@"queues" append2:queuePath append3:@"subscribers" append4:subscriberPath];
    return [self httpTransaction:url op:kUGHTTPDelete opData:nil];
}

/*************************** REMOTE PUSH NOTIFICATIONS ***************************/
/*************************** REMOTE PUSH NOTIFICATIONS ***************************/
/*************************** REMOTE PUSH NOTIFICATIONS ***************************/

- (UGClientResponse *)setDevicePushToken:(NSData *)newDeviceToken forNotifier:(NSString *)notifier
{
    // Pull the push token string out of the device token data
    NSString *tokenString = [[[newDeviceToken description]
                              stringByTrimmingCharactersInSet:[NSCharacterSet characterSetWithCharactersInString:@"<>"]]
                             stringByReplacingOccurrencesOfString:@" " withString:@""];
    
    // Register device and push token to App Services
    NSString *deviceId = [UGClient getUniqueDeviceID];
    
    // create/update device - use deviceId for App Services entity UUID
    NSMutableDictionary *entity = [[NSMutableDictionary alloc] init];
    [entity setObject: @"device"   forKey: @"type"];
    [entity setObject: deviceId    forKey: @"uuid"];
    
    NSString *notifierKey = [notifier stringByAppendingString: @".notifier.id"];
    [entity setObject: tokenString forKey: notifierKey];
    
    return [self updateEntity: deviceId entity: entity];
}

- (UGClientResponse *)pushAlert:(NSString *)message
                      withSound:(NSString *)sound
                             to:(NSString *)path
                  usingNotifier:(NSString *)notifier
{
    NSDictionary *apsDict = [NSDictionary dictionaryWithObjectsAndKeys:
                             message, @"alert",
                             sound, @"sound",
                             nil];
    
    NSDictionary *notifierDict = [NSDictionary dictionaryWithObjectsAndKeys:
                                  apsDict, @"aps",
                                  nil];
    
    NSDictionary *payloadsDict = [NSDictionary dictionaryWithObjectsAndKeys:
                                  notifierDict, notifier,
                                  nil];
    
    NSString *notificationsPath = [path stringByAppendingString: @"/notifications"];
    
    NSMutableDictionary *entity = [[NSMutableDictionary alloc] init];
    [entity setObject: notificationsPath forKey: @"type"];
    [entity setObject: payloadsDict      forKey: @"payloads"];
    
    return [self createEntity: entity];
}


/*************************** SERVER-SIDE STORAGE ***************************/
/*************************** SERVER-SIDE STORAGE ***************************/
/*************************** SERVER-SIDE STORAGE ***************************/
// fun with uuids. Apple made this needlessly complex when they decided 
// developers were no longer allowed to access the device ID of the handset. 
+(NSString *)getUniqueDeviceID
{
    // first, see if we have the value cached
    if ( g_deviceUUID ) return g_deviceUUID;
    
    // next, see if we have the value in our database
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if ( !defaults ) return nil; // serious problems
    g_deviceUUID = [defaults valueForKey:@"UGClientDeviceUUID"];

    // if we did, we're good
    if ( g_deviceUUID ) return g_deviceUUID;
    
    // if we're here, we need to create a unique ID
    CFUUIDRef uuidRef = CFUUIDCreate(nil);
    CFStringRef uuidStringRef = CFUUIDCreateString(nil, uuidRef);
    CFRelease(uuidRef);
    
    // convert it to a usable string. Make our own copy.
    g_deviceUUID = [NSString stringWithString:(__bridge NSString *)uuidStringRef];
    if ( !g_deviceUUID ) return nil;

    // store it
    [defaults setObject:g_deviceUUID forKey:@"UGClientDeviceUUID"];
    [defaults synchronize];
    
    // done
    return g_deviceUUID;
}

-(UGClientResponse *)setRemoteStorage: (NSDictionary *)data
{
    // prep and validate the sent-in dict
    NSString *error;
    NSString *jsonStr = [self createJSON:data error:&error];
    if ( !jsonStr )
    {
        // report the error
        UGClientResponse *ret = [UGClientResponse new];
        [ret setTransactionID:-1];
        [ret setTransactionState:kUGClientResponseFailure];
        [ret setResponse:error];
        [ret setRawResponse:nil];
        return ret;
    }
    
    NSString *handsetUUID = [UGClient getUniqueDeviceID];
    NSString *url = [self createURL:@"devices" append2:handsetUUID];
    
    // this is a put. We replace whatever was there before
    return [self httpTransaction:url op:kUGHTTPPut opData:jsonStr];
}

-(UGClientResponse *)getRemoteStorage
{
    NSString *handsetUUID = [UGClient getUniqueDeviceID];
    NSString *url = [self createURL:@"devices" append2:handsetUUID];
    return [self httpTransaction:url op:kUGHTTPGet opData:nil];
}

/***************************** OBLIQUE USAGE ******************************/
-(UGClientResponse *)apiRequest: (NSString *)url operation:(NSString *)op data:(NSString *)opData
{
    // work out the op to use
    int opID = kUGHTTPGet;
    if ( [op isEqualToString:@"GET"] ) opID = kUGHTTPGet;
    if ( [op isEqualToString:@"POST"] ) opID = kUGHTTPPost;
    if ( [op isEqualToString:@"POSTFORM"] ) opID = kUGHTTPPostAuth;
    if ( [op isEqualToString:@"PUT"] ) opID = kUGHTTPPut;
    if ( [op isEqualToString:@"DELETE"] ) opID = kUGHTTPDelete;
    
    // fire it off. The data, formatting, etc. is all the client's problem. 
    // That's the way oblique functionality is. 
    return [self httpTransaction:url op:opID opData:opData];
}

/**************************** LOGGING ************************************/
-(void)setLogging: (BOOL)loggingState
{
    m_bLogging = loggingState;
}

@end


