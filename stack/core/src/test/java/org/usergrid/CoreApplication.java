package org.usergrid;


import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.mq.QueueManager;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static junit.framework.Assert.assertNotNull;


public class CoreApplication implements Application, TestRule
{
    private final static Logger LOG = LoggerFactory.getLogger( CoreApplication.class );

    protected UUID id;
    protected String appName;
    protected String orgName;
    protected CoreITSetup setup;
    protected EntityManager em;
    protected Map<String, Object> properties = new LinkedHashMap<String, Object>();


    public CoreApplication(CoreITSetup setup)
    {
        this.setup = setup;
    }


    @Override
    public void putAll( Map<String, Object> properties )
    {
        this.properties.putAll( properties );
    }


    @Override
    public Object get( String key )
    {
        return properties.get( key );
    }


    @Override
    public Map<String,Object> getProperties()
    {
        return properties;
    }


    @Override
    public UUID getId()
    {
        return id;
    }


    @Override
    public String getOrgName()
    {
        return orgName;
    }


    @Override
    public String getAppName()
    {
        return appName;
    }


    @Override
    public Entity create( String type ) throws Exception
    {
        Entity entity = em.create( type, properties );
        clear();
        return entity;
    }


    @Override
    public Object put( String property, Object value )
    {
        return properties.put( property, value );
    }


    @Override
    public void clear()
    {
        properties.clear();
    }


    @Override
    public void addToCollection( Entity user, String collection, Entity item ) throws Exception
    {
        em.addToCollection( user, collection, item );
    }


    @Override
    public Results searchCollection( Entity user, String collection, Query query ) throws Exception
    {
        return  em.searchCollection( user, collection, query );
    }


    @Override
    public Entity get( UUID id ) throws Exception
    {
        return em.get( id );
    }


    @Override
    public Statement apply( final Statement base, final Description description )
    {
        return new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                before( description );

                try
                {
                    base.evaluate();
                }
                finally
                {
                    after( description );
                }
            }
        };
    }


    protected void after( Description description )
    {
        LOG.info( "Test {}: finish with application", description.getDisplayName() );
    }


    protected void before( Description description ) throws Exception
    {
        orgName = description.getClassName();
        appName = description.getMethodName();
        id = setup.createApplication( orgName, appName );
        assertNotNull( id );

        em = setup.getEmf().getEntityManager( id );
        assertNotNull( em );

        LOG.info( "Created new application {} in organization {}", appName, orgName );
    }


    public EntityManager getEm()
    {
        return em;
    }


    public QueueManager getQm()
    {
        return setup.getQmf().getQueueManager( getId() );
    }
}
