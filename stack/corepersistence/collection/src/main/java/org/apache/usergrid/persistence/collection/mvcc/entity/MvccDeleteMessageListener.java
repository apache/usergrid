package org.apache.usergrid.persistence.collection.mvcc.entity;

import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityEvent;
import org.apache.usergrid.persistence.core.consistency.MessageListener;
import org.apache.usergrid.persistence.core.scope.EntityVersion;

import java.util.UUID;

public interface MvccDeleteMessageListener extends MessageListener<MvccEntityEvent<MvccEntity>, EntityVersion> {
}
