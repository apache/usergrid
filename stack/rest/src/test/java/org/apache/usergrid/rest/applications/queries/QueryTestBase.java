package org.apache.usergrid.rest.applications.queries;

import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base class containing common methods used by query tests
 */
public class QueryTestBase  extends AbstractRestIT {
    private static Logger log = LoggerFactory.getLogger(QueryTestBase.class);
    /**
     * Create a number of entities in the specified collection
     * with properties to make them independently searchable
     *
     * @param numberOfEntities
     * @param collectionName
     * @return an array of the Entity objects created
     */
    protected Entity[] generateTestEntities(int numberOfEntities, String collectionName) {
        Entity[] entities = new Entity[numberOfEntities];
        Entity props = new Entity();
        //Insert the desired number of entities
        for (int i = 0; i < numberOfEntities; i++) {
            Entity actor = new Entity();
            actor.put("displayName", String.format("Test User %d", i));
            actor.put("username", String.format("user%d", i));
            props.put("actor", actor);
            //give each entity a unique, numeric ordinal value
            props.put("ordinal", i);
            //Set half the entities to have a 'madeup' property of 'true'
            // and set the other half to 'false'
            if (i < numberOfEntities / 2) {
                props.put("madeup", false);
            } else {
                props.put("madeup", true);
            }
            //Set even-numbered users to have a verb of 'go' and the rest to 'stop'
            if (i % 2 == 0) {
                props.put("verb", "go");
            } else {
                props.put("verb", "stop");
            }
            //create the entity in the desired collection and add it to the return array
            entities[i] = this.app().collection(collectionName).post(props);
            log.info(entities[i].entrySet().toString());
        }
        //refresh the index so that they are immediately searchable
        this.refreshIndex();

        return entities;
    }


}
