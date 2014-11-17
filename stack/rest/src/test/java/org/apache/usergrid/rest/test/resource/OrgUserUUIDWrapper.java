package org.apache.usergrid.rest.test.resource;


import java.util.UUID;


/**
 * Created by ApigeeCorporation on 11/14/14.
 */
public class OrgUserUUIDWrapper {
    private UUID orgUUID;
    private UUID userUUID;


    public OrgUserUUIDWrapper( final UUID orgUUID, final UUID userUUID ) {
        this.orgUUID = orgUUID;
        this.userUUID = userUUID;
    }


    public UUID getOrgUUID() {
        return orgUUID;
    }


    public UUID getUserUUID() {
        return userUUID;
    }
}
