package com.usergrid.count.common;

import com.usergrid.count.common.Count;
import com.usergrid.count.common.CountSerDeUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author zznate
 */
public class CountSerDeUtilsTest {

    private static final String SIMPLE_JSON =
            "{\"tableName\":\"Counters\",\"keyName\":\"k1\",\"columnName\":\"c1\",\"value\":1}";

    @Test
    public void testSerialize() {
        Count count = new Count("Counters","k1","c1",1);
        String sered = CountSerDeUtils.serialize(count);
        assertEquals(SIMPLE_JSON, sered);
    }

    @Test
    public void testDeserializer() {
        Count count = CountSerDeUtils.deserialize(SIMPLE_JSON);
        assertEquals("k1",count.getKeyName());
        assertEquals("c1",count.getColumnName());
        assertEquals("Counters", count.getTableName());
        assertEquals(1,count.getValue());
    }
}
