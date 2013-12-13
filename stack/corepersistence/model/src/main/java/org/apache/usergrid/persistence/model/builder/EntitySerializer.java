package org.apache.usergrid.persistence.model.builder;


import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Responsible for serializing the specific type of entity to and from an object
 *  @author tnine */
public interface EntitySerializer<T> {


    /**
     * Return a full entity or a set of fields?
     *
     * @param object
     * @return
     */
    Entity fromEntity(T object);


    /**
     * Convert the entity to the runtime type we need
     * @param e
     * @return
     */
    T toObject(Entity e);

}
