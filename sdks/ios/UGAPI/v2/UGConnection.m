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

#pragma mark Queries

- (NSMutableDictionary *) queryWithString:(NSString *) queryString
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
    return query;
}

#pragma mark Authentication

- (BOOL) authenticateWithResult:(UGHTTPResult *) result
{
    id results = result.object;
    self.token = results[@"access_token"];
    id expires = results[@"expires_in"];
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
    if (!username || !password) {
        return nil;
    }
    NSDictionary *query = @{@"grant_type":@"password",
                            @"username":username,
                            @"password":password};
    NSString *path = [NSMutableString stringWithFormat:@"%@/management/token?%@",
                      self.server, [query URLQueryString]];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) getAccessTokenForOrganizationWithClientID:(NSString *) clientID
                                                       clientSecret:(NSString *) clientSecret
{
    if (!clientID || !clientSecret) {
        return nil;
    }
    NSDictionary *query = @{@"grant_type":@"client_credentials",
                            @"client_id":clientID,
                            @"client_secret":clientSecret};
    NSString *path = [NSMutableString stringWithFormat:@"%@/management/token?%@",
                      self.server, [query URLQueryString]];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) getAccessTokenForApplicationWithUsername:(NSString *) username
                                                          password:(NSString *) password
{
    if (!username || !password) {
        return nil;
    }
    NSDictionary *query = @{@"grant_type":@"password",
                            @"username":username,
                            @"password":password};
    NSString *path = [NSMutableString stringWithFormat:@"%@/token?%@",
                      self.root, [query URLQueryString]];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) getAccessTokenForApplicationWithClientID:(NSString *) clientID
                                                      clientSecret:(NSString *) clientSecret
{
    if (!clientID || !clientSecret) {
        return nil;
    }
    NSDictionary *query = @{@"grant_type":@"client_credentials",
                            @"client_id":clientID,
                            @"client_secret":clientSecret};
    NSString *path = [NSMutableString stringWithFormat:@"%@/token?%@",
                      self.root, [query URLQueryString]];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

#pragma mark Organizations and Applications
// http://apigee.com/docs/usergrid/content/organization

- (NSMutableURLRequest *) createOrganization:(NSDictionary *) organization
{
    NSString *path = [NSString stringWithFormat:@"%@/management/organizations",
                      self.server];
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
                      self.server, self.organization];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

#pragma mark Admin users
// http://apigee.com/docs/usergrid/content/admin-user

- (NSMutableURLRequest *) createAdminUser:(NSDictionary *) user
{
    NSString *path = [NSString stringWithFormat:@"%@/management/users",
                      self.server];
    return [self authorizedRequestWithMethod:@"POST"
                                        path:path
                                        body:[user URLQueryData]];
}

#pragma mark - Application API request generators -

#pragma mark Activity
// http://apigee.com/docs/usergrid/content/activity

#pragma mark Assets

- (NSMutableURLRequest *) getDataForAsset:(NSString *) assetIdentifier
{
    NSString *path = [NSMutableString stringWithFormat:@"%@/assets/%@/data",
                      self.root, assetIdentifier];
    NSMutableURLRequest *request = [self authorizedRequestWithMethod:@"GET"
                                                                path:path
                                                                body:nil];
    return request;
}

- (NSMutableURLRequest *) postData:(NSData *) data
                          forAsset:(NSString *) assetIdentifier
{
    NSString *path = [NSMutableString stringWithFormat:@"%@/assets/%@/data",
                      self.root,
                      assetIdentifier];
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
                      self.root, collection];
    NSError *error;
    NSData *data = [NSJSONSerialization dataWithJSONObject:values options:0 error:&error];
    if (!data) {
        NSLog(@"%@", error);
    }
    return [self authorizedRequestWithMethod:@"POST"
                                        path:path
                                        body:data];
}

- (NSMutableURLRequest *) getEntity:(NSString *) entityIdentifier
                       inCollection:(NSString *) collection
{
    NSString *path = [NSMutableString stringWithFormat:@"%@/%@/%@",
                      self.root, collection, entityIdentifier];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) updateEntity:(NSString *) entityIdentifier
                          inCollection:(NSString *) collection
                            withValues:(NSDictionary *) values {
    NSString *path = [NSString stringWithFormat:@"%@/%@/%@", self.root, collection, entityIdentifier];
    NSError *error;
    NSData *data = [NSJSONSerialization dataWithJSONObject:values options:0 error:&error];
    if (!data) {
        NSLog(@"%@", error);
    }
    return [self authorizedRequestWithMethod:@"PUT"
                                        path:path
                                        body:data];
}

