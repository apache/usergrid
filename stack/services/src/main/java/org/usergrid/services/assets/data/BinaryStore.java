package org.usergrid.services.assets.data;


import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.usergrid.persistence.Entity;


public interface BinaryStore {

    /**
     * writes the inputStream to the store and updates the entity's file-metadata field. however, it doesn't persistent
     * the entity.
     */
    void write( UUID appId, Entity entity, InputStream inputStream ) throws IOException;

    /** read the entity's file data from the store */
    InputStream read( UUID appId, Entity entity ) throws IOException;

    /** read partial data from the store */
    InputStream read( UUID appId, Entity entity, long offset, long length ) throws IOException;

    /** delete the entity data from the store. */
    void delete( UUID appId, Entity entity );
}
