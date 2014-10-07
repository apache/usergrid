package org.apache.usergrid.persistence.collection.event.impl;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.event.EntityDeleted;
import org.apache.usergrid.persistence.model.entity.Id;


public class EntityDeletedImpl implements EntityDeleted {

    public EntityDeletedImpl(){}

    @Override
    public void deleted(CollectionScope scope, Id entityId) {

    }
}
