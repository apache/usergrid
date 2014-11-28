/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Core.
 * 
 * Usergrid Core is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Usergrid Core is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Usergrid Core. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.apache.usergrid.android.sdk.entities;


import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.usergrid.android.sdk.UGClient;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;

/**
 * An entity type for representing posts to an activity stream. These are similar to
 * the more generic message entity type except provide the necessary properties
 * for supporting activity stream implementations.
 * 
 * @see <a href="http://activitystrea.ms/specs/json/1.0/">JSON activity stream spec</a>
 * @see <a href="http://apigee.com/docs/app-services/content/activity">Activity entity documentation</a>
 */
public class Activity extends Entity {

    public static final String ENTITY_TYPE = "activity";

    public static final String PROP_ACTOR = "actor";

    public static final String VERB_ADD = "add";
    public static final String VERB_CANCEL = "cancel";
    public static final String VERB_CHECKIN = "checkin";
    public static final String VERB_DELETE = "delete";
    public static final String VERB_FAVORITE = "favorite";
    public static final String VERB_FOLLOW = "follow";
    public static final String VERB_GIVE = "give";
    public static final String VERB_IGNORE = "ignore";
    public static final String VERB_INVITE = "invite";
    public static final String VERB_JOIN = "join";
    public static final String VERB_LEAVE = "leave";
    public static final String VERB_LIKE = "like";
    public static final String VERB_MAKE_FRIEND = "make-friend";
    public static final String VERB_PLAY = "play";
    public static final String VERB_POST = "post";
    public static final String VERB_RECEIVE = "receive";
    public static final String VERB_REMOVE = "remove";
    public static final String VERB_REMOVE_FRIEND = "remove-friend";
    public static final String VERB_REQUEST_FRIEND = "request-friend";
    public static final String VERB_RSVP_MAYBE = "rsvp-maybe";
    public static final String VERB_RSVP_NO = "rsvp-no";
    public static final String VERB_RSVP_YES = "rsvp-yes";
    public static final String VERB_SAVE = "save";
    public static final String VERB_SHARE = "share";
    public static final String VERB_STOP_FOLLOWING = "stop-following";
    public static final String VERB_TAG = "tag";
    public static final String VERB_UNFAVORITE = "unfavorite";
    public static final String VERB_UNLIKE = "unlike";
    public static final String VERB_UNSAVE = "unsave";
    public static final String VERB_UPDATE = "update";

    public static final String OBJECT_TYPE_ARTICLE = "article";
    public static final String OBJECT_TYPE_AUDIO = "audio";
    public static final String OBJECT_TYPE_BADGE = "badge";
    public static final String OBJECT_TYPE_BOOKMARK = "bookmark";
    public static final String OBJECT_TYPE_COLLECTION = "collection";
    public static final String OBJECT_TYPE_COMMENT = "comment";
    public static final String OBJECT_TYPE_EVENT = "event";
    public static final String OBJECT_TYPE_FILE = "file";
    public static final String OBJECT_TYPE_GROUP = "group";
    public static final String OBJECT_TYPE_IMAGE = "image";
    public static final String OBJECT_TYPE_NOTE = "note";
    public static final String OBJECT_TYPE_PERSON = "person";
    public static final String OBJECT_TYPE_PLACE = "place";
    public static final String OBJECT_TYPE_PRODUCT = "product";
    public static final String OBJECT_TYPE_QUESTION = "question";
    public static final String OBJECT_TYPE_REVIEW = "review";
    public static final String OBJECT_TYPE_SERVICE = "service";
    public static final String OBJECT_TYPE_VIDEO = "video";

    protected ActivityObject actor;

    protected String content;

    protected ActivityObject generator;

    protected MediaLink icon;

    protected String category;

    protected String verb;

    protected Long published;

    protected ActivityObject object;

    // protected
    // String objectType;

    // protected
    // String objectEntityType;

    // protected
    // String objectName;

    protected String title;

    protected Set<String> connections;

    /**
     * Checks if the provided type equals 'activity'.
     *
     * @return boolean true/false
     */
	public static boolean isSameType(String type) {
		return type.equals(ENTITY_TYPE);
	}
	
    /**
     * Default constructor. Sets entity type to 'activity'.
     */
	public Activity() {
		setType("activity");
	}

