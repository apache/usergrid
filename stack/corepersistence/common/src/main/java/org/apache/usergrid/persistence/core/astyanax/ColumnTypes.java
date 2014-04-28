package org.apache.usergrid.persistence.core.astyanax;


import org.apache.cassandra.db.marshal.BooleanType;
import org.apache.cassandra.db.marshal.DynamicCompositeType;


/**
 * Simple class to hold constants we'll need for column types
 *
 */
public class ColumnTypes {

    /**
     * UUID from max by the row key to min at the end of the row
     */
    public static final String UUID_TYPE_REVERSED = "UUIDType(reversed=true)";
    /**
     * Constant for the dynamic composite comparator type we'll need
     */
    public static final String DYNAMIC_COMPOSITE_TYPE = DynamicCompositeType.class.getSimpleName() + "(a=>AsciiType,b=>BytesType,i=>IntegerType,x=>LexicalUUIDType,l=>LongType," +
                        "t=>TimeUUIDType,s=>UTF8Type,u=>UUIDType,A=>AsciiType(reversed=true),B=>BytesType(reversed=true)," +
                        "I=>IntegerType(reversed=true),X=>LexicalUUIDType(reversed=true),L=>LongType(reversed=true)," +
                        "T=>TimeUUIDType(reversed=true),S=>UTF8Type(reversed=true),U=>"+UUID_TYPE_REVERSED+ ")";

    /**
     * Long time with max by the row key and min at the end of the row
     */
    public static final String LONG_TYPE_REVERSED = "LongType(reversed=true)";


}
