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

// These three properties tie a  UGConnection to a specific application instance
@property (nonatomic, strong) NSString *server;
@property (nonatomic, strong) NSString *organization;
@property (nonatomic, strong) NSString *application;

// As a convenience, a sharedConnection object is available,
// but any number of UGConnection objects may be created and separately configured.
+ (UGConnection *) sharedConnection;

// Query helper: construct query dictionary from arguments
- (NSMutableDictionary *) queryWithString:(NSString *) queryString
                                    limit:(int) limit
                                startUUID:(NSString *) startUUID
                                   cursor:(NSString *) cursor
                                 reversed:(BOOL) reversed;

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
//
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

// Admin users http://apigee.com/docs/usergrid/content/admin-user

- (NSMutableURLRequest *) createAdminUserWithValues:(NSDictionary *) values;

- (NSMutableURLRequest *) updateAdminUser:(NSString *) adminUserIdentifier
                               withValues:(NSDictionary *) values;

- (NSMutableURLRequest *) getAdminUser:(NSString *) adminUserIdentifier;

- (NSMutableURLRequest *) setPasswordForAdminUser:(NSString *) adminUserIdentifier
                                          toValue:(NSString *) password;

- (NSMutableURLRequest *) initiatePasswordResetForAdminUser;

- (NSMutableURLRequest *) completePasswordResetForAdminUserWithValues:(NSDictionary *) values;

- (NSMutableURLRequest *) activateAdminUser:(NSString *) adminUserIdentifier
                                  withToken:(NSString *) token
                   sendingConfirmationEmail:(BOOL) sendingConfirmationEmail;

- (NSMutableURLRequest *) reactivateAdminUser:(NSString *) adminUserIdentifier;

- (NSMutableURLRequest *) getActivityFeedForAdminUser:(NSString *) adminUserIdentifier;

// Client authorization http://apigee.com/docs/usergrid/content/client-authorization

- (NSMutableURLRequest *) authorizeClient:(NSString *) clientIdentifier withResponseType:(NSString *) responseType;

// Organizations and Applications http://apigee.com/docs/usergrid/content/organization

- (NSMutableURLRequest *) createOrganizationWithValues:(NSDictionary *) values;

- (NSMutableURLRequest *) getOrganization:(NSString *) organizationIdentifier;

- (NSMutableURLRequest *) activateOrganization:(NSString *) organizationIdentifier
                                     withToken:(NSString *) token
                      sendingConfirmationEmail:(BOOL) sendingConfirmationEmail;

- (NSMutableURLRequest *) reactivateOrganization:(NSString *) organizationIdentifier;

- (NSMutableURLRequest *) generateClientCredentialsForOrganization:(NSString *) organizationIdentifier;

- (NSMutableURLRequest *) getClientCredentialsForOrganization:(NSString *) organizationIdentifier;

- (NSMutableURLRequest *) getActivityFeedForOrganization:(NSString *) organizationIdentifier;

- (NSMutableURLRequest *) createApplicationInOrganization:(NSString *) organizationIdentifier
                                               withValues:(NSDictionary *) values;

- (NSMutableURLRequest *) deleteApplication:(NSString *) applicationIdentifier
                             inOrganization:(NSString *) organizationIdentifier;

- (NSMutableURLRequest *) generateClientCredentialsForApplication:(NSString *) applicationIdentifier
                                                   inOrganization:(NSString *) organizationIdentifier;

- (NSMutableURLRequest *) getClientCredentialsForApplication:(NSString *) applicationIdentifier
                                              inOrganization:(NSString *) organizationIdentifier;

- (NSMutableURLRequest *) getApplicationsInOrganization:(NSString *) organizationIdentifier;

- (NSMutableURLRequest *) addAdminUser:(NSString *) adminUserIdentifier
                        toOrganization:(NSString *) organizationIdentifier;

- (NSMutableURLRequest *) getAdminUsersInOrganization:(NSString *) organizationIdentifier;

- (NSMutableURLRequest *) removeAdminUser:(NSString *) adminUserIdentifier
                         fromOrganization:(NSString *) organizationIdentifier;

- (NSMutableURLRequest *) getApplication:(NSString *) applicationIdentifier
                          inOrganization:(NSString *) organizationIdentifier;

// Activity http://apigee.com/docs/usergrid/content/activity

- (NSMutableURLRequest *) createActivityForUser:(NSString *) userIdentifier
                                     withValues:(NSDictionary *) values;

- (NSMutableURLRequest *) createActivityForGroup:(NSString *) groupIdentifier
                                      withValues:(NSDictionary *) values;

- (NSMutableURLRequest *) createActivityForFollowersOfUser:(NSString *) userIdentifier
                                                   inGroup:(NSString *) groupIdentifier
                                                withValues:(NSDictionary *) values;

- (NSMutableURLRequest *) getActivitiesForUser:(NSString *) userIdentifier;

- (NSMutableURLRequest *) getActivitiesForGroup:(NSString *) groupIdentifier;

- (NSMutableURLRequest *) getActivityFeedForUser:(NSString *) userIdentifier;

