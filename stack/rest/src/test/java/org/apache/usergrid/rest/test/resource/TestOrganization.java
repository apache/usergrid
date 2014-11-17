package org.apache.usergrid.rest.test.resource;


import java.util.UUID;


/**
 * Wrapper class that contains all the resources needed by a organization
 */
public class TestOrganization {
    private String orgName;
    private UUID uuid;

    //test organizations are always initialized by name, and the uuid will be set on creations
    public TestOrganization( final String orgName) {
        this.orgName = orgName;
    }

    public UUID getUuid() {
        return uuid;
    }


    public void setUuid( final UUID uuid ) {
        this.uuid = uuid;
    }


    public String getOrgName() {

        return orgName;
    }


    public void setOrgName( final String orgName ) {
        this.orgName = orgName;
    }

}
