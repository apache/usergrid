package org.apache.usergrid.persistence.graph.impl;


import org.apache.usergrid.persistence.collection.OrganizationScope;


/**
 * Get the edge event in the organizational scope
 *
 */
public class EdgeEvent<T> {

    private final OrganizationScope organizationScope;
    private final T data;


    public EdgeEvent( final OrganizationScope organizationScope, final T data ) {
        this.organizationScope = organizationScope;
        this.data = data;
    }


    public OrganizationScope getOrganizationScope() {
        return organizationScope;
    }


    public T getData() {
        return data;
    }
}
