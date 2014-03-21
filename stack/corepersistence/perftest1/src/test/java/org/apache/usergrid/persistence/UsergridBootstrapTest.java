
package org.apache.usergrid.persistence;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.RandomStringUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UsergridBootstrapTest {
    private static final Logger log = LoggerFactory.getLogger(UsergridBootstrapTest.class);
   
    @Ignore // just an example and not included in performance test 
    @Test
    public void testBasicOperation() throws Throwable {

        UsergridBootstrap sbf = UsergridBootstrap.newInstance();
        EntityManagerFactory emf = sbf.getBean( EntityManagerFactory.class );

        UUID appId = emf.createApplication(
            "testorg-" + RandomStringUtils.randomAlphanumeric(6), 
            "testapp-" + RandomStringUtils.randomAlphanumeric(6));

        EntityManager em = emf.getEntityManager( appId );

        Map<String, Object> map = new HashMap<String, Object>() {{
            put("name", RandomStringUtils.randomAlphanumeric(6));
            put("timestamp", new Date().getTime());
        }};
        Entity entity = em.create( "testentity", map );
        String name = (String)entity.getProperty( "name" );

        Entity got = em.get( entity.getUuid() );
        assertNotNull(got);
        String returnedName = (String)got.getProperty("name");
        long timestamp = (long)got.getProperty("timestamp");
        assertEquals( name, returnedName );
        log.info(">>>> Got back name={} : time={}",  returnedName, timestamp);

        Query query = Query.fromQL("name = '" + name + "'");
        Entity found = em.searchCollection( em.getApplication(), "testentities", query ).getEntity();
        assertNotNull(found);
        returnedName = (String)found.getProperty("name");
        timestamp = (long)found.getProperty("timestamp");
        assertEquals( name, returnedName );
        log.info(">>>> Found name={} : time={}",  returnedName, timestamp);
    }
    
}
