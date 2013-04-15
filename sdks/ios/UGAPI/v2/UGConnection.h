//
//  UGConnection.h
//  UGAPIApp
//
//  Created by Tim Burks on 3/30/13.
//
//
#import <Foundation/Foundation.h>

@class UGHTTPResult;

@interface UGConnection : NSObject

@property (nonatomic, strong) NSString *server;
@property (nonatomic, strong) NSString *organization;
@property (nonatomic, strong) NSString *application;

+ (UGConnection *) sharedConnection;

// Authentication helper: call this method with the result of a getAccessToken request.
- (BOOL) authenticateWithResult:(UGHTTPResult *) result;
// Authentication helper: use this to confirm that a connection has a usable access token.
- (BOOL) isAuthenticated;

//
// Usergrid API methods
// 
// The following calls return NSMutableURLRequest objects that can be used to make Usergrid API calls.
// We recommend (but do not require) that they be made with instances of the UGHTTPClient class.
//
// The goal here is to directly expose the complete Usergrid API.
// This follows http://apigee.com/docs/usergrid/content/app-services-resources
//

// Access tokens http://apigee.com/docs/usergrid/content/accesstoken

- (NSMutableURLRequest *) getAccessTokenForAdminWithUsername:(NSString *) username
                                                    password:(NSString *) password;

- (NSMutableURLRequest *) getAccessTokenForOrganizationWithClientID:(NSString *) clientID
                                                       clientSecret:(NSString *) clientSecret;

- (NSMutableURLRequest *) getAccessTokenForApplicationWithUsername:(NSString *) username
                                                          password:(NSString *) password;

- (NSMutableURLRequest *) getAccessTokenForApplicationWithClientID:(NSString *) clientID
                                                      clientSecret:(NSString *) clientSecret;

// Organizations and Applications http://apigee.com/docs/usergrid/content/organization

- (NSMutableURLRequest *) createOrganization:(NSDictionary *) organization;

- (NSMutableURLRequest *) getOrganization;

- (NSMutableURLRequest *) createApplication:(NSDictionary *) application;

- (NSMutableURLRequest *) deleteApplication;

- (NSMutableURLRequest *) getApplicationsForOrganization;

- (NSMutableURLRequest *) getApplication;

// Admin users http://apigee.com/docs/usergrid/content/admin-user

- (NSMutableURLRequest *) createAdminUser:(NSDictionary *) organization;

// Activity http://apigee.com/docs/usergrid/content/activity

// Assets

- (NSMutableURLRequest *) getDataForAssetWithUUID:(NSString *) uuid;

- (NSMutableURLRequest *) postData:(NSData *) data
                  forAssetWithUUID:(NSString *) uuid;

// General-purpose endpoints http://apigee.com/docs/usergrid/content/general-purpose-endpoints

- (NSMutableURLRequest *) createEntityInCollection:(NSString *) collection
                                        withValues:(NSDictionary *) values;

- (NSMutableURLRequest *) getEntityInCollection:(NSString *) collection
                                       withUUID:(NSString *) uuid;

- (NSMutableURLRequest *) updateEntityInCollection:(NSString *) collection
                                          withUUID:(NSString *) uuid
                                         newValues:(NSDictionary *) newValues;

- (NSMutableURLRequest *) deleteEntityInCollection:(NSString *) collection
                                          withUUID:(NSString *) uuid;

- (NSMutableURLRequest *) getEntitiesInCollection:(NSString *) collection
                                            limit:(int) limit;

- (NSMutableURLRequest *) getEntitiesInCollection:(NSString *) collection
                                  withQueryString:(NSString *) queryString
                                            limit:(int) limit
                                        startUUID:(NSString *) startUUID
                                           cursor:(NSString *) cursor
                                         reversed:(BOOL) reversed;

- (NSMutableURLRequest *) updateEntitiesInCollection:(NSString *) collection
                                     withQueryString:(NSString *) queryString
                                           newValues:(NSDictionary *) newValues;

- (NSMutableURLRequest *) deleteEntitiesInCollection:(NSString *) collection;

- (NSMutableURLRequest *) deleteEntitiesInCollection:(NSString *) collection
                                     withQueryString:(NSString *) queryString;


// Devices http://apigee.com/docs/usergrid/content/device

// Events and Counters http://apigee.com/docs/usergrid/content/events-and-counters

// Groups http://apigee.com/docs/usergrid/content/group

// Roles http://apigee.com/docs/usergrid/content/role

// Users http://apigee.com/docs/usergrid/content/user

@end
