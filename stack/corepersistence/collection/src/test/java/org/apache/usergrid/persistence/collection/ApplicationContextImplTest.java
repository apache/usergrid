package org.apache.usergrid.persistence.collection;


import org.junit.Test;

import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import static junit.framework.TestCase.assertEquals;


/** @author tnine */
public class ApplicationContextImplTest {


    @Test( expected = NullPointerException.class )
    public void orgIdrequired() {
        new ApplicationScopeImpl( null);
    }



    @Test
    public void correctValues() {
        final SimpleId ownerId = new SimpleId( "test" );

        ApplicationScopeImpl context = new ApplicationScopeImpl(ownerId );

        assertEquals( ownerId, context.getApplication() );
    }


}
