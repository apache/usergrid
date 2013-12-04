package org.apache.usergrid.persistence.collection.mvcc.stage;


import java.util.Collection;
import java.util.List;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.event.PostProcessListener;


/** @author tnine */
public interface WriteContext {

    /**
     * Perform the write in the context with the specified entity
     * @param inputData The data to use to being the write
     */
    void performWrite(Object inputData);


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
    CollectionContext getCollectionContext();



}
