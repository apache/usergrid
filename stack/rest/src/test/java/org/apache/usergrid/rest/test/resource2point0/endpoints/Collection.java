package org.apache.usergrid.rest.test.resource2point0.endpoints;


import java.util.UUID;

import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.EntityResponse;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import com.google.common.base.Optional;


/**
 * Created by ApigeeCorporation on 12/4/14.
 */
public class Collection extends NamedResource {


    public Collection( final String name, final ClientContext context,  final UrlResource parent ) {
        super( name, context, parent );
    }


    /**
     * Get a list of entities
     * @return
     */
    public ApiResponse get(final Optional<String> cursor){
        return null;
    }


    /**
     * Get the response as an entity response
     * @return
     */
    public EntityResponse getEntityResponse(){
        return EntityResponse.fromCollection( this );
    }


    /**
     * Post the entity to the users collection
     * @param user
     * @return
     */
    public Entity post(final Entity user){
        return null;
    }


    /**
     * Get the entity by uuid
     * @param uuid
     * @return
     */
    public Entity get(final UUID uuid){
        return get(uuid.toString());
    }


    /**
     * Get the entity by name
     * @param name
     * @return
     */
    public Entity get(final String name){
        return null;
    }


    /**
     * Updte the entity
     * @param toUpdate
     * @return
     */
    public Entity put(final Entity toUpdate){
        return null;
    }


    /**
     * Delete the entity
     * @param uuid
     * @return
     */
    public Entity delete(final UUID uuid){
        return delete(uuid.toString());
    }


    /**
     * Delete the entity by name
     * @param name
     * @return
     */
    public Entity delete(final String name){
        return null;
    }
}
