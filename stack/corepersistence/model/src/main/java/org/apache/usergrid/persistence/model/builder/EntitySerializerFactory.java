package org.apache.usergrid.persistence.model.builder;


import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Get the entity serializer for the given id type
 * @author tnine */
public interface EntitySerializerFactory {


    /**
     * Get the entity serializer for the type T
     * @param id
     * @param <T>
     * @return
     */
    <T> EntitySerializer<T> getSerializer(Id id);


}
