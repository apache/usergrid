package org.apache.usergrid.persistence.collection.migration;


import java.util.HashMap;
import java.util.Map;

import com.netflix.astyanax.model.ColumnFamily;


/**
 * Bean wrapper for column family information
 *
 * @author tnine
 */
public class CollectionColumnFamily {

    public static final String COMPARATOR_TYPE = "comparator_type";
    public static final String REVERSED = "reversed";
    public static final String READ_REPAIR_CHANCE = "read_repair_chance";


    private final ColumnFamily columnFamily;
    private final String comparator;
    private final boolean reversed;


    public CollectionColumnFamily( final ColumnFamily columnFamily, final String comparator, final boolean reversed ) {
        this.columnFamily = columnFamily;
        this.comparator = comparator;
        this.reversed = reversed;
    }


    public Map<String, Object> getOptions(){

        Map<String, Object> options = new HashMap<String, Object>();
        options.put( COMPARATOR_TYPE, comparator );
        options.put( REVERSED, reversed );

        //always use 10% read repair chance!
        options.put( READ_REPAIR_CHANCE, 0.1d );

        return options;
    }


    public ColumnFamily getColumnFamily() {
        return columnFamily;
    }
}
