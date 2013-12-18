package org.apache.usergrid.persistence.graph.serialization.stage;


import org.apache.usergrid.persistence.collection.OrganizationScope;


public class GraphIoEvent<T> {

    private OrganizationScope scope;

    private T event;


    public GraphIoEvent( final OrganizationScope scope, final T event ) {
        this.scope = scope;
        this.event = event;
    }


    public OrganizationScope getOrganization() {
        return scope;
    }


    public T getEvent() {
        return event;
    }
}
