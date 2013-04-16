//
//  UGConnection.m
//  UGAPIApp
//
//  Created by Tim Burks on 3/30/13.
//
//
#import "UGHTTPHelpers.h"
#import "UGHTTPResult.h"
#import "UGConnection.h"

@interface UGConnection ()
@property (nonatomic, strong) NSString *token;
@property (nonatomic, strong) NSDate *tokenExpirationDate;
@end

@implementation UGConnection

+ (UGConnection *) sharedConnection
{
    static UGConnection *connection = nil;
    if (!connection) {
        connection = [[UGConnection alloc] init];
    }
    return connection;
}

- (id) init {
    if (self = [super init]) {
        self.server = @"http://api.usergrid.com";
    }
    return self;
}

#pragma mark - Internal helpers -

- (NSString *) root
{
    return [NSString stringWithFormat:@"%@/%@/%@", self.server, self.organization, self.application];
}

- (NSMutableURLRequest *) authorizedRequestWithMethod:(NSString *) method
                                                 path:(NSString *) path
                                                 body:(NSData *) body
{
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:path]];
    [request setHTTPMethod:method];
    if (body) {
        [request setHTTPBody:body];
    }
    if (self.token) {
        [request setValue:[NSString stringWithFormat:@"Bearer %@", self.token] forHTTPHeaderField:@"Authorization"];
    }
    return request;
}

#pragma mark - External helpers -

#pragma mark Authentication

- (BOOL) authenticateWithResult:(UGHTTPResult *) result
{
    id results = result.object;
    id expires = results[@"expires_in"];
    self.token = results[@"access_token"];
    self.tokenExpirationDate = [NSDate dateWithTimeIntervalSinceNow:[expires intValue]];
    return [self isAuthenticated];
}

- (BOOL) isAuthenticated
{
    return (self.token &&
            self.tokenExpirationDate &&
            ([self.tokenExpirationDate compare:[NSDate date]] == NSOrderedDescending));
}

#pragma mark - Management API request generators -

#pragma mark Access tokens
// http://apigee.com/docs/usergrid/content/accesstoken

- (NSMutableURLRequest *) getAccessTokenForAdminWithUsername:(NSString *) username
                                                    password:(NSString *) password
{
    NSDictionary *query = @{@"grant_type":@"password",
                            @"username":username,
                            @"password":password};
    NSString *path = [NSMutableString stringWithFormat:@"%@/management/token?%@",
                      self.server,
                      [query URLQueryString]];
    return [self authorizedRequestWithMethod:@"GET" path:path body:nil];
}

- (NSMutableURLRequest *) getAccessTokenForOrganizationWithClientID:(NSString *) clientID
                                                       clientSecret:(NSString *) clientSecret
{
    NSDictionary *query = @{@"grant_type":@"client_credentials",
                            @"client_id":clientID,
                            @"client_secret":clientSecret};
    NSString *path = [NSMutableString stringWithFormat:@"%@/management/token?%@",
                      self.server,
                      [query URLQueryString]];
    return [self authorizedRequestWithMethod:@"GET" path:path body:nil];
}

- (NSMutableURLRequest *) getAccessTokenForApplicationWithUsername:(NSString *) username
                                                          password:(NSString *) password
{
    NSDictionary *query = @{@"grant_type":@"password",
                            @"username":username,
                            @"password":password};
    NSString *path = [NSMutableString stringWithFormat:@"%@/token?%@",
                      self.root,
                      [query URLQueryString]];
    return [self authorizedRequestWithMethod:@"GET" path:path body:nil];
}

- (NSMutableURLRequest *) getAccessTokenForApplicationWithClientID:(NSString *) clientID
                                                      clientSecret:(NSString *) clientSecret
{
    NSDictionary *query = @{@"grant_type":@"client_credentials",
                            @"client_id":clientID,
                            @"client_secret":clientSecret};
    NSString *path = [NSMutableString stringWithFormat:@"%@/token?%@",
                      self.root,
                      [query URLQueryString]];
    return [self authorizedRequestWithMethod:@"GET" path:path body:nil];
}

#pragma mark Organizations and Applications
// http://apigee.com/docs/usergrid/content/organization

- (NSMutableURLRequest *) createOrganization:(NSDictionary *) organization
{
    NSString *path = [NSString stringWithFormat:@"%@/management/organizations", self.server];
    return [self authorizedRequestWithMethod:@"POST"
                                        path:path
                                        body:[organization URLQueryData]];
}

- (NSMutableURLRequest *) getOrganization
{
    NSString *path = [NSString stringWithFormat:@"%@/management/organizations/%@",
                      self.server, self.organization];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) createApplication:(NSDictionary *) application
{
    NSString *path = [NSMutableString stringWithFormat:@"%@/management/organizations/%@/applications",
                      self.server,
                      self.organization];
    return [self authorizedRequestWithMethod:@"POST"
                                        path:path
                                        body:[application URLQueryData]];
}

- (NSMutableURLRequest *) deleteApplication
{
    return [self authorizedRequestWithMethod:@"DELETE"
                                        path:self.root
                                        body:nil];
}

- (NSMutableURLRequest *) getApplication
{
    return [self authorizedRequestWithMethod:@"GET"
                                        path:self.root
                                        body:nil];
}

