package org.apache.usergrid.persistence.index;


import org.apache.usergrid.persistence.model.entity.Entity;


/**
 *
 * @author: tnine
 *
 */
public interface QueryEngine
{


    /** Search and return the entities */
    public Results<Entity> search( Query query );


    /** Search the query, but parse the entities into the given class We may not need to implement this at first */
    public <T> Results<T> search( Query query, Class<T> clazz );
}
