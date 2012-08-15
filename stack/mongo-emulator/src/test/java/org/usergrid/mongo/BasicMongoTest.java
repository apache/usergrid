package org.usergrid.mongo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.usergrid.persistence.EntityManager;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;

public class BasicMongoTest extends AbstractMongoTest {

	@Test
	public void basicTest() throws Exception {
		UUID appId = emf.lookupApplication("test-organization/test-app");
		EntityManager em = emf.getEntityManager(appId);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", "nico");
		properties.put("color", "tabby");
		em.create("cat", properties);

		properties = new LinkedHashMap<String, Object>();
		properties.put("name", "dylan");
		properties.put("color", "black");
		em.create("cat", properties);

		properties = new LinkedHashMap<String, Object>();
		properties.put("name", "fishbone");
		properties.put("color", "tuxedo");
		em.create("cat", properties);

		properties = new LinkedHashMap<String, Object>();
		properties.put("name", "fritz");
		properties.put("color", "tabby");
		em.create("cat", properties);

		properties = new LinkedHashMap<String, Object>();
		properties.put("name", "lulu");
		properties.put("color", "tabby");
		em.create("cat", properties);

		properties = new LinkedHashMap<String, Object>();
		properties.put("name", "mimi");
		properties.put("color", "black");
		em.create("cat", properties);

		// See http://www.mongodb.org/display/DOCS/Java+Tutorial

		Mongo m = new Mongo("localhost", 27017);

		DB db = m.getDB("test-organization/test-app");
		db.authenticate("test", "test".toCharArray());

		Set<String> colls = db.getCollectionNames();

		for (String s : colls) {
			System.out.println(s);
		}

		DBCollection coll = db.getCollection("cats");
		DBCursor cur = coll.find();
		while (cur.hasNext()) {
			System.out.println(cur.next());
		}

		BasicDBObject query = new BasicDBObject();
		query.put("color", "black");
		cur = coll.find(query);

		while (cur.hasNext()) {
			System.out.println(cur.next());
		}
	}
}
