package org.apache.usergrid.persistence.collection.astynax;


import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.ColumnFamily;


/**
 * Simple wrapper to force every column family to use ScopedRowKeys
 * @author tnine */
public class MultiTennantColumnFamily<K, V> extends ColumnFamily<ScopedRowKey<K>, V> {
    public MultiTennantColumnFamily( final String columnFamilyName, final Serializer<ScopedRowKey<K>> keySerializer,
                                     final Serializer<V> columnSerializer ) {
        super( columnFamilyName, keySerializer, columnSerializer );
    }


    public MultiTennantColumnFamily( final String columnFamilyName, final Serializer<ScopedRowKey<K>> keySerializer,
                                     final Serializer<V> columnSerializer,
                                     final Serializer<?> defaultValueSerializer ) {
        super( columnFamilyName, keySerializer, columnSerializer, defaultValueSerializer );
    }
}
