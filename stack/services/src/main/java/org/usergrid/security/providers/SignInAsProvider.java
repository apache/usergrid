package org.usergrid.security.providers;


import java.util.Map;

import org.usergrid.persistence.entities.User;
import org.usergrid.security.tokens.exceptions.BadTokenException;


/** @author zznate */
public interface SignInAsProvider {

    /** Authenticate a userId and external token against this provider */
    User createOrAuthenticate( String externalToken ) throws BadTokenException;

    Map<Object, Object> loadConfigurationFor();

    void saveToConfiguration( Map<String, Object> config );
}
