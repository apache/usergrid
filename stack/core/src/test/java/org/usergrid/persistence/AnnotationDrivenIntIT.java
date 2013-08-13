package org.usergrid.persistence;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.AbstractCoreIT;
import org.usergrid.cassandra.Concurrent;

import static org.junit.Assert.assertTrue;


/**
 * @author zznate
 */
@Concurrent()
public class AnnotationDrivenIntIT extends AbstractCoreIT
{
    private Logger logger = LoggerFactory.getLogger( AnnotationDrivenIntIT.class );


    @Test
    public void shouldSpinUp() throws Exception
    {
        logger.info( "shouldSpinUp" );
        assertTrue( true );
    }


    @Test
    public void shouldStillBeUp() throws Exception
    {
        logger.info( "shouldStillBeUp" );
        assertTrue( true );
    }

}
