package org.apache.usergrid.persistence.collection.mvcc.entity.impl;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.mvcc.entity.MvccId;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 *
 *
 */
public class MvccIdImpl implements MvccId {
    UUID version;

    Id id;

    public MvccIdImpl(UUID version, Id id){
        this.version = version;
        this.id = id;
    }

    @Override
    public UUID getVersion() {
        return version;
    }


    @Override
    public Id getId() {
        return id;
    }
}