- (NSMutableURLRequest *) deleteEntity:(NSString *) entityIdentifier
                          inCollection:(NSString *) collection
{
    NSString *path = [NSString stringWithFormat:@"%@/%@/%@", self.root, collection, entityIdentifier];
    return [self authorizedRequestWithMethod:@"DELETE"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) getEntitiesInCollection:(NSString *) collection
                                            limit:(int) limit
{
    NSDictionary *query = [self queryWithString:@"select *"
                                          limit:limit
                                      startUUID:nil
                                         cursor:nil
                                       reversed:NO];
    return [self getEntitiesInCollection:collection
                              usingQuery:query];
}

- (NSMutableURLRequest *) getEntitiesInCollection:(NSString *) collection
                                       usingQuery:(NSDictionary *) query
{
    NSString *path = [NSMutableString stringWithFormat:@"%@/%@?%@",
                      self.root,
                      collection,
                      [query URLQueryString]];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) updateEntitiesInCollection:(NSString *) collection
                                          usingQuery:(NSDictionary *) query
                                          withValues:(NSDictionary *) values
{
    assert(0); // todo
    return nil;
}

- (NSMutableURLRequest *) deleteEntitiesInCollection:(NSString *) collection
{
    NSDictionary *query = [self queryWithString:@"select *"
                                          limit:0
                                      startUUID:nil
                                         cursor:nil
                                       reversed:NO];
    return [self deleteEntitiesInCollection:collection
                                 usingQuery:query];
}

- (NSMutableURLRequest *) deleteEntitiesInCollection:(NSString *) collection
                                          usingQuery:(NSDictionary *) query
{
    NSString *path = [NSString stringWithFormat:@"%@/%@?%@", self.root, collection, [query URLQueryString]];
    return [self authorizedRequestWithMethod:@"DELETE"
                                        path:path
                                        body:nil];
}

#pragma mark Devices
// http://apigee.com/docs/usergrid/content/device

#pragma mark Events and Counters
// http://apigee.com/docs/usergrid/content/events-and-counters

- (NSMutableURLRequest *) createEventWithValues:(NSDictionary *) values
{
    NSString *path = [NSString stringWithFormat:@"%@/events", self.root];
    return [self authorizedRequestWithMethod:@"POST"
                                        path:path
                                        body:[values URLQueryData]];
}

#pragma mark Groups
// http://apigee.com/docs/usergrid/content/group

- (NSMutableURLRequest *) createGroupWithValues:(NSDictionary *) values
{
    NSString *path = [NSString stringWithFormat:@"%@/groups", self.root];
    return [self authorizedRequestWithMethod:@"POST"
                                        path:path
                                        body:[values URLQueryData]];
}

- (NSMutableURLRequest *) addUser:(NSString *) user
                          toGroup:(NSString *) group
{
    NSString *path = [NSString stringWithFormat:@"%@/groups/%@/users/%@",
                      self.root, group, user];
    return [self authorizedRequestWithMethod:@"POST"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) getGroup:(NSString *) groupName
{
    NSString *path = [NSString stringWithFormat:@"%@/groups/%@", self.root, groupName];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) updateGroup:(NSString *) groupName
                           withValues:(NSDictionary *) values
{
    NSString *path = [NSString stringWithFormat:@"%@/groups/%@",
                      self.root, groupName];
    return [self authorizedRequestWithMethod:@"PUT"
                                        path:path
                                        body:[values URLQueryData]];
}

- (NSMutableURLRequest *) deleteUser:(NSString *) user
                           fromGroup:(NSString *) groupName
{
    NSString *path = [NSString stringWithFormat:@"%@/groups/%@/users/%@",
                      self.root, groupName, user];
    return [self authorizedRequestWithMethod:@"DELETE"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) deleteGroup:(NSString *) groupName
{
    NSString *path = [NSString stringWithFormat:@"%@/groups/%@",
                      self.root, groupName];
    return [self authorizedRequestWithMethod:@"DELETE"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) getUsersInGroup:(NSString *) groupName
{
    NSString *path = [NSString stringWithFormat:@"%@/groups/%@/users",
                      self.root, groupName];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

#pragma mark Roles
// http://apigee.com/docs/usergrid/content/role

- (NSMutableURLRequest *) createRoleWithValues:(NSDictionary *) values
{
    NSString *path = [NSString stringWithFormat:@"%@/roles", self.root];
    return [self authorizedRequestWithMethod:@"POST"
                                        path:path
                                        body:[values URLQueryData]];
}

- (NSMutableURLRequest *) getRoles
{
    NSString *path = [NSString stringWithFormat:@"%@/roles", self.root];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) deleteRole:(NSString *) roleName
{
    NSString *path = [NSString stringWithFormat:@"%@/roles/%@",
                      self.root, roleName];
    return [self authorizedRequestWithMethod:@"DELETE"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) getPermissionsForRole:(NSString *) roleName
{
    NSString *path = [NSString stringWithFormat:@"%@/roles/%@",
                      self.root, roleName];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) addPermissionsToRole:(NSString *) roleName
                                    withValues:(NSDictionary *) values
{
    NSString *path = [NSString stringWithFormat:@"%@/roles/%@/permissions",
                      self.root, roleName];
    return [self authorizedRequestWithMethod:@"POST"
                                        path:path
                                        body:[values URLQueryData]];
}

- (NSMutableURLRequest *) deletePermissionsFromRole:(NSString *) roleName
                                       usingPattern:(NSString *) pattern
{
    NSString *path = [NSString stringWithFormat:@"%@/roles/%@/permissions?pattern=%@",
                      self.root, roleName, pattern];
    return [self authorizedRequestWithMethod:@"DELETE"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) addUser:(NSString *)user
                           toRole:(NSString *)roleName
{
    NSString *path = [NSString stringWithFormat:@"%@/roles/%@/users/%@",
                      self.root, roleName, user];
    return [self authorizedRequestWithMethod:@"POST"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) getUsersInRole:(NSString *) roleName
{
    NSString *path = [NSString stringWithFormat:@"%@/roles/%@/users",
                      self.root, roleName];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) deleteUser:(NSString *) user
                            fromRole:(NSString *) roleName
{
    NSString *path = [NSString stringWithFormat:@"%@/roles/%@/users/%@",
                      self.root, roleName, user];
    return [self authorizedRequestWithMethod:@"DELETE"
                                        path:path
                                        body:nil];
}

#pragma mark Users
// http://apigee.com/docs/usergrid/content/user

- (NSMutableURLRequest *) createUserWithValues:(NSDictionary *) values
{
    NSString *path = [NSString stringWithFormat:@"%@/users", self.root];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:[values URLQueryData]];
}

- (NSMutableURLRequest *) setPasswordForUser:(NSString *) username
                                  toPassword:(NSString *) newPassword
                                fromPassword:(NSString *) oldPassword
{
    NSDictionary *query = @{@"newpassword":newPassword,
                            @"oldpassword":oldPassword};
    NSString *path = [NSString stringWithFormat:@"%@/users/%@/password",
                      self.root, username];
    return [self authorizedRequestWithMethod:@"POST"
                                        path:path
                                        body:[query URLQueryData]];
}

- (NSMutableURLRequest *) getUser:(NSString *) username
{
    NSString *path = [NSString stringWithFormat:@"%@/users/%@",
                      self.root, username];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) updateUser:(NSString *) username
                          withValues:(NSDictionary *) values
{
    NSString *path = [NSString stringWithFormat:@"%@/users/%@",
                      self.root, username];
    return [self authorizedRequestWithMethod:@"PUT"
                                        path:path
                                        body:[values URLQueryData]];
}

- (NSMutableURLRequest *) deleteUser:(NSString *) username
{
    NSString *path = [NSString stringWithFormat:@"%@/users/%@",
                      self.root, username];
    return [self authorizedRequestWithMethod:@"DELETE"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) getUsersUsingQuery:(NSDictionary *) query
{
    NSString *path = [NSString stringWithFormat:@"%@/users?%@",
                      self.root, [query URLQueryString]];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) connectEntity:(NSString *) entity1
                           inCollection:(NSString *) collection
                               toEntity:(NSString *) entity2
                    throughRelationship:(NSString *) relationship
{
    NSString *path = [NSString stringWithFormat:@"%@/%@/%@/%@/%@",
                      self.root, collection, entity1, relationship, entity2];
    return [self authorizedRequestWithMethod:@"POST"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) disconnectEntity:(NSString *) entity1
                              inCollection:(NSString *) collection
                                fromEntity:(NSString *) entity2
                       throughRelationship:(NSString *) relationship
{
    NSString *path = [NSString stringWithFormat:@"%@/%@/%@/%@/%@",
                      self.root, collection, entity1, relationship, entity2];
    return [self authorizedRequestWithMethod:@"DELETE"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) getConnectionsToEntity:(NSString *) entity
                                    inCollection:(NSString *) collection
                             throughRelationship:(NSString *) relationship
                                      usingQuery:(NSDictionary *) query
{
    NSString *path = [NSString stringWithFormat:@"%@/%@/%@/%@?%@",
                      self.root, collection, entity, relationship, [query URLQueryString]];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

- (NSMutableURLRequest *) getFeedForUser:(NSString *) username
{
    NSString *path = [NSString stringWithFormat:@"%@/users/%@/feed",
                      self.root, username];
    return [self authorizedRequestWithMethod:@"GET"
                                        path:path
                                        body:nil];
}

@end
