package org.apache.usergrid.persistence.collection.exception;


/**
 * @author tnine
 */
public class CollectionRuntimeException extends RuntimeException {
    public CollectionRuntimeException() {
    }


    public CollectionRuntimeException( final String message ) {
        super( message );
    }


    public CollectionRuntimeException( final String message, final Throwable cause ) {
        super( message, cause );
    }


    public CollectionRuntimeException( final Throwable cause ) {
        super( cause );
    }


    public CollectionRuntimeException( final String message, final Throwable cause, final boolean enableSuppression,
                                       final boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }
}
