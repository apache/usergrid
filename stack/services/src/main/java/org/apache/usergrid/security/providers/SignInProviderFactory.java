package org.apache.usergrid.security.providers;


import org.springframework.beans.factory.annotation.Autowired;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.entities.Application;


/** @author zznate */
public class SignInProviderFactory {

    private EntityManagerFactory emf;
    private ManagementService managementService;


    @Autowired
    public void setEntityManagerFactory( EntityManagerFactory emf ) {
        this.emf = emf;
    }


    @Autowired
    public void setManagementService( ManagementService managementService ) {
        this.managementService = managementService;
    }


    public SignInAsProvider facebook( Application application ) {
        FacebookProvider facebookProvider =
                new FacebookProvider( emf.getEntityManager( application.getUuid() ), managementService );
        facebookProvider.configure();
        return facebookProvider;
    }


    public SignInAsProvider foursquare( Application application ) {
        FoursquareProvider foursquareProvider =
                new FoursquareProvider( emf.getEntityManager( application.getUuid() ), managementService );
        foursquareProvider.configure();
        return foursquareProvider;
    }


    public SignInAsProvider pingident( Application application ) {
        PingIdentityProvider pingIdentityProvider =
                new PingIdentityProvider( emf.getEntityManager( application.getUuid() ), managementService );
        pingIdentityProvider.configure();
        return pingIdentityProvider;
    }
}
