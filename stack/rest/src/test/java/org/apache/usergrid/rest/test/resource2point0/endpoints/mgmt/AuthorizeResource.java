package org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.representation.Form;
import org.apache.usergrid.rest.test.resource2point0.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

/**
 * OAuth authorization resource
 */
public class AuthorizeResource extends NamedResource {
    public AuthorizeResource(final ClientContext context, final UrlResource parent) {
        super( "authorize", context, parent );
    }
    /**
     * Obtains an OAuth authorization
     * @param requestEntity
     * @return
     */
    public Object post(Object requestEntity){
        return getResource().post(Object.class,requestEntity);

    }
    /**
     * Obtains an OAuth authorization
     * @param type
     * @param requestEntity
     * @return
     */
    public <T> T post(Class<T> type, Object requestEntity){
        GenericType<T> gt = new GenericType<>((Class)type);
        return getResource().post(gt.getRawClass(),requestEntity);

    }

}
