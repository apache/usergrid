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
package org.usergrid.java.client.entities;

import static org.usergrid.java.client.utils.JsonUtils.getLongProperty;
import static org.usergrid.java.client.utils.JsonUtils.getObjectProperty;
import static org.usergrid.java.client.utils.JsonUtils.getStringProperty;
import static org.usergrid.java.client.utils.JsonUtils.setLongProperty;
import static org.usergrid.java.client.utils.JsonUtils.setObjectProperty;
import static org.usergrid.java.client.utils.JsonUtils.setStringProperty;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/**
 * An entity type for representing activity stream actions. These are similar to
 * the more generic message entity type except provide the necessary properties
 * for supporting activity stream implementations.
 * 
 * @see http://activitystrea.ms/specs/json/1.0/
 */
public class Activity extends Entity {

	public static final String ENTITY_TYPE = "activity";

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

	public final static String PROPERTY_CONTENT = "content";
	public final static String PROPERTY_GENERATOR = "generator";
	public final static String PROPERTY_ICON = "icon";
	public final static String PROPERTY_CATEGORY = "category";
	public final static String PROPERTY_VERB = "verb";
	public final static String PROPERTY_PUBLISHED = "published";
	public final static String PROPERTY_OBJECT = "object";
	public final static String PROPERTY_TITLE = "title";
	public final static String PROPERTY_ACTOR = "actor";

	public Activity() {
		super();
		setType(ENTITY_TYPE);
	}

	public Activity(Entity entity) {
		super();
		properties = entity.properties;
		setType(ENTITY_TYPE);
	}

	@Override
	@JsonIgnore
	public String getNativeType() {
		return ENTITY_TYPE;
	}

	@Override
	@JsonIgnore
	public List<String> getPropertyNames() {
		List<String> properties = super.getPropertyNames();
		properties.add(PROPERTY_CONTENT);
		properties.add(PROPERTY_GENERATOR);
		properties.add(PROPERTY_ICON);
		properties.add(PROPERTY_CATEGORY);
		properties.add(PROPERTY_VERB);
		properties.add(PROPERTY_PUBLISHED);
		properties.add(PROPERTY_OBJECT);
		properties.add(PROPERTY_TITLE);
		properties.add(PROPERTY_ACTOR);
		return properties;
	}

