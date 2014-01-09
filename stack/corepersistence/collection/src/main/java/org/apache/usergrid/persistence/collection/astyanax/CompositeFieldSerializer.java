package org.apache.usergrid.persistence.collection.astyanax;


import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;


/**
 * This interface is for re-using multiple components in a composite. Implementing this allows many different types to
 * be serialized together in a single composite
 *
 * @author tnine
 */
public interface CompositeFieldSerializer<K> {

    /**
     * Add this to the composite
     */
    public void toComposite( CompositeBuilder builder, K value );


    /**
     * Create an instance from the composite
     */
    public K fromComposite( CompositeParser composite );
}
