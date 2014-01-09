package org.apache.usergrid.persistence.model.entity;


import java.util.UUID;


/**
 * Interface for creating identifiers for an entity. The implementation should implement
 * the equals and hasCode methods
 * @author tnine */
public interface Id {

    /**
     * Get the uuid for this id
     * @return
     */
    UUID getUuid();

    /**
     * Get the unique type for this id
     * @return
     */
    String getType();


    //Application -> Class "Application"

    //DynamicEntity -> DynamicEntity


}