    /**
     * Constructs the Activity with an instance of UGClient.
     *
     * @param  UGClient  an instance of UGClient
     */
    public Activity(UGClient client) {
    	super(client);
        setType("activity");
    }

    /**
     * Constructs the Activity with an instance of UGClient and
     * an entity UUID.
     *
     * @param  UGClient  an instance of UGClient
     * @param  id  the UUID of the activity entity
     */
    public Activity(UGClient client, UUID id) {
        this(client);
        setUuid(id);
    }

    /**
     * Creates a new Activity object.
     *
     * @param  UGClient  an instance of UGClient
     * @param  verb  the 'verb' to associate with the activity, e.g. 'uploaded', 'posted', etc
     * @param  title  the title of the activity
     * @param  content  the content of the posted activity
     * @param  category  the category of the activity
     * @param  user  an Entity object that represents the 'actor', i.e. the user performing the activity.
     *      If the name property is set it will be used for the display name of the actor,
     *      otherwise the username will be used.
     * @param  object  the Entity object that is acted on, e.g. the article posted, the image uploaded, etc.
     * @param  objectType  the type of activity object, e.g. article, group, review, etc.
     * @param  objectName  optional. The name of the activity object, e.g. 
     * @param  objectContent  optional. The content of the object, e.g. a link to a posted photo
     * @return an Activity object
     */
    public static Activity newActivity(UGClient client, String verb, String title,
            String content, String category, Entity user, Entity object,
            String objectType, String objectName, String objectContent){

        Activity activity = new Activity(client);
        activity.setVerb(verb);
        activity.setCategory(category);
        activity.setContent(content);
        activity.setTitle(title);
        
        ActivityObject actor = new ActivityObject();
        actor.setObjectType("person");
        
        if (user != null) {            
            if(user.getStringProperty("name") != null) {
        	   actor.setDisplayName(user.getStringProperty("name"));
            } else {
               actor.setDisplayName(user.getStringProperty("username"));
            }
            actor.setEntityType(user.getType());
            actor.setUuid(user.getUuid());
        }
        
        activity.setActor(actor);

        ActivityObject obj = new ActivityObject();
        obj.setDisplayName(objectName);
        obj.setObjectType(objectType);
        if (object != null) {
            obj.setEntityType(object.getType());
            obj.setUuid(object.getUuid());
        }
        if (objectContent != null) {
            obj.setContent(objectContent);
        } else {
            obj.setContent(content);
        }
        activity.setObject(obj);

        return activity;
    }

    /**
     * Gets the 'actor' of the activity
     *
     * @return  an ActivityObject that represents the actor
     */
    @JsonSerialize(include = Inclusion.NON_NULL)
    public ActivityObject getActor() {
        return actor;
    }

    /**
     * Sets the 'actor' of the activity
     *
     * @param  actor  an ActivityObject that represents the actor
     */
    public void setActor(ActivityObject actor) {
        this.actor = actor;
    }

    /**
     * Gets the activity generator, i.e. a link to the application that
     * generated the activity object.
     *
     * @return  the generator
     */
    @JsonSerialize(include = Inclusion.NON_NULL)
    public ActivityObject getGenerator() {
        return generator;
    }

    /**
     * Sets the activity generator, i.e. a link to the application that
     * generated the activity object.
     *
     * @param  generator  the generator
     */
    public void setGenerator(ActivityObject generator) {
        this.generator = generator;
    }

    /*
     * @JsonSerialize(include = Inclusion.NON_NULL) public String getActorName()
     * { return actorName; }
     * 
     * public void setActorName(String actorName) { this.actorName = actorName;
     * }
     */
    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the verb of the Activity.
     *
     * @return  the activity verb
     */
    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getVerb() {
        return verb;
    }

    /**
     * Sets the verb of the Activity.
     *
     * @param  verb  the verb
     */
    public void setVerb(String verb) {
        this.verb = verb;
    }

    /**
     * Retrieves the UNIX timestamp of when the activity was published.
     *
     * @return a UNIX timestamp
     */
    @JsonSerialize(include = Inclusion.NON_NULL)
    public Long getPublished() {
        return published;
    }

    /**
     * Sets the UNIX timestamp of when the activity was published.
     *
     * @param  published  a UNIX timestamp
     */
    public void setPublished(Long published) {
        this.published = published;
    }

