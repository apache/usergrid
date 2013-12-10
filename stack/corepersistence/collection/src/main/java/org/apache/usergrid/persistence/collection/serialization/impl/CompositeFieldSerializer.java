package org.apache.usergrid.persistence.collection.serialization.impl;


import org.apache.usergrid.persistence.collection.astynax.fixes.CompositeBuilder;
import org.apache.usergrid.persistence.collection.astynax.fixes.CompositeParser;


/**
 * This interface is for re-using multiple components in a composite.
 * Implementing this allows many different types to be serialized together in a single composite
 *
 * @author tnine */
public interface CompositeFieldSerializer<K> {

    /**
     * Add this to the composite
     * @param builder
     * @param value
     */
    public void toComposite(CompositeBuilder builder, K value);


    /**
     * Create an instance from the composite
     * @param composite
     * @return
     */
    public K fromComposite(CompositeParser composite);


}
