#import "UGActivity.h"

// the way they have set up the object info
enum
{
    kUGActivityNoObject = 0,
    kUGActivityObjectEntity = 1,
    kUGActivityObjectContent = 2,
    kUGActivityObjectNameOnly = 3
};

@implementation UGActivity
{
    // basic stats
    NSString *m_verb;
    NSString *m_category;
    NSString *m_content;
    NSString *m_title;
    
    // stats related to the actor
    NSString *m_actorUserName;
    NSString *m_actorDisplayName;
    NSString *m_actorUUID;
    NSString *m_actorEmail;
    
    // stats related to the object
    NSString *m_objectType;
    NSString *m_objectDisplayName;
    
    // for the object, either these must be set...
    NSString *m_entityType;
    NSString *m_entityUUID;
    
    // ...or this must be set
    NSString *m_objectContent;
    
    // or it will use the content field as the content for the object
    
    // tracking how the object is currently set up
    int m_objectDataType;
}

-(id)init
{
    self = [super init];
    if ( self )
    {
        m_objectDataType = kUGActivityNoObject;
    }
    return self;
}

-(BOOL) setBasics: (NSString *)verb category:(NSString *)category content:(NSString *)content title:(NSString *)title
{
    // input validation
    if ( !verb || !category || !content || !title ) return NO;
    
    m_verb = verb;
    m_category = category;
    m_content = content;
    m_title = title;

    return YES;
}

-(BOOL) setActorInfo: (NSString *)actorUserName actorDisplayName:(NSString *)actorDisplayName actorUUID:(NSString *)actorUUID
{
    // input validation
    if ( !actorUserName || !actorDisplayName || !actorUUID ) return NO;
    
    m_actorUserName = actorUserName;
    m_actorDisplayName = actorDisplayName;
    m_actorUUID = actorUUID;
    m_actorEmail = nil;
    
    return YES;
}

-(BOOL) setActorInfo: (NSString *)actorUserName actorDisplayName:(NSString *)actorDisplayName actorEmail:(NSString *)actorEmail
{
    // input validation
    if ( !actorUserName || !actorDisplayName || !actorEmail ) return NO;
    
    m_actorUserName = actorUserName;
    m_actorDisplayName = actorDisplayName;
    m_actorEmail = actorEmail;
    m_actorUUID = nil;
    
    return YES;
}

-(BOOL)setObjectInfo: (NSString *)objectType displayName:(NSString *)displayName entityType:(NSString *)entityType entityUUID:(NSString *)entityUUID
{
    // input validation
    if ( !objectType || !displayName || !entityType || !entityUUID ) return NO;
    
    m_objectType = objectType;
    m_objectDisplayName = displayName;
    m_entityType = entityType;
    m_entityUUID = entityUUID;
    m_objectDataType = kUGActivityObjectEntity;
    
    // clear out the unused value
    m_objectContent = nil;
    
    return YES;
}

-(BOOL)setObjectInfo: (NSString *)objectType displayName:(NSString *)displayName objectContent:(NSString *)objectContent
{
    // input validation
    if ( !objectType || !displayName || !objectContent ) return NO;
    
    m_objectType = objectType;
    m_objectDisplayName = displayName;
    m_objectContent = objectContent;
    m_objectDataType = kUGActivityObjectContent;
    
    // clear out the unused values
    m_entityType = nil;
    m_entityUUID = nil;
    
    return YES;
}

-(BOOL)setObjectInfo: (NSString *)objectType displayName:(NSString *)displayName
{
    // input validation
    if ( !objectType || !displayName ) return NO;
    
    m_objectType = objectType;
    m_objectDisplayName = displayName;
    m_objectDataType = kUGActivityObjectNameOnly;
    
    // we'll use m_content when the time comes. But we don't want to 
    // assume they've set it yet. They can call the setup functions in any order.
    m_objectContent = nil; 
    m_entityType = nil;
    m_entityUUID = nil;
    
    return YES;
}

-(BOOL)isValid
{
    // if any of the required values are nil, it's not valid
    if ( !m_verb || !m_category || !m_content || !m_title || !m_actorUserName || !m_actorDisplayName )
    {
        return NO;
    }
    
    // either the uuid or the email of the user must be valid
    if ( !m_actorUUID && !m_actorEmail )
    {
        return NO;
    }
    
    // the object data requirements are based on the object setup
    switch ( m_objectDataType )
    {
        case kUGActivityObjectEntity:
        {
            if ( !m_objectType || !m_objectDisplayName || !m_entityType || !m_entityUUID ) return NO;
        }
        break;
            
        case kUGActivityObjectContent:
        {
            if ( !m_objectType || !m_objectDisplayName || !m_objectContent ) return NO;
        }
            break;
            
        case kUGActivityObjectNameOnly:
        {
            if ( !m_objectType || !m_objectDisplayName ) return NO;
        }
        break;
            
        // kUGActivityNoObject has no requirements.
    }
    
    // if we're here, we're valid.
    return YES;
}

-(NSDictionary *)toNSDictionary
{
    NSMutableDictionary *ret = [NSMutableDictionary new];
    
    // add all the fields in
    [ret setObject:@"activity" forKey:@"type"];
    [ret setObject:m_verb forKey:@"verb"];
    [ret setObject:m_category forKey:@"category"];
    [ret setObject:m_content forKey:@"content"];
    [ret setObject:m_title forKey:@"title"];
    
    // make the actor's subdictionary
    NSMutableDictionary *actor = [NSMutableDictionary new];
    [actor setObject:@"person" forKey:@"type"];
    [actor setObject:@"user" forKey:@"entityType"];
    [actor setObject:m_actorDisplayName forKey:@"displayName"];
    
    if ( m_actorUUID )
    {
        [actor setObject:m_actorUUID forKey:@"uuid"];
    }
    if ( m_actorEmail )
    {
        [actor setObject:m_actorEmail forKey:@"email"];
    }
    
    // add the actor to the main dict
    [ret setObject:actor forKey:@"actor"];
    
    if ( m_objectDataType != kUGActivityNoObject )
    {
        // there is an associated object. Prep a dict for it
        NSMutableDictionary *object = [NSMutableDictionary new];
        
        // these fields are involved in all cases
        [object setObject:m_objectType forKey:@"type"];
        [object setObject:m_objectDisplayName forKey:@"displayName"];
        
        if ( m_objectDataType == kUGActivityObjectContent )
        {
            [object setObject:m_objectContent forKey:@"content"];
        }
        else if ( m_objectDataType == kUGActivityObjectNameOnly )
        {
            [object setObject:m_content forKey:@"content"];
        }
        else if ( m_objectDataType == kUGActivityObjectEntity )
        {
            [object setObject:m_entityType forKey:@"entityType"];
            [object setObject:m_entityUUID forKey:@"entityUUID"];
        }
        
        // add to the dict
        [ret setObject:object forKey:@"object"];
    }
    
    // done with the assembly
    return ret;
}

@end