    /**
     * Retrieves the object of the activity.
     *
     * @return  an ActivityObject representing the object
     */
    @JsonSerialize(include = Inclusion.NON_NULL)
    public ActivityObject getObject() {
        return object;
    }

    /**
     * Sets the object of the activity.
     *
     * @param  object  an ActivityObject representing the object
     */
    public void setObject(ActivityObject object) {
        this.object = object;
    }

    /*
     * @JsonSerialize(include = Inclusion.NON_NULL) public String
     * getObjectType() { return objectType; }
     * 
     * public void setObjectType(String objectType) { this.objectType =
     * objectType; }
     * 
     * @JsonSerialize(include = Inclusion.NON_NULL) public String
     * getObjectEntityType() { return objectEntityType; }
     * 
     * public void setObjectEntityType(String objectEntityType) {
     * this.objectEntityType = objectEntityType; }
     * 
     * @JsonSerialize(include = Inclusion.NON_NULL) public String
     * getObjectName() { return objectName; }
     * 
     * public void setObjectName(String objectName) { this.objectName =
     * objectName; }
     */

    /**
     * Retrieves the title of the activity
     *
     * @return the title
     */
    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the Activity
     *
     * @param  title  the title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the icon to display with the Activity.
     *
     * @return  a MediaLink object that represents the icon
     */
    @JsonSerialize(include = Inclusion.NON_NULL)
    public MediaLink getIcon() {
        return icon;
    }

    /**
     * Sets the icon to display with the Activity.
     *
     * @param  icon  a MediaLink object that represents the icon
     */
    public void setIcon(MediaLink icon) {
        this.icon = icon;
    }

    /**
     * Retrieves the content of the Activity.
     *
     * @return  the activity content
     */
    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getContent() {
        return content;
    }

    /**
     * Sets the content of the Activity.
     *
     * @param  content  the activity content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Gets the entity connections for the activity.
     *
     * @return  the connections as a Set object
     */
    @JsonSerialize(include = Inclusion.NON_NULL)
    public Set<String> getConnections() {
        return connections;
    }

    /**
     * Stores the entity connections for the activity.
     *
     * @param  connections  the connections as a Set object
     */
    public void setConnections(Set<String> connections) {
        this.connections = connections;
    }

    /**
     * Models a media object, such as an image.
     */
    //@XmlRootElement
    static public class MediaLink {

        int duration;

        int height;

        String url;

        int width;

        protected Map<String, Object> dynamic_properties = new TreeMap<String, Object>(
                String.CASE_INSENSITIVE_ORDER);

        /**
         * Default constructor.
         */
        public MediaLink() {
        }

        /**
         * Retrieves the duration of the media, e.g. the length of a video.
         *
         * @return the duration
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public int getDuration() {
            return duration;
        }

        /**
         * Sets the duration of the media, e.g. the length of a video.
         *
         * @param  duration  the duration
         */
        public void setDuration(int duration) {
            this.duration = duration;
        }

        /**
         * Retrieves the height of the media, e.g. height of the image.
         *
         * @return the height
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public int getHeight() {
            return height;
        }

        /**
         * Sets the height of the media, e.g. height of the image.
         *
         * @param  height  the height
         */
        public void setHeight(int height) {
            this.height = height;
        }

        /**
         * Retrieves the url of the media, e.g. url a video can be streamed from.
         *
         * @return the url
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public String getUrl() {
            return url;
        }

        /**
         * Sets the url of the media, e.g. url a video can be streamed from.
         *
         * @param  url  the url
         */
        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * Retrieves the width of the media, e.g. image width.
         *
         * @return the width
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public int getWidth() {
            return width;
        }

        /**
         * Sets the width of the media, e.g. image width.
         *
         * @param  width  the width
         */
        public void setWidth(int width) {
            this.width = width;
        }

        @JsonAnySetter
        public void setDynamicProperty(String key, Object value) {
            dynamic_properties.put(key, value);
        }

        @JsonAnyGetter
        public Map<String, Object> getDynamicProperties() {
            return dynamic_properties;
        }

        /**
         * Returns the properties of the MediaLink object as a string.
         *
         * @return the object properties
         */
        @Override
        public String toString() {
            return "MediaLink [duration=" + duration + ", height=" + height
                    + ", url=" + url + ", width=" + width
                    + ", dynamic_properties=" + dynamic_properties + "]";
        }

    }

