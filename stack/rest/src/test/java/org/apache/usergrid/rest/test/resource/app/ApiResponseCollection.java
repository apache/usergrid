package org.apache.usergrid.rest.test.resource.app;


import java.util.Iterator;
import java.util.List;

import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.RevisedApiResponse;
import org.apache.usergrid.rest.test.resource.CollectionResource;


/**
 * A stateful iterable collection respose.  This is a "collection" of entities from our response that are easier
 * to work with
 */
public class ApiResponseCollection<T> implements Iterable<T>, Iterator<T> {

    private final CollectionResource sourceEndpoint;
    private RevisedApiResponse<T> response;


    public Iterator<T> entities;


    public ApiResponseCollection(final CollectionResource sourceCollection, final RevisedApiResponse response){
        this.response = response;
        this.sourceEndpoint = sourceCollection;
        this.entities = response.getEntities().iterator();
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }


    @Override
    public boolean hasNext() {
        if(!entities.hasNext()){
            advance();
        }

        return entities.hasNext();
    }


    @Override
    public T next() {
        return entities.next();
    }


    /**
     * Go back to the endpoint and try to load the next page
     */
    private void advance(){

      //call the original resource for the next page.

        final String cursor = response.getCursor();

        //no next page
        if(cursor == null){
            return;
        }

        response = sourceEndpoint.withCursor( cursor ).getResponse();
        this.entities = response.getEntities().iterator();
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException( "Remove is unsupported" );
    }
}
