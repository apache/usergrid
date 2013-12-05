package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.apache.usergrid.persistence.collection.EntityCollection;


/** @author tnine */
public interface ExecutionContext {

    /**
     * Perform the write in the context with the specified entity
     * @param inputData The data to use to being the write
     */
    void execute( Object inputData );


    /**
     * Get the current message.  If the message is not the right type at runtime, an assertion exception will be thrown
     * @return
     */
    <T> T getMessage(Class<T> clazz);

    /**
     * Set the message into the write context
     * @return
     */
    Object setMessage(Object object);


    /**
     * Signal that the next stage in the write should proceed
     */
    void proceed();



    /**
     * Return the current collection context
     * @return
     */
    EntityCollection getCollectionContext();



}