    /**
     * Models the object of an activity. For example, for the activity
     * 'John posted a new article', the article is the activity object.
     */
    //@XmlRootElement
    static public class ActivityObject {

        ActivityObject[] attachments;

        ActivityObject author;

        String content;

        String displayName;

        String[] downstreamDuplicates;

        String id;

        MediaLink image;

        String objectType;

        Date published;

        String summary;

        String updated;

        String upstreamDuplicates;

        String url;

        UUID uuid;

        String entityType;

        protected Map<String, Object> dynamic_properties = new TreeMap<String, Object>(
                String.CASE_INSENSITIVE_ORDER);

        /**
         * Default constructor.
         */
        public ActivityObject() {
        }

        /**
         * Gets the attachments for the activity
         *
         * @return an array of ActivityObject objects that represent the attachments
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public ActivityObject[] getAttachments() {
            return attachments;
        }

        /**
         * Sets the attachments for the activity
         *
         * @param  attachments  an array of ActivityObject objects that represent the attachments
         */
        public void setAttachments(ActivityObject[] attachments) {
            this.attachments = attachments;
        }

        /**
         * Gets the author who posted the activity. This can be distinct from
         * the actor, who is the user that performed the activity.
         * 
         * @return an ActivityObject that represents the author
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public ActivityObject getAuthor() {
            return author;
        }

        /**
         * Sets the author who posted the activity. This can be distinct from
         * the actor, who is the user that performed the activity.
         * 
         * @param  author  an ActivityObject that represents the author
         */
        public void setAuthor(ActivityObject author) {
            this.author = author;
        }

        /**
         * Gets the content of the activity.
         *
         * @return  the activity content
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public String getContent() {
            return content;
        }

        /**
         * Sets the content of the activity.
         *
         * @param  content  the activity content
         */
        public void setContent(String content) {
            this.content = content;
        }

        /**
         * Gets the display name of the activity.
         *
         * @return  the dislay name
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Sets the display name of the activity.
         *
         * @param  displayName  the dislay name
         */
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Gets the IRIs identifying objects that duplicate this object's content.
         *
         * @return An array of one or more absolute IRIs
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public String[] getDownstreamDuplicates() {
            return downstreamDuplicates;
        }

        /**
         * Sets the IRIs identifying objects that duplicate this object's content.
         *
         * @param  downstreamDuplicates  An array of one or more absolute IRIs
         */
        public void setDownstreamDuplicates(String[] downstreamDuplicates) {
            this.downstreamDuplicates = downstreamDuplicates;
        }

        /**
         * Gets the id of this object. Should be universally unique.
         *
         * @return  the id
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public String getId() {
            return id;
        }

        /**
         * Sets the id of this object. Should be universally unique.
         *
         * @param  id  the id
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * Gets the image associated with this object.
         *
         * @return  a MediaLink object that describes the image
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public MediaLink getImage() {
            return image;
        }

        /**
         * Sets the image associated with this object.
         *
         * @param  image  a MediaLink object that describes the image
         */
        public void setImage(MediaLink image) {
            this.image = image;
        }

        /**
         * Gets the object type associated with this object.
         *
         * @return  the type of the object
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public String getObjectType() {
            return objectType;
        }

        /**
         * Sets the object type associated with this object.
         *
         * @param  objectType  the type of the object
         */
        public void setObjectType(String objectType) {
            this.objectType = objectType;
        }

        /**
         * Gets the date this object was published.
         *
         * @return the date
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public Date getPublished() {
            return published;
        }

        /**
         * Sets the date this object was published.
         *
         * @param  published  the date
         */
        public void setPublished(Date published) {
            this.published = published;
        }

        /**
         * Gets the summary for this object.
         *
         * @return the summary
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public String getSummary() {
            return summary;
        }

        /**
         * Sets the summary for this object.
         *
         * @param  summary  the summary
         */
        public void setSummary(String summary) {
            this.summary = summary;
        }

        /**
         * Gets the date this object was last updated.
         *
         * @return the updated date
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public String getUpdated() {
            return updated;
        }

        /**
         * Sets the date this object was last updated.
         *
         * @param  updated  the updated date
         */
        public void setUpdated(String updated) {
            this.updated = updated;
        }