	public static Activity newActivity(String verb, String title,
			String content, String category, User user, Entity object,
			String objectType, String objectName, String objectContent) {

		Activity activity = new Activity();
		activity.setVerb(verb);
		activity.setCategory(category);
		activity.setContent(content);
		activity.setTitle(title);

		ActivityObject actor = new ActivityObject();
		actor.setObjectType("person");
		if (user != null) {
			actor.setDisplayName(user.getName());
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

	@JsonSerialize(include = Inclusion.NON_NULL)
	public ActivityObject getActor() {
		return getObjectProperty(properties, PROPERTY_ACTOR,
				ActivityObject.class);
	}

	public void setActor(ActivityObject actor) {
		setObjectProperty(properties, PROPERTY_ACTOR, actor);
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public ActivityObject getGenerator() {
		return getObjectProperty(properties, PROPERTY_GENERATOR,
				ActivityObject.class);
	}

	public void setGenerator(ActivityObject generator) {
		setObjectProperty(properties, PROPERTY_GENERATOR, generator);
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getCategory() {
		return getStringProperty(properties, PROPERTY_CATEGORY);
	}

	public void setCategory(String category) {
		setStringProperty(properties, PROPERTY_CATEGORY, category);
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getVerb() {
		return getStringProperty(properties, PROPERTY_VERB);
	}

	public void setVerb(String verb) {
		setStringProperty(properties, PROPERTY_VERB, verb);
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public Long getPublished() {
		return getLongProperty(properties, PROPERTY_PUBLISHED);
	}

	public void setPublished(Long published) {
		setLongProperty(properties, PROPERTY_PUBLISHED, published);
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public ActivityObject getObject() {
		return getObjectProperty(properties, PROPERTY_OBJECT,
				ActivityObject.class);
	}

	public void setObject(ActivityObject object) {
		setObjectProperty(properties, PROPERTY_OBJECT, object);
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getTitle() {
		return getStringProperty(properties, PROPERTY_TITLE);
	}

	public void setTitle(String title) {
		setStringProperty(properties, PROPERTY_TITLE, title);
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public MediaLink getIcon() {
		return getObjectProperty(properties, PROPERTY_ICON, MediaLink.class);
	}

	public void setIcon(MediaLink icon) {
		setObjectProperty(properties, PROPERTY_ICON, icon);
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getContent() {
		return getStringProperty(properties, PROPERTY_CONTENT);
	}

	public void setContent(String content) {
		setStringProperty(properties, PROPERTY_CONTENT, content);
	}

	static public class MediaLink {

		int duration;

		int height;

		String url;

		int width;

		protected Map<String, Object> dynamic_properties = new TreeMap<String, Object>(
				String.CASE_INSENSITIVE_ORDER);

		public MediaLink() {
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public int getDuration() {
			return duration;
		}

		public void setDuration(int duration) {
			this.duration = duration;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public int getWidth() {
			return width;
		}

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

		@Override
		public String toString() {
			return "MediaLink [duration=" + duration + ", height=" + height
					+ ", url=" + url + ", width=" + width
					+ ", dynamic_properties=" + dynamic_properties + "]";
		}

	}

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

		public ActivityObject() {
		}
		
		public ActivityObject(String json){
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public ActivityObject[] getAttachments() {
			return attachments;
		}

		public void setAttachments(ActivityObject[] attachments) {
			this.attachments = attachments;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public ActivityObject getAuthor() {
			return author;
		}

		public void setAuthor(ActivityObject author) {
			this.author = author;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String[] getDownstreamDuplicates() {
			return downstreamDuplicates;
		}

		public void setDownstreamDuplicates(String[] downstreamDuplicates) {
			this.downstreamDuplicates = downstreamDuplicates;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public MediaLink getImage() {
			return image;
		}

		public void setImage(MediaLink image) {
			this.image = image;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getObjectType() {
			return objectType;
		}

		public void setObjectType(String objectType) {
			this.objectType = objectType;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public Date getPublished() {
			return published;
		}

		public void setPublished(Date published) {
			this.published = published;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getSummary() {
			return summary;
		}

		public void setSummary(String summary) {
			this.summary = summary;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getUpdated() {
			return updated;
		}

		public void setUpdated(String updated) {
			this.updated = updated;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getUpstreamDuplicates() {
			return upstreamDuplicates;
		}

		public void setUpstreamDuplicates(String upstreamDuplicates) {
			this.upstreamDuplicates = upstreamDuplicates;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public UUID getUuid() {
			return uuid;
		}

		public void setUuid(UUID uuid) {
			this.uuid = uuid;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getEntityType() {
			return entityType;
		}

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

	static public class ActivityCollection {

		int totalItems;

		ActivityObject[] items;

		String url;

		protected Map<String, Object> dynamic_properties = new TreeMap<String, Object>(
				String.CASE_INSENSITIVE_ORDER);

		public ActivityCollection() {
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public int getTotalItems() {
			return totalItems;
		}

		public void setTotalItems(int totalItems) {
			this.totalItems = totalItems;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public ActivityObject[] getItems() {
			return items;
		}

		public void setItems(ActivityObject[] items) {
			this.items = items;
		}

		@JsonSerialize(include = Inclusion.NON_NULL)
		public String getUrl() {
			return url;
		}

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

		@Override
		public String toString() {
			return "ActivityCollection [totalItems=" + totalItems + ", items="
					+ Arrays.toString(items) + ", url=" + url
					+ ", dynamic_properties=" + dynamic_properties + "]";
		}

	}

}
