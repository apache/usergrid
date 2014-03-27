package org.apache.usergrid.persistence.graph.impl;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.OrganizationScope;


/**
 * Get the edge event in the organizational scope
 *
 */
public class EdgeEvent<T> {

    private final OrganizationScope organizationScope;
    private final T data;
    private final UUID version;


    public EdgeEvent( final OrganizationScope organizationScope, final UUID version, final T data ) {
        this.organizationScope = organizationScope;
        this.data = data;
        this.version = version;
    }


    public OrganizationScope getOrganizationScope() {
        return organizationScope;
    }


    public UUID getVersion() {
        return version;
    }


    public T getData() {
        return data;
    }


}