- (NSMutableURLRequest *) getActivityFeedForGroup:(NSString *) groupIdentifier;

// Assets

- (NSMutableURLRequest *) getDataForAsset:(NSString *) assetIdentifier;

- (NSMutableURLRequest *) postData:(NSData *) data
                          forAsset:(NSString *) assetIdentifier;

// Collections (aka General-purpose endpoints) http://apigee.com/docs/usergrid/content/general-purpose-endpoints

- (NSMutableURLRequest *) createEntityInCollection:(NSString *) collection
                                        withValues:(NSDictionary *) values;

- (NSMutableURLRequest *) getEntity:(NSString *) entityIdentifier
                       inCollection:(NSString *) collection;

- (NSMutableURLRequest *) updateEntity:(NSString *) entityIdentifier
                          inCollection:(NSString *) collection
                            withValues:(NSDictionary *) values;

- (NSMutableURLRequest *) deleteEntity:(NSString *) entityIdentifier
                          inCollection:(NSString *) collection;

- (NSMutableURLRequest *) getEntitiesInCollection:(NSString *) collection
                                            limit:(int) limit;

- (NSMutableURLRequest *) getEntitiesInCollection:(NSString *) collection
                                       usingQuery:(NSDictionary *) query;

- (NSMutableURLRequest *) updateEntitiesInCollection:(NSString *) collection
                                          usingQuery:(NSDictionary *) query
                                          withValues:(NSDictionary *) values;

- (NSMutableURLRequest *) deleteEntitiesInCollection:(NSString *) collection;

- (NSMutableURLRequest *) deleteEntitiesInCollection:(NSString *) collection
                                          usingQuery:(NSDictionary *) query;


// Devices http://apigee.com/docs/usergrid/content/device

// there are no device-specific methods

// Events and Counters http://apigee.com/docs/usergrid/content/events-and-counters

- (NSMutableURLRequest *) createEventWithValues:(NSDictionary *) values;

// Groups http://apigee.com/docs/usergrid/content/group

- (NSMutableURLRequest *) createGroupWithValues:(NSDictionary *) values;

- (NSMutableURLRequest *) addUser:(NSString *) userIdentifier
                          toGroup:(NSString *) groupIdentifier;

- (NSMutableURLRequest *) getGroup:(NSString *) groupIdentifier;

- (NSMutableURLRequest *) updateGroup:(NSString *) groupIdentifier
                           withValues:(NSDictionary *) values;

- (NSMutableURLRequest *) deleteUser:(NSString *) userIdentifier
                           fromGroup:(NSString *) groupIdentifier;

- (NSMutableURLRequest *) deleteGroup:(NSString *) groupIdentifier;

- (NSMutableURLRequest *) getUsersInGroup:(NSString *) groupIdentifier;

// Roles http://apigee.com/docs/usergrid/content/role

- (NSMutableURLRequest *) createRoleWithValues:(NSDictionary *) values;

- (NSMutableURLRequest *) getRoles;

- (NSMutableURLRequest *) deleteRole:(NSString *) roleName;

- (NSMutableURLRequest *) getPermissionsForRole:(NSString *) roleName;

- (NSMutableURLRequest *) addPermissionsToRole:(NSString *) roleName
                                    withValues:(NSDictionary *) values;

- (NSMutableURLRequest *) deletePermissionsFromRole:(NSString *) roleName
                                       usingPattern:(NSString *) pattern;

- (NSMutableURLRequest *) addUser:(NSString *)userIdentifier
                           toRole:(NSString *)roleName;

- (NSMutableURLRequest *) getUsersInRole:(NSString *) roleName;

- (NSMutableURLRequest *) deleteUser:(NSString *) userIdentifier
                            fromRole:(NSString *) roleName;

// Users http://apigee.com/docs/usergrid/content/user

- (NSMutableURLRequest *) createUserWithValues:(NSDictionary *) values;

- (NSMutableURLRequest *) setPasswordForUser:(NSString *) username
                                  toPassword:(NSString *) newPassword
                                fromPassword:(NSString *) oldPassword;

- (NSMutableURLRequest *) getUser:(NSString *) username;

- (NSMutableURLRequest *) updateUser:(NSString *) username
                          withValues:(NSDictionary *) values;

- (NSMutableURLRequest *) deleteUser:(NSString *) username;

- (NSMutableURLRequest *) getUsersUsingQuery:(NSDictionary *) query;

- (NSMutableURLRequest *) connectEntity:(NSString *) entity1
                           inCollection:(NSString *) collection
                               toEntity:(NSString *) entity2
                    throughRelationship:(NSString *) relationship;

- (NSMutableURLRequest *) disconnectEntity:(NSString *) entity1
                              inCollection:(NSString *) collection
                                fromEntity:(NSString *) entity2
                       throughRelationship:(NSString *) relationship;

- (NSMutableURLRequest *) getConnectionsToEntity:(NSString *) entity
                                    inCollection:(NSString *) collection
                             throughRelationship:(NSString *) relationship
                                      usingQuery:(NSDictionary *) query;

@end
