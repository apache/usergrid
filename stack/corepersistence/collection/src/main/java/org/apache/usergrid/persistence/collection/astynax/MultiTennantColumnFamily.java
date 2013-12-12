package org.apache.usergrid.persistence.collection.astynax;


import org.apache.usergrid.persistence.collection.OrganizationScope;

import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.ColumnFamily;


/**
 * Simple wrapper to force every column family to use ScopedRowKeys
 *
 * @author tnine
 */
public class MultiTennantColumnFamily<S extends OrganizationScope, K, V> extends ColumnFamily<ScopedRowKey<S, K>, V> {

    public MultiTennantColumnFamily( final String columnFamilyName, final Serializer<ScopedRowKey<S, K>> keySerializer,
                                     final Serializer<V> columnSerializer ) {
        super( columnFamilyName, keySerializer, columnSerializer );
    }


    public MultiTennantColumnFamily( final String columnFamilyName, final Serializer<ScopedRowKey<S, K>> keySerializer,
                                     final Serializer<V> columnSerializer,
                                     final Serializer<?> defaultValueSerializer ) {
        super( columnFamilyName, keySerializer, columnSerializer, defaultValueSerializer );
    }
}
