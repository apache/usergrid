package org.apache.usergrid.persistence.collection.mvcc.stage;


import java.util.ArrayList;
import java.util.List;


/** @author tnine */
public class Result {

    private List<Object> results;


    public Result() {
        results = new ArrayList<Object>();
    }


    public List<Object> getResults() {
        return results;
    }


    public void addResult( final Object result ) {
        this.results.add( result );
    }


    /**
     * Get the last occurrence of an instance that implements the type provided.
     * @param clazz  The class that the value should be an instance of
     * @param <T> The type of class
     * @return The value if one is found, null otherwise
     */
    public <T> T getLast( Class<T> clazz ) {

        final int size = results.size();

        for ( int i = size - 1; i > -1; i-- ) {
            final Object value = results.get( i );
            if ( clazz.isInstance( value ) ) {
                return ( T ) value;
            }
        }

        return null;
    }
}
