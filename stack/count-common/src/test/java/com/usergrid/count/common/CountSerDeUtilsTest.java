package com.usergrid.count.common;

import me.prettyprint.cassandra.utils.TimeUUIDUtils;
import me.prettyprint.hector.api.factory.HFactory;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author zznate
 */
public class CountSerDeUtilsTest {

    private static final String SIMPLE_JSON =
            "{\"tableName\":\"Counters\",\"keyName\":\"k1\",\"columnName\":\"c1\",\"value\":1}";

    private static final String MIXED_TYPE_JSON =
          "{\"tableName\":\"Counters\",\"keyName\":1,\"columnName\":\"c1\",\"value\":1}";

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

    @Test
    public void testMixedSerializer() {
        Count count = new Count("Counters",1,"c1",1);
        String sered = CountSerDeUtils.serialize(count);
        assertEquals(MIXED_TYPE_JSON, sered);


    }


  @Test
  public void testMixedDeserializer() {
      Count count = CountSerDeUtils.deserialize(MIXED_TYPE_JSON);
      assertEquals(1,count.getKeyName());
      assertEquals("c1",count.getColumnName());
      assertEquals("Counters", count.getTableName());
      assertEquals(1,count.getValue());
  }
}