- (NSMutableURLRequest *) getApplicationsForOrganization
{
    NSString *path = [NSMutableString stringWithFormat:@"%@/management/organizations/%@/applications",
                      self.server,
                      self.organization];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

#pragma mark Admin users
// http://apigee.com/docs/usergrid/content/admin-user

- (NSMutableURLRequest *) createAdminUser:(NSDictionary *) user
{
    NSString *path = [NSString stringWithFormat:@"%@/management/users", self.server];
    return [self authorizedRequestWithMethod:@"POST"
                                        path:path
                                        body:[user URLQueryData]];
}

#pragma mark - Application API request generators -

#pragma mark Activity
// http://apigee.com/docs/usergrid/content/activity

#pragma mark Assets

- (NSMutableURLRequest *) getDataForAssetWithUUID:(NSString *) uuid
{
    NSString *path = [NSMutableString stringWithFormat:@"%@/assets/%@/data",
                      self.root,
                      uuid];
    NSMutableURLRequest *request = [self authorizedRequestWithMethod:@"GET"
                                                                path:path
                                                                body:nil];
    return request;
}

- (NSMutableURLRequest *) postData:(NSData *) data
                  forAssetWithUUID:(NSString *) uuid
{
    NSString *path = [NSMutableString stringWithFormat:@"%@/assets/%@/data",
                      self.root,
                      uuid];
    NSMutableURLRequest *request = [self authorizedRequestWithMethod:@"POST"
                                                                path:path
                                                                body:data];
    [request setValue:@"application/octet-stream" forHTTPHeaderField:@"Content-Type"];
    return request;
}

#pragma mark General-purpose endpoints
// http://apigee.com/docs/usergrid/content/general-purpose-endpoints

- (NSMutableURLRequest *) createEntityInCollection:(NSString *) collection
                                        withValues:(NSDictionary *)values
{
    NSString *path = [NSMutableString stringWithFormat:@"%@/%@",
                      self.root,
                      collection];
    NSError *error;
    NSData *data = [NSJSONSerialization dataWithJSONObject:values options:0 error:&error];
    if (!data) {
        NSLog(@"%@", error);
    }
    return [self authorizedRequestWithMethod:@"POST"
                                        path:path
                                        body:data];
}

- (NSMutableURLRequest *) getEntityInCollection:(NSString *)collection
                                       withUUID:(NSString *)uuid
{
    NSString *path = [NSMutableString stringWithFormat:@"%@/%@/%@",
                      self.root,
                      collection,
                      uuid];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) updateEntityInCollection:(NSString *) collection
                                          withUUID:(NSString *) uuid
                                         newValues:(NSDictionary *) newValues {
    NSString *path = [NSString stringWithFormat:@"%@/%@/%@", self.root, collection, uuid];
    NSError *error;
    NSData *data = [NSJSONSerialization dataWithJSONObject:newValues options:0 error:&error];
    if (!data) {
        NSLog(@"%@", error);
    }
    return [self authorizedRequestWithMethod:@"PUT"
                                        path:path
                                        body:data];
}

- (NSMutableURLRequest *) deleteEntityInCollection:(NSString *) collection
                                          withUUID:(NSString *) uuid
{
    NSString *path = [NSString stringWithFormat:@"%@/%@/%@", self.root, collection, uuid];
    return [self authorizedRequestWithMethod:@"DELETE"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) getEntitiesInCollection:(NSString *) collection
                                            limit:(int) limit
{
    return [self getEntitiesInCollection:collection
                         withQueryString:@"select *"
                                   limit:limit
                               startUUID:nil
                                  cursor:nil
                                reversed:NO];
}

- (NSMutableURLRequest *) getEntitiesInCollection:(NSString *) collection
                                  withQueryString:(NSString *) queryString
                                            limit:(int) limit
                                        startUUID:(NSString *) startUUID
                                           cursor:(NSString *) cursor
                                         reversed:(BOOL) reversed
{
    NSMutableDictionary *query = [NSMutableDictionary dictionary];
    if (queryString) {
        query[@"ql"] = queryString;
    }
    if (limit > 0) {
        query[@"limit"] = [NSNumber numberWithInt:limit];
    }
    if (startUUID) {
        query[@"start"] = startUUID;
    }
    if (cursor) {
        query[@"cursor"] = cursor;
    }
    if (reversed) {
        query[@"reversed"] = @"true";
    }
    NSString *path = [NSMutableString stringWithFormat:@"%@/%@?%@",
                      self.root,
                      collection,
                      [query URLQueryString]];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) updateEntitiesInCollection:(NSString *) collection
                                     withQueryString:(NSString *) queryString
                                           newValues:(NSDictionary *) newValues
{
    assert(0); // todo
    return nil;
}

- (NSMutableURLRequest *) deleteEntitiesInCollection:(NSString *) collection
{
    return [self deleteEntitiesInCollection:collection
                            withQueryString:@"select *"];
}

- (NSMutableURLRequest *) deleteEntitiesInCollection:(NSString *) collection
                                     withQueryString:(NSString *) queryString
{
    NSDictionary *query = @{@"ql":queryString};
    NSString *path = [NSString stringWithFormat:@"%@/%@?%@", self.root, collection, [query URLQueryString]];
    return [self authorizedRequestWithMethod:@"DELETE"
                                        path:path
                                        body:nil];
}

#pragma mark Devices
// http://apigee.com/docs/usergrid/content/device

#pragma mark Events and Counters
// http://apigee.com/docs/usergrid/content/events-and-counters

#pragma mark Groups
// http://apigee.com/docs/usergrid/content/group

#pragma mark Roles
// http://apigee.com/docs/usergrid/content/role

#pragma mark Users
// http://apigee.com/docs/usergrid/content/user

@end