        /**
         * Gets the IRIs identifying objects that this object's content duplicates.
         *
         * @return A JSON Array of one or more absolute IRIs
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public String getUpstreamDuplicates() {
            return upstreamDuplicates;
        }

        /**
         * Sets the IRIs identifying objects that this object's content duplicates.
         *
         * @param  upstreamDuplicates  A JSON Array of one or more absolute IRIs
         */
        public void setUpstreamDuplicates(String upstreamDuplicates) {
            this.upstreamDuplicates = upstreamDuplicates;
        }

        /**
         * Gets the url for the entity that corresponds to this object
         *
         * @return the URL
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public String getUrl() {
            return url;
        }

        /**
         * Sets the url for the entity that corresponds to this object
         *
         * @param  url  the URL
         */
        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * Gets the UUID for the entity this object is modeling.
         *
         * @return the UUID
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public UUID getUuid() {
            return uuid;
        }

        /**
         * Sets the UUID for the entity this object is modeling.
         *
         * @param  uuid  a UUID object
         */
        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }

        /**
         * Gets the entity type for the entity this object is modeling.
         *
         * @return the entity type
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public String getEntityType() {
            return entityType;
        }

        /**
         * Sets the entity type for the entity this object is modeling.
         *
         * @param  entityType  the entity type
         */
        public void setEntityType(String entityType) {
            this.entityType = entityType;
        }

        @JsonAnySetter
        public void setDynamicProperty(String key, Object value) {
            dynamic_properties.put(key, value);
        }

        @JsonAnyGetter
        public Map<String, Object> getDynamicProperties() {
            return dynamic_properties;
        }

        /**
         * Returns the properties of the ActivityObject as a string.
         *
         * @return the object properties
         */        
        @Override
        public String toString() {
            return "ActivityObject [attachments="
                    + Arrays.toString(attachments) + ", author=" + author
                    + ", content=" + content + ", displayName=" + displayName
                    + ", downstreamDuplicates="
                    + Arrays.toString(downstreamDuplicates) + ", id=" + id
                    + ", image=" + image + ", objectType=" + objectType
                    + ", published=" + published + ", summary=" + summary
                    + ", updated=" + updated + ", upstreamDuplicates="
                    + upstreamDuplicates + ", url=" + url + ", uuid=" + uuid
                    + ", entityType=" + entityType + ", dynamic_properties="
                    + dynamic_properties + "]";
        }

    }

    /**
     * Models the feed from an activity stream as a collection
     * of individual Activity objects.
     */
    //@XmlRootElement
    static public class ActivityCollection {

        int totalItems;

        ActivityObject[] items;

        String url;

        protected Map<String, Object> dynamic_properties = new TreeMap<String, Object>(
                String.CASE_INSENSITIVE_ORDER);

        /**
         * Default constructor.
         */
        public ActivityCollection() {
        }

        /**
         * Gets a count of the number of activities
         *
         * @return  the activity count
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public int getTotalItems() {
            return totalItems;
        }

        /**
         * @y.exclude
         */
        public void setTotalItems(int totalItems) {
            this.totalItems = totalItems;
        }

        /**
         * Gets an array of the activities.
         *
         * @return  an array of ActivityObject objects
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public ActivityObject[] getItems() {
            return items;
        }

        /**
         * @y.exclude
         */
        public void setItems(ActivityObject[] items) {
            this.items = items;
        }

        /**
         * Gets the url for the activity feed.
         *
         * @return  the URL
         */
        @JsonSerialize(include = Inclusion.NON_NULL)
        public String getUrl() {
            return url;
        }

        /**
         * @y.exclude
         */
        public void setUrl(String url) {
            this.url = url;
        }

        @JsonAnySetter
        public void setDynamicProperty(String key, Object value) {
            dynamic_properties.put(key, value);
        }

        @JsonAnyGetter
        public Map<String, Object> getDynamicProperties() {
            return dynamic_properties;
        }

        /**
         * Returns the properties of the ActivityCollection as a string.
         *
         * @return the object properties
         */
        @Override
        public String toString() {
            return "ActivityCollection [totalItems=" + totalItems + ", items="
                    + Arrays.toString(items) + ", url=" + url
                    + ", dynamic_properties=" + dynamic_properties + "]";
        }

    }

}
