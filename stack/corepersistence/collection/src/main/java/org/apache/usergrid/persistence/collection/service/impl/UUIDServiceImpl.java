package org.apache.usergrid.persistence.collection.service.impl;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;


/**
 * @author tnine
 */
public class UUIDServiceImpl implements UUIDService {

    @Override
    public UUID newTimeUUID() {
        return UUIDGenerator.newTimeUUID();
    }
}
