package org.apache.usergrid.persistence.collection.mvcc.entity;


import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Id;


/**
 *
 *
 */
public interface MvccId {

    UUID getVersion();

    Id getId();
}
