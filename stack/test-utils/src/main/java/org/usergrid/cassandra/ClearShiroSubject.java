package org.usergrid.cassandra;


import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link org.junit.rules.TestRule} that cleans up the Shiro Subject's
 * ThreadState.
 */
public class ClearShiroSubject extends ExternalResource
{
    private static final Logger LOG = LoggerFactory.getLogger( ClearShiroSubject.class );

    @Override
    protected void after()
    {
        super.after();
        Subject subject = SecurityUtils.getSubject();

        if ( subject == null )
        {
            LOG.info( "Shiro Subject was null. No need to clear." );
            return;
        }

        new SubjectThreadState( subject ).clear();
        LOG.info( "Shiro Subject was NOT null. Subject has been cleared." );
    }
}
