#import <Foundation/Foundation.h>

@interface UGActivity : NSObject

// In order for an activity to be valid, you must call setBasics and one of the setActor functions. 
// In all cases, the return value will be YES if the function succeeded and NO if there
// was a problem with the set. A response of NO will usually mean you sent nil for a required field.

// these are the basics of the activity.
// verb: the action being taken
// category: The type of activity it is
// content: The content of this activity. The format is defined by the category
// title: The title of this category.
-(BOOL) setBasics: (NSString *)verb category:(NSString *)category content:(NSString *)content title:(NSString *)title;

// actorUserName: The username of the entity doing this activity
// actorDisplayName: The visible name of the entity doing this activity
// actorUUID: The UUID of the entity doing this activity
-(BOOL) setActorInfo: (NSString *)actorUserName actorDisplayName:(NSString *)actorDisplayName actorUUID:(NSString *)actorUUID;

// actorUserName: The username of the entity doing this activity
// actorDisplayName: The visible name of the entity doing this activity
// actorUUID: The UUID of the entity doing this activity
-(BOOL) setActorInfo: (NSString *)actorUserName actorDisplayName:(NSString *)actorDisplayName actorEmail:(NSString *)actorEmail;

// Associating an object with the Activity is optional. You don't have to supply an object at all.

// objectType: the type of the object associated with this activity
// displayName: The visible name of the object associated with this activity
// entityType: the entity type of this object within UserGrid. The actual type that it is stored under
// entityUUID: The uuid of the object associated with this activity
-(BOOL)setObjectInfo: (NSString *)objectType displayName:(NSString *)displayName entityType:(NSString *)entityType entityUUID:(NSString *)entityUUID;

// similar to the function above, but it takes an arbitrary object content (which can be new and unique) instead of an already-defined object
-(BOOL)setObjectInfo: (NSString *)objectType displayName:(NSString *)displayName objectContent:(NSString *)objectContent;

// similar to the other two functions, but simply has the type and displayName. In this case, the 
// "content" value supplied in setBasics will be used as the object content.
-(BOOL)setObjectInfo: (NSString *)objectType displayName:(NSString *)displayName;

// returns YES if this is properly set up. NO if it has not been properly set up
-(BOOL)isValid;

// turn this object in to an NSDictionary. Used internally by UGClient
-(NSDictionary *)toNSDictionary;

@end
